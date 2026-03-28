package com.ui.ailvyou.agent;

import com.ui.ailvyou.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class UIManus extends ToolCallAgent {

        public UIManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
                super(allTools);
                this.setName("UIManus");
                String SYSTEM_PROMPT = """
                                You are UIManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                                """;
                this.setSystemPrompt(SYSTEM_PROMPT);
                String NEXT_STEP_PROMPT = """
                                Based on user needs, proactively select the most appropriate tool or combination of tools.
                                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                                After using each tool, clearly explain the results in a user-friendly way.
                                - Important: Only provide the download URL as a clickable Markdown link (e.g., [Download File](URL)).
                                - Prohibited: Do not show any local absolute file paths (e.g., C:\\... or F:\\...) or internal tool response logs to the user.
                                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                                """;
                this.setNextStepPrompt(NEXT_STEP_PROMPT);
                this.setMaxSteps(20);
                // 初始化客户端
                ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                                .defaultAdvisors(new MyLoggerAdvisor())
                                .build();
                this.setChatClient(chatClient);
        }
}
