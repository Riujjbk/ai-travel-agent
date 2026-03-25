package com.ui.ailvyou.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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
import java.util.stream.Collectors;

/**
 * 实现think和act抽象方法
 *
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent{


    // 可用的工具
    private final ToolCallback[] avaliableTools;

    //  保存了工具调用信息的响应
    private ChatResponse toolcallChatResponse;

    // 工具调用管理器
    private final ToolCallingManager  toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文  里面有些控制参数
    private final ChatOptions chatOptions;


    // 这里阿里云的和spring ai原生的有冲突
    public ToolCallAgent(ToolCallback[] avaliableTools){
        super();  // 作用是啥？？
        this.avaliableTools = avaliableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // withProxyToolsCalls 在1.0的版本中改为了  禁用Spring ai内置的工具调用，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false).build();
    }


    /**
     *  处理当前状态并决定下一步行动
     * @return
     */
    @Override
    public boolean think() {
        if (getNextStepPrompt() != null &&!getNextStepPrompt().isEmpty()){
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessages().add(userMessage);
        }
        List<Message>  messagesList = getMessages();
        Prompt prompt = new Prompt(messagesList, chatOptions);
        try {
            // 获取带工具选项的响应
            ChatResponse response = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    //
                    .toolCallbacks(avaliableTools)
                    .call()
                    .chatResponse();

            // 保存响应 用于Act
            this.toolcallChatResponse = response;
            AssistantMessage assistantMessage = response.getResult().getOutput();

            // 输出提示信息
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();

            log.info(getName()+"的思考"+result);
            log.info(getName() +"选择了"+toolCalls.size()+"个工具使用");

            // 这个可以代替上面的吧！！！！
            String toolcollect = toolCalls.stream().map(toolCall -> String.format("工具名称：%s 参数：%S",
                            toolCall.name(),
                            toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            //   助手消息的作用，告诉ai用什么？？？？
            log.info(toolcollect);
            if (toolCalls.isEmpty()){
                // 没有工具调用，记录助手消息，记录啥？？？？我没工具？？
                getMessages().add(assistantMessage);
                return false;
            }else {
                // 有工具调用，无需记录助手消息，因为调用工具是会自动记录
                return true;
            }
        }catch (Exception e){
            log.error("Error running agent",e.getMessage());
            getMessages().add(new AssistantMessage("处理时遇到的错误"+e.getMessage()));
            return false;
        }
    }


    /**
     * 执行工具调用并处理结果
     * @return
     */
    @Override
    public String act() {
        if (!toolcallChatResponse.hasToolCalls())
        return "没有工具调用";
        
        // 调用工具
        Prompt prompt = new Prompt(getMessages(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolcallChatResponse);
        // 记录上下文，conversationHistory 中包含了助手消息和工具调用返回的结果
        setMessages(toolExecutionResult.conversationHistory());

        // 返回工具调用结果  这里为何要强转？？？？？  由message 到子类 toolresponsemessage
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage)CollUtil.getLast(toolExecutionResult.conversationHistory());


        String result = toolResponseMessage.getResponses().stream()
                .map(response -> "工具" + response.name() + "返回结果：" + response.responseData())
                .collect(Collectors.joining("\n"));

        //判断是否调用了终止工具  doTerminate这是啥？？？？？
        boolean b = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));

        if (b){
            setState(AgentState.COMPLETED);
        }

        log.info(result);
        return result;
    }
}
