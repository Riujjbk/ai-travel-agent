package com.ui.ailvyou.tools;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScrapingToolTest {

    @Test
    public void testScrapeWeb() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "https://www.baidu.com";
        String result = tool.scrapeWeb(url);
        assertNotNull(result);
    }
}