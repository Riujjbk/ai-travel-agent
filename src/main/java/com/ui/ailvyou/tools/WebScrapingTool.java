package com.ui.ailvyou.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebScrapingTool {

    @Tool(description = "Scrape information from a web page.")
    public String scrapeWeb(@ToolParam(description = "URL of the web page to scrape") String url) {

        try {
            Document document = Jsoup.connect(url).get();
            return document.html();

        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
