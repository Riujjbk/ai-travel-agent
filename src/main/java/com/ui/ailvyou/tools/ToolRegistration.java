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
    private WebmySearchTool webmySearchTool;

    // 工厂模式
    @Bean
    public ToolCallback[] allTools(){
        FileOperationTool fileOperationTool = new FileOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
      //  WebmySearchTool searchTool = new WebmySearchTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();

        TerminateTool terminateTool = new TerminateTool();
        // 适配器模式 将不同工具转为了ToolCallback[]
        return ToolCallbacks.from(
                fileOperationTool,
                pdfGenerationTool,
                resourceDownloadTool,
                webmySearchTool,
                webScrapingTool,
                terminalOperationTool,
                terminateTool

        );


    }


}
