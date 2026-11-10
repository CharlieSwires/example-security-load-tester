package com.charliestenacity.loadtest;

import java.util.Comparator;
import java.util.List;

record Statistics(long total, long successes, long failures, double requestsPerSecond,
                  long min, long mean, long p50, long p90, long p95, long p99, long max) {
    static Statistics from(List<LoadSample> samples, long elapsedMillis) {
        if (samples.isEmpty()) return new Statistics(0,0,0,0,0,0,0,0,0,0,0);
        List<Long> values = samples.stream().map(LoadSample::latencyMillis).sorted().toList();
        long ok=samples.stream().filter(LoadSample::success).count();
        long sum=values.stream().mapToLong(Long::longValue).sum();
        return new Statistics(samples.size(),ok,samples.size()-ok,samples.size()*1000.0/Math.max(1,elapsedMillis),
                values.get(0),sum/values.size(),pct(values,.50),pct(values,.90),pct(values,.95),pct(values,.99),values.get(values.size()-1));
    }
    private static long pct(List<Long> v,double p) { return v.get(Math.min(v.size()-1,(int)Math.ceil(p*v.size())-1)); }
}

