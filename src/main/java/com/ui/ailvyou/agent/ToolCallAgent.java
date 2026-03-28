package com.ui.ailvyou.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ui.ailvyou.agent.exception.BusinessException;
import com.ui.ailvyou.agent.exception.ErrorCode;
import com.ui.ailvyou.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 实现think和act抽象方法
 *
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] avaliableTools;

    // 保存了工具调用信息的响应
    private ChatResponse toolcallChatResponse;

    // 工具调用管理器
    private final ToolCallingManager toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文 里面有些控制参数
    private final ChatOptions chatOptions;

    // 这里阿里云的和spring ai原生的有冲突
    public ToolCallAgent(ToolCallback[] avaliableTools) {
        super(); // 作用是啥？？
        this.avaliableTools = avaliableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // withProxyToolsCalls 在1.0的版本中改为了 禁用Spring ai内置的工具调用，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false).build();
    }

    @Override
    public boolean think() {
        List<Message> messagesList = getMessages();

        // 如果是第一步，且有 nextStepPrompt，则添加它作为引导
        if (getCurrentStep() == 1 && StrUtil.isNotBlank(getNextStepPrompt())) {
            // 检查是否已经添加过，避免重复
            boolean alreadyHasPrompt = messagesList.stream()
                    .anyMatch(m -> m instanceof UserMessage && getNextStepPrompt().equals(m.getText()));
            if (!alreadyHasPrompt) {
                addMessage(new UserMessage(getNextStepPrompt()));
            }
        }

        Prompt prompt = new Prompt(getMessages(), chatOptions);

        try {
            log.info("Agent [{}] 正在思考下一步行动...", getName());
            ChatResponse response = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(avaliableTools)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "LLM 响应为空");
            }

            this.toolcallChatResponse = response;
            AssistantMessage assistantMessage = response.getResult().getOutput();

            String textResponse = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

            // 生成步骤指纹 (工具名 + 参数 + 文本响应的摘要)
            String fingerprint = generateStepFingerprint(textResponse, toolCalls);
            if (recordAndCheckLoop(fingerprint)) {
                log.warn("Agent [{}] 检测到循环，连续 {} 步执行相同操作: {}", getName(), 3, fingerprint);

                // 尝试通过系统消息提醒 LLM 打破循环
                String loopWarning = "你已经连续多次执行了相同的操作，这表明你可能陷入了无限循环。请分析当前状态，尝试不同的工具或参数，或者如果任务无法完成，请直接说明原因并结束。";
                addMessage(new UserMessage(loopWarning));

                // 如果是第二次检测到循环，则强制停止
                if (getStepHistory().stream().filter(f -> f.equals(fingerprint)).count() > 5) {
                    log.error("Agent [{}] 陷入严重无限循环，强制终止任务", getName());
                    setState(AgentState.FAILED);
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务因检测到无限循环而强制终止");
                }
            }

            if (StrUtil.isNotBlank(textResponse)) {
                log.info("Agent [{}] 思考回复: {}", getName(), textResponse);
            }

            if (CollUtil.isNotEmpty(toolCalls)) {
                for (AssistantMessage.ToolCall toolCall : toolCalls) {
                    log.info("Agent [{}] 决定调用工具: {}，参数: {}", getName(), toolCall.name(), toolCall.arguments());
                }
                return true;
            } else {
                log.info("Agent [{}] 任务已完成或无需调用工具", getName());
                addMessage(assistantMessage);
                return false;
            }
        } catch (Exception e) {
            log.error("Agent [{}] 思考过程发生异常: ", getName(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "LLM 思考失败: " + e.getMessage());
        }
    }

    /**
     * 执行工具调用并处理结果
     */
    @Override
    public String act() {
        if (toolcallChatResponse == null || !toolcallChatResponse.hasToolCalls()) {
            return "无需执行工具";
        }

        // 如果检测到循环，且已经在 think 中处理过 (注入了警告)，这里可以决定是否跳过实际执行
        // 为了安全，我们先让它执行一次，但如果连续循环，可以在此处拦截
        if (getState() == AgentState.FAILED) {
            return "任务因检测到无限循环而终止";
        }

        try {
            log.info("Agent [{}] 正在执行工具调用...", getName());
            Prompt prompt = new Prompt(getMessages(), chatOptions);
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolcallChatResponse);

            // 更新上下文消息
            setMessages(toolExecutionResult.conversationHistory());

            // 获取并处理工具执行结果
            Message lastMessage = CollUtil.getLast(toolExecutionResult.conversationHistory());
            if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "工具执行后未获取到结果消息");
            }

            StringBuilder resultSummary = new StringBuilder();
            for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                String toolName = response.name();
                String data = String.valueOf(response.responseData());

                // 敏感信息脱敏：过滤绝对路径 (Windows 和 Linux 风格)
                data = data.replaceAll("[a-zA-Z]:[/\\\\].*?[/\\\\]tmp[/\\\\]", "/tmp/");
                data = data.replaceAll("/.*?/tmp/", "/tmp/");

                log.info("Agent [{}] 工具 [{}] 执行完成，结果长度: {}", getName(), toolName, data.length());
                resultSummary.append(String.format("[%s] 执行成功。%s\n", toolName, data));

                // 检查终止信号
                if (isTerminationTool(toolName) || data.contains("任务结束")) {
                    log.info("Agent [{}] 检测到任务完成信号 ({})", getName(), toolName);
                    setState(AgentState.COMPLETED);
                }
            }

            return resultSummary.toString().trim();
        } catch (Exception e) {
            log.error("Agent [{}] 执行工具时发生异常: ", getName(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 生成当前步骤的唯一指纹 (用于循环检测)
     * TEXT:发现用户需求不明确，需要进一步确|TOOLS:getTravelPreferences({"user": "123});
     * validateBudget({"amount": "5000});
     */
    private String generateStepFingerprint(String text, List<AssistantMessage.ToolCall> toolCalls) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(text)) {
            // 只取文本的前 50 个字符
            sb.append("TEXT:").append(text.substring(0, Math.min(text.length(), 50)));
        }
        if (CollUtil.isNotEmpty(toolCalls)) {
            sb.append("|TOOLS:");
            for (AssistantMessage.ToolCall tc : toolCalls) {
                sb.append(tc.name()).append("(").append(tc.arguments()).append(");");
            }
        }
        return sb.toString();
    }

    /**
     * 判断是否为终止工具
     */
    private boolean isTerminationTool(String toolName) {
        return "doTerminate".equalsIgnoreCase(toolName)
                || "terminate".equalsIgnoreCase(toolName)
                || "finish".equalsIgnoreCase(toolName);
    }
}
