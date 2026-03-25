package com.ui.ailvyou.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebSearchToolTest {

    @Resource
    private WebmySearchTool webmySearchTool;

    @Test
    public void testSearchWeb() {
        // 测试关键词：Animal Planet
        String query = "太行山图片";
        String result = webmySearchTool.search(query);

        // 打印结果便于观察
        System.out.println("Search Result for '" + query + "':\n" + result);

        // 断言验证
        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.contains("query_results"), "返回结果应包含 query_results 字段");
        assertFalse(result.contains("\"error\":"), "返回结果不应包含错误信息: " + result);
    }

    @Test
    public void testSearchWithEmptyQuery() {
        // 测试空查询
        String result = webmySearchTool.search("");
        assertNotNull(result);
        // 通常 API 会返回错误或空结果，确保程序不会崩溃
        System.out.println("Empty Query Result:\n" + result);
    }
}
