package com.ui.ailvyou.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class WebmySearchTool {

    @Value("${search-api.api-key}")
    private String apiKey;

    @Value("${search-api.url:https://www.searchapi.io/api/v1/search}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient();

    /**
     * 使用 SearchAPI 执行搜索功能，返回百度搜索结果。
     * 自动过滤广告和无关内容，只保留与搜索内容相关的自然搜索结果。
     * 
     * @param query 搜索关键词
     * @return 过滤后的搜索结果 JSON 字符串
     */
    @Tool(description = "根据关键词执行百度搜索，获取相关的网页搜索结果。已自动过滤广告。")
    public String search(@ToolParam(description = "要搜索的关键词或问题") String query) {
        log.info("Executing filtered search for query: {}", query);

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(apiUrl)).newBuilder();
        urlBuilder.addQueryParameter("engine", "baidu");
        urlBuilder.addQueryParameter("q", query);
        urlBuilder.addQueryParameter("api_key", apiKey);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Search failed with status: {}, body: {}", response.code(),
                        response.body() != null ? response.body().string() : "empty");
                return "{\"error\": \"Search request failed with status " + response.code() + "\"}";
            }
            String responseBody = response.body() != null ? response.body().string() : "";

            // 执行过滤逻辑
            return filterResults(responseBody);

        } catch (IOException e) {
            log.error("Error executing search request", e);
            return "{\"error\": \"Search request failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 过滤搜索结果：
     * 1. 只保留 organic_results（自然搜索结果）
     * 2. 去除广告、新闻推荐等无关内容
     * 3. 精简字段，只保留 title, link, snippet，减少 Token 消耗
     */
    private String filterResults(String rawJson) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(rawJson);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            if (organicResults == null || organicResults.isEmpty()) {
                return "{\"results\": [], \"message\": \"No organic results found.\"}";
            }

            List<JSONObject> filteredList = new ArrayList<>();
            for (int i = 0; i < organicResults.size(); i++) {
                JSONObject result = organicResults.getJSONObject(i);

                // 只提取核心字段，去除广告标识和多余的图片链接
                JSONObject cleanItem = new JSONObject();
                cleanItem.set("title", result.getStr("title"));
                cleanItem.set("link", result.getStr("link"));
                cleanItem.set("snippet", result.getStr("snippet"));

                filteredList.add(cleanItem);
            }

            JSONObject finalResult = new JSONObject();
            finalResult.set("query_results", filteredList);
            return finalResult.toString();

        } catch (Exception e) {
            log.error("Error filtering search results", e);
            return rawJson; // 过滤失败则返回原内容
        }
    }
}