package com.charliestenacity.loadtest;

record LoadSample(long epochMillis, String endpoint, int status, long latencyMillis, String error) {
    boolean success() { return error == null && status >= 200 && status < 400; }
}

