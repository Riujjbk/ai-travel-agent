package com.ui.ailvyou.app;

import com.ui.ailvyou.advisor.ContrabandAdvisor;
import com.ui.ailvyou.advisor.MyLoggerAdvisor;
import com.ui.ailvyou.chatmemory.FileBasedChatMemory;

import com.ui.ailvyou.prompt.MyPromptTemplate;
import com.ui.ailvyou.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TravelApp {



    // 一个自动注入的bean，实现了  * 1. 加载文档
    // * 2. 转为向量
    @Resource
    private VectorStore TravelAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private ToolCallback[] allTools;

   // @Resource
 //   private Advisor  travelAppRagCloudAdvisor;

    @Resource
    private MyPromptTemplate mypromptTemplate;

    private final ChatClient  chatClient;

    //  ai 调用mcp服务，可以从配置文件中找到mcp 注册到这里
    @Resource
    private ToolCallbackProvider  toolCallbackProvider;


    public static final String TRAVEL_AI_ASSISTANT_PROMPT =
            "你是一个专业的「旅游攻略 AI 助手」，需要根据用户的需求提供个性化、可执行的旅行方案与实时建议。\n\n" +

                    "你的能力包括：\n" +
                    "1. 行程规划：根据用户的目的地、时间、预算、兴趣（如美食/拍照/文化/购物）生成详细行程（按天拆分，含时间安排、景点顺序、交通方式）。\n" +
                    "2. 信息整合：融合旅游博主经验、真实用户分享（如小红书风格）与权威信息，提炼高价值建议，避免冗余。\n" +
                    "3. 预算控制：给出费用拆解（交通、住宿、餐饮、门票等），并提供节省成本的优化方案。\n" +
                    "4. 出行准备：提供签证/证件、行李清单、注意事项（天气、文化禁忌、安全提示）。\n" +
                    "5. 实时问题处理：用户在旅途中遇到问题（迷路、行程变更、突发情况）时，给出快速解决方案。\n\n" +

                    "输出要求：\n" +
                    "1. 结构清晰：使用分点/表格/时间轴等格式，提高可读性\n" +
                    "2. 内容真实可执行：避免空泛建议，优先提供具体方案\n" +
                    "3. 具体优先：优先给\"具体方案\"，而不是泛泛推荐\n" +
                    "4. 多方案备选：必要时给出多个备选方案（如高预算/低预算）\n" +
                    "5. 风格要求：简洁但有温度，像经验丰富的旅行达人\n" +
                    "6. 信息补全：当信息不足时，主动向用户提问（如预算、出行人数、偏好）\n\n" +

                    "禁止事项：\n" +
                    "1. 绝对禁止编造不存在的信息\n" +
                    "2. 禁止输出模糊建议（如\"可以去很多地方看看\"）\n" +
                    "3. 禁止忽略用户的预算与时间限制";

    public TravelApp(ChatModel dashscopeChatModel) {

        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";

        FileBasedChatMemory chatMemory = new FileBasedChatMemory(fileDir);


        // ChatMemory chatMemory = new InMemoryChatMemory();

    //    MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository())
    //                .maxMessages(20).build();



        // 将算法何存储进行了解耦， chatMemory

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(TRAVEL_AI_ASSISTANT_PROMPT)
                .defaultAdvisors(
                            MessageChatMemoryAdvisor.builder(chatMemory).build(),
                            // 自定义日志 Advisor，可按需开启
                            new MyLoggerAdvisor(),
                            new ContrabandAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()  成本高了
                )
                .build();

        //        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        String rewrite = queryRewriter.rewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                //.user(message)
                // 重写的输入语句
                .user(rewrite)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 旅行报告
     * java 21 的新特性
     * @param title
     */
     public record TravelReport(String title, List<String> suggestions ) {

    }

    /**
     * AI 恋爱报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public TravelReport doChatWithReport(String message, String chatId) {
        TravelReport travelReport = chatClient
                .prompt()
                .system(TRAVEL_AI_ASSISTANT_PROMPT + "每次对话后生成一份旅行报告，标题为{用户名}的旅行报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);

        log.info("travelReport: {}", travelReport);
        return travelReport;
    }


    /**
     *
     // 新增方法：使用PromptTemplate生成旅行规划
     public String generateTripPlan(Map<String, Object> variables) {
     try {
     return promptTemplate.render(
     promptTemplate.getTripPlannerResource(),
     variables
     );
     } catch (Exception e) {
     log.error("Failed to generate trip plan", e);
     return "抱歉，生成旅行计划时出现错误";
     }
     }

     */


    public TravelReport generateTripPlan(Map<String, Object> variables,String chatId) {

// 1. 从资源文件创建 SystemPromptTemplate（如文档所示）
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(mypromptTemplate.getTripPlannerResource());
// 2. 使用变量渲染模板，得到完整的系统消息字符串 !!!!!!!!!!
        String systemMessageText = systemPromptTemplate.render(variables);

        /**
         * advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)) 这行代码的作用是 为当前对话设置对话记忆（Chat Memory）的会话标识符，具体功能如下：
         * 核心作用
         * 通过 ChatMemory.CONVERSATION_ID 参数绑定当前对话的唯一标识符 chatId，使得后续的对话内容能被关联到同一个会话上下文中。
         * 确保多轮对话的记忆（如历史消息）能基于 chatId 正确存储和检索。
         */
        // 3. 将渲染后的字符串传入 .system() 方法
        return chatClient.prompt()
                .system(systemMessageText) // 直接传入渲染好的字符串
                .user("请根据以上要求，为我生成这份旅行报告。")
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);
    }


    /**
     *  基于向量知识库文档进行回答
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId){

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(10).build();


        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors( MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(new MyLoggerAdvisor())
                // 使用到了
                //.advisors(new QuestionAnswerAdvisor(TravelAppVectorStore))
             //   .advisors(travelAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * 基于工具调用
     */
    public String doChatWithTool(String message, String chatId){

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).
                maxMessages(10).build();

        ChatResponse response = chatClient.prompt()
                .user(message)  // !
                .advisors(s -> s.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call().chatResponse();

        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;

    }







    /**
     * 基于mcp 服务调用
     */
    public String doChatWithMcp(String message, String chatId){

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).
                maxMessages(10).build();

        // !!!!tool()  和 toolCallbacks
        // .toolCallbacks(...) 专门用于注册 ToolCallbackProvider 实现类（如 toolCallbackProvider），
        // 而 .tools(...) 用于注册单个 ToolCallback。
        ChatResponse response = chatClient.prompt()
                .user(message)  // !
                .advisors(s -> s.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call().chatResponse();

        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;

    }



    public Flux<String> doChatWithStream(String message, String chatId){

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository()).
                maxMessages(10).build();

        return chatClient.prompt()
                .user(message)
                .advisors(s -> s.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .stream()
                .content();
    }
}
