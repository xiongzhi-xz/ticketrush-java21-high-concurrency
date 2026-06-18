package com.ticketrush;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class StaticDemoConsoleHtmlTest {

    private static final Path INDEX_HTML = Path.of("src/main/resources/static/index.html");
    private static final Pattern ID_PATTERN = Pattern.compile("\\bid=\"([^\"]+)\"");

    @Test
    void shouldExposeDemoConsoleEntryPoints() throws IOException {
        String html = readIndexHtml();

        assertThat(html).contains(
                "TicketRush Demo Console",
                "id=\"refreshHealthButton\"",
                "id=\"preloadButton\"",
                "id=\"rushButton\"",
                "id=\"indexEventButton\"",
                "id=\"searchButton\"",
                "id=\"benchmarkButton\"",
                "href=\"/actuator/health\"",
                "href=\"/actuator/prometheus\""
        );
    }

    @Test
    void shouldKeepApiPathsAndCoreJavaScriptFunctions() throws IOException {
        String html = readIndexHtml();

        assertThat(html).contains(
                "function refreshHealth()",
                "function preloadInventory()",
                "function rushTicket()",
                "function indexEvent()",
                "function searchTickets()",
                "function runBenchmark()",
                "/api/system/health",
                "/api/rush/inventory/preload",
                "/api/rush/tickets",
                "/api/search/events/",
                "/api/search/ticket-skus",
                "/api/benchmark/executors"
        );
    }

    @Test
    void shouldKeepResponsiveLayoutGuards() throws IOException {
        String html = readIndexHtml();

        assertThat(html).contains(
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">",
                "@media (max-width: 1100px)",
                "@media (max-width: 760px)",
                "grid-template-columns: 1fr",
                "overflow-wrap: anywhere",
                "min-width: 320px"
        );
    }

    @Test
    void shouldKeepStaticElementIdsUnique() throws IOException {
        String html = readIndexHtml();
        Matcher matcher = ID_PATTERN.matcher(html);
        Set<String> seenIds = new HashSet<>();
        Set<String> duplicateIds = new HashSet<>();

        while (matcher.find()) {
            String id = matcher.group(1);
            if (!seenIds.add(id)) {
                duplicateIds.add(id);
            }
        }

        assertThat(duplicateIds).isEmpty();
        assertThat(seenIds).contains(
                "overallStatus",
                "metricApp",
                "preloadSkuId",
                "rushRequestId",
                "searchSummary",
                "benchThroughput",
                "resultBody"
        );
    }

    private String readIndexHtml() throws IOException {
        return Files.readString(INDEX_HTML);
    }
}
