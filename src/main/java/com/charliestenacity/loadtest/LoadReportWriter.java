package com.charliestenacity.loadtest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

final class LoadReportWriter {
    private LoadReportWriter() {}
    static void writeCsv(Path path, List<LoadSample> samples) throws Exception {
        StringBuilder out=new StringBuilder("epochMillis,endpoint,status,latencyMillis,error\n");
        for(LoadSample s:samples) out.append(s.epochMillis()).append(',').append(csv(s.endpoint())).append(',').append(s.status()).append(',').append(s.latencyMillis()).append(',').append(csv(s.error()==null?"":s.error())).append('\n');
        Files.writeString(path,out,StandardCharsets.UTF_8);
    }
    static void writeHtml(Path path,String target,Statistics s) throws Exception {
        String h="<!doctype html><html><head><meta charset=\"utf-8\"><title>Load test report</title><style>body{font-family:Arial;margin:2rem}td,th{border:1px solid #bbb;padding:.6rem}table{border-collapse:collapse}</style></head><body><h1>Example Security load test</h1><p><b>Target:</b> "+esc(target)+"<br><b>Generated:</b> "+ Instant.now()+"</p><table><tr><th>Total</th><th>Success</th><th>Failures</th><th>req/s</th><th>Mean</th><th>p50</th><th>p90</th><th>p95</th><th>p99</th><th>Max</th></tr><tr><td>"+s.total()+"</td><td>"+s.successes()+"</td><td>"+s.failures()+"</td><td>"+String.format("%.2f",s.requestsPerSecond())+"</td><td>"+s.mean()+" ms</td><td>"+s.p50()+" ms</td><td>"+s.p90()+" ms</td><td>"+s.p95()+" ms</td><td>"+s.p99()+" ms</td><td>"+s.max()+" ms</td></tr></table><p>Traffic was generated only after explicit authorisation confirmation.</p></body></html>";
        Files.writeString(path,h,StandardCharsets.UTF_8);
    }
    private static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}

