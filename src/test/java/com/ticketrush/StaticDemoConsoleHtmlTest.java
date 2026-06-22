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
                "TicketRush 高并发抢票演示台",
                "5 分钟证明一个高并发抢票链路",
                "系统健康检查",
                "抢票主链路",
                "票档检索读模型",
                "虚拟线程压测对比",
                "本次演示结论",
                "id=\"refreshHealthButton\"",
                "id=\"preloadButton\"",
                "id=\"rushButton\"",
                "id=\"repeatRushButton\"",
                "id=\"indexEventButton\"",
                "id=\"searchButton\"",
                "id=\"benchmarkButton\"",
                "href=\"/actuator/health\"",
                "href=\"/actuator/prometheus\""
        );
    }

    @Test
    void shouldUseChineseBusinessActionLabels() throws IOException {
        String html = readIndexHtml();

        assertThat(html).contains(
                "刷新系统状态",
                "初始化库存",
                "发起抢票",
                "重复提交验证幂等",
                "重建活动索引",
                "查询票档",
                "运行压测",
                "清空结果",
                "防超卖",
                "幂等",
                "异步削峰",
                "查看原始接口响应",
                "库存来源：Redis Hash",
                "库存来源：MySQL 库存表",
                "共享 Redis 预热库存",
                "独立数据库库存"
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
                "@media (max-width: 1120px)",
                "@media (max-width: 780px)",
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
                "repeatRushButton",
                "summaryOutcome",
                "strategyHint",
                "rushSourceBadge",
                "searchSummary",
                "benchThroughput",
                "resultBody"
        );
    }

    private String readIndexHtml() throws IOException {
        return Files.readString(INDEX_HTML);
    }
}
