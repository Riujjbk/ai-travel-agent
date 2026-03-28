package com.ui.ailvyou.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * 使用注册模式
 */
@Configuration
public class ToolRegistration {

    // 依赖注入模式
    @Value("${search-api.api-key}")
    private String seachApiKey;

    @Autowired
    private FileOperationTool fileOperationTool;

    @Autowired
    private PDFGenerationTool pdfGenerationTool;

    @Autowired
    private ResourceDownloadTool resourceDownloadTool;

    @Autowired
    private LocalTimeTool localTimeTool;

    @Autowired
    private WebScrapingTool webScrapingTool;

    @Autowired
    private WebmySearchTool webmySearchTool;

    @Autowired
    private TerminalOperationTool terminalOperationTool;

    @Autowired
    private TerminateTool terminateTool;

    // 工厂模式
    @Bean
    public ToolCallback[] allTools(){
      //  WebmySearchTool searchTool = new WebmySearchTool();
        // 适配器模式 将不同工具转为了ToolCallback[]
        return ToolCallbacks.from(
                fileOperationTool,
                pdfGenerationTool,
                resourceDownloadTool,
                webmySearchTool,
                webScrapingTool,
                terminalOperationTool,
                terminateTool,
                localTimeTool

        );
    }


}
