package com.charliestenacity.loadtest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LoadEngineSelfTest {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ExampleSecurity/health", LoadEngineSelfTest::healthy);
        server.start();
        try {
            List<LoadSample> samples = new CopyOnWriteArrayList<>();
            LoadEngine.Config config = new LoadEngine.Config(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/ExampleSecurity",
                    "", new char[0], List.of("/health"), 2, 2, 0, 20, 10);
            new LoadEngine(config, samples::add, System.out::println).run();
            Statistics stats = Statistics.from(samples, 2_000);
            if (stats.total() == 0 || stats.failures() != 0 || stats.p95() < 0) {
                throw new AssertionError("Unexpected statistics: " + stats);
            }
            System.out.println("LoadEngineSelfTest passed: " + stats.total() + " requests, p95=" + stats.p95() + " ms");
        } finally {
            server.stop(0);
        }
    }

    private static void healthy(HttpExchange exchange) throws java.io.IOException {
        byte[] body = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}

