package org.blackum.blackaddons.common.util.io;

import org.blackum.blackaddons.Blackaddons;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.blackum.blackaddons.common.constants.Constants;

public class HttpUtils {
    public static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
            .build();

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(String url) {
        return sendGetRequest(url, new String[0]);
    }

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(String url, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .header("User-Agent", Constants.BROWSER_USER_AGENT)
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5");

        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }

        HttpRequest request = builder.GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully fetched: " + url);
                    } else {
                        Blackaddons.LOGGER.warn("Fetch failed. Status: " + res.statusCode() + " URL: " + url);
                    }
                    return res;
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error fetching " + url + ": " + e.getMessage());
                    return null;
                });
    }
}
