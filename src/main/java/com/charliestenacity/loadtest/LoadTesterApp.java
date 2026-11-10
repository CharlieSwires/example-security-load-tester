package com.charliestenacity.loadtest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LoadTesterApp extends JFrame {
    private final JTextField target=new JTextField("https://charlie-example-security.co.uk/ExampleSecurity",38);
    private final JTextField endpoints=new JTextField("/health",35);
    private final JTextField username=new JTextField(12); private final JPasswordField password=new JPasswordField(12);
    private final JSpinner users=new JSpinner(new SpinnerNumberModel(5,1,200,1));
    private final JSpinner duration=new JSpinner(new SpinnerNumberModel(60,5,3600,5));
    private final JSpinner ramp=new JSpinner(new SpinnerNumberModel(10,0,600,1));
    private final JSpinner rps=new JSpinner(new SpinnerNumberModel(10,1,500,1));
    private final JSpinner think=new JSpinner(new SpinnerNumberModel(250,0,10000,50));
    private final JCheckBox authorised=new JCheckBox("I own or am authorised to load test this target");
    private final JButton start=new JButton("Start"); private final JButton stop=new JButton("Stop");
    private final JButton csv=new JButton("Export CSV"); private final JButton html=new JButton("Export HTML");
    private final JLabel summary=new JLabel("Ready"); private final JTextArea log=new JTextArea(7,100);
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Metric","Value"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
    private final List<LoadSample> samples=new CopyOnWriteArrayList<>();
    private volatile LoadEngine engine; private volatile long started;
    private final Timer refresh;

    public LoadTesterApp(){
        super("Example Security Load Tester"); setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); setSize(1050,760); setLocationByPlatform(true);
        JPanel p=new JPanel(new GridBagLayout()); GridBagConstraints c=new GridBagConstraints(); c.insets=new Insets(3,4,3,4); c.anchor=GridBagConstraints.WEST;
        row(p,c,0,"Target:",target,"GET endpoints, comma separated:",endpoints);
        row(p,c,1,"Test username (optional):",username,"Password:",password);
        JPanel nums=new JPanel(new FlowLayout(FlowLayout.LEFT)); nums.add(new JLabel("Users"));nums.add(users);nums.add(new JLabel("Duration s"));nums.add(duration);nums.add(new JLabel("Ramp s"));nums.add(ramp);nums.add(new JLabel("Max req/s"));nums.add(rps);nums.add(new JLabel("Think ms"));nums.add(think);
        c.gridx=0;c.gridy=2;c.gridwidth=4;c.fill=GridBagConstraints.HORIZONTAL;p.add(nums,c);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));actions.add(authorised);actions.add(start);actions.add(stop);actions.add(csv);actions.add(html);actions.add(summary);
        c.gridy=3;p.add(actions,c); stop.setEnabled(false);csv.setEnabled(false);html.setEnabled(false);
        JTable table=new JTable(model);table.setRowHeight(28);JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(table),new JScrollPane(log));split.setResizeWeight(.65);
        add(p,BorderLayout.NORTH);add(split,BorderLayout.CENTER);log.setEditable(false);
        start.addActionListener(e->startTest());stop.addActionListener(e->{if(engine!=null)engine.stop();append("Stop requested.");});csv.addActionListener(e->exportCsv());html.addActionListener(e->exportHtml());
        refresh=new Timer(1000,e->refreshStats());refresh.start();
    }
    private void row(JPanel p,GridBagConstraints c,int y,String l1,JComponent f1,String l2,JComponent f2){c.gridy=y;c.gridwidth=1;c.fill=GridBagConstraints.NONE;c.weightx=0;c.gridx=0;p.add(new JLabel(l1),c);c.gridx=1;c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;p.add(f1,c);c.gridx=2;c.fill=GridBagConstraints.NONE;c.weightx=0;p.add(new JLabel(l2),c);c.gridx=3;c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;p.add(f2,c);}
    private void startTest(){
        if(!authorised.isSelected()){JOptionPane.showMessageDialog(this,"Confirm authorisation before generating traffic.");return;}
        String url=target.getText().trim();if(!url.startsWith("https://")&&!url.startsWith("http://localhost")){JOptionPane.showMessageDialog(this,"Remote load tests must use HTTPS.");return;}
        List<String> paths=Arrays.stream(endpoints.getText().split(",")).map(String::trim).filter(s->s.startsWith("/")&&!s.contains(" ")).distinct().toList();if(paths.isEmpty()){JOptionPane.showMessageDialog(this,"Supply at least one GET endpoint beginning with /.");return;}
        if(((int)users.getValue()>50||(int)rps.getValue()>100)&&JOptionPane.showConfirmDialog(this,"This is a high traffic setting. Have you confirmed it is acceptable to Krystal?","Confirm high load",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        samples.clear();log.setText("");started=System.currentTimeMillis();start.setEnabled(false);stop.setEnabled(true);csv.setEnabled(false);html.setEnabled(false);
        char[] pass=password.getPassword();LoadEngine.Config cfg=new LoadEngine.Config(url,username.getText().trim(),pass,paths,(int)users.getValue(),(int)duration.getValue(),(int)ramp.getValue(),(int)rps.getValue(),(int)think.getValue());
        engine=new LoadEngine(cfg,samples::add,this::append);append("Starting GET-only test against "+url+" with "+cfg.users()+" users and max "+cfg.maxRps()+" req/s.");
        SwingWorker<Void,Void> worker=new SwingWorker<>(){@Override protected Void doInBackground()throws Exception{engine.run();return null;}@Override protected void done(){try{get();append("Test finished.");}catch(Exception ex){append("Test failed: "+ex.getMessage());}start.setEnabled(true);stop.setEnabled(false);csv.setEnabled(!samples.isEmpty());html.setEnabled(!samples.isEmpty());refreshStats();}};worker.execute();
    }
    private void refreshStats(){long elapsed=started==0?0:System.currentTimeMillis()-started;Statistics s=Statistics.from(new ArrayList<>(samples),elapsed);model.setRowCount(0);Object[][] rows={{"Requests",s.total()},{"Successful",s.successes()},{"Failed",s.failures()},{"Throughput",String.format("%.2f req/s",s.requestsPerSecond())},{"Minimum",s.min()+" ms"},{"Mean",s.mean()+" ms"},{"p50",s.p50()+" ms"},{"p90",s.p90()+" ms"},{"p95",s.p95()+" ms"},{"p99",s.p99()+" ms"},{"Maximum",s.max()+" ms"}};for(Object[]r:rows)model.addRow(r);summary.setText(s.total()+" requests, "+s.failures()+" failures, p95 "+s.p95()+" ms");}
    private void append(String s){SwingUtilities.invokeLater(()->{log.append(java.time.LocalTime.now().withNano(0)+"  "+s+"\n");log.setCaretPosition(log.getDocument().getLength());});}
    private void exportCsv(){choose("example-security-load-samples.csv",p->{LoadReportWriter.writeCsv(p,new ArrayList<>(samples));});}
    private void exportHtml(){choose("example-security-load-report.html",p->{LoadReportWriter.writeHtml(p,target.getText(),Statistics.from(new ArrayList<>(samples),Math.max(1,System.currentTimeMillis()-started)));});}
    private void choose(String name,Writer writer){JFileChooser ch=new JFileChooser();ch.setSelectedFile(new java.io.File(name));if(ch.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)try{writer.write(ch.getSelectedFile().toPath());JOptionPane.showMessageDialog(this,"Saved "+ch.getSelectedFile());}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}}
    @FunctionalInterface interface Writer{void write(Path p)throws Exception;}
    public static void main(String[]args){SwingUtilities.invokeLater(()->new LoadTesterApp().setVisible(true));}
}

