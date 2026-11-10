package com.charliestenacity.loadtest;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class VirtualUserClient {
    private final URI base;
    private final HttpClient client;

    VirtualUserClient(String baseUrl) {
        base = URI.create(baseUrl.trim().replaceAll("/+$", "") + "/");
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    LoginResult login(String username, char[] password) throws Exception {
        String body = "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(new String(password)) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(resolve("/api/login")).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new LoginResult(response.statusCode(), response.body().contains("\"mfaRequired\":true"));
    }

    HttpResponse<Void> get(String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(resolve(endpoint)).timeout(Duration.ofSeconds(30)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private URI resolve(String path) { return base.resolve(path.replaceFirst("^/", "")); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    record LoginResult(int status, boolean mfaRequired) {}
}

