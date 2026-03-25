package com.ui.aiimagesearchmcp;

import com.ui.aiimagesearchmcp.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiImageSearchMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiImageSearchMcpApplication.class, args);
	}


	// 服务提供 服务发现
	@Bean
	public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {

		return MethodToolCallbackProvider.builder()
				.toolObjects(imageSearchTool)
				.build();

	}
}
