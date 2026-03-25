package com.ui.ailvyou.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class ContrabandAdvisor implements BaseAdvisor {

    // 添加警告次数
    // 定义违禁词列表  这里无法将赌博与赌场联系！！容易漏出
    private static final List<String> BANNED_WORDS = Arrays.asList(
        "偷渡", "走私", "毒品", "枪支", "赌博",
        "诈骗", "色情", "恐怖主义", "黑客", "洗钱"
    );

    @Override
    public String getName() {
        return "ContrabandAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userMessage = chatClientRequest.prompt().getUserMessage().getText();

/**
 *         // 检查是否包含违禁词
 *         for (String bannedWord : BANNED_WORDS) {
 *             if (userMessage.contains(bannedWord)) {
 *                 log.warn("检测到违禁词: {}", bannedWord);
 *                 // 修改用户消息为警告信息
 *                 return chatClientRequest.mutate()
 *                     .prompt(chatClientRequest.prompt().mutate()
 *                         .userMessage("您的问题包含违禁词，请重新提问")
 *                         .build())
 *                     .build();
 *             }
 *         }
 */

        // 优化1: 使用Aho-Corasick算法进行多模式字符串匹配
        if (containsBannedWords(userMessage)) {
            log.warn("检测到违禁词");
            return createWarningResponse(chatClientRequest);
        }
        return chatClientRequest;
    }

    private ChatClientRequest createWarningResponse(ChatClientRequest originalRequest) {
        /**
         * 在 Spring AI 的 ChatClientRequest 中，mutate() 方法是用来创建一个可修改的副本（即构建器模式中的可变构建器），
         * 允许我们对请求进行修改而不影响原始对象。这是不可变对象设计模式中的常见做法。  具体来说：
         * mutate() 的作用：
         * 创建一个当前请求的可修改副本（构建器）
         * 保持原始请求不变（不可变性）
         * 允许链式调用修改各个属性
         */
        /**
         * 外层 mutate()：修改 ChatClientRequest 对象
         * originalRequest.mutate()...
         * 内层 mutate()：修改 ChatClientRequest 内部的 Prompt 对象
         * originalRequest.prompt().mutate()...
         * 为什么需要双重 mutate()？
         * 不可变对象设计原则：
         * ChatClientRequest 和 Prompt 都是不可变对象
         * 每次修改都需要创建新副本，而不是修改原对象
         * */
        return originalRequest.mutate()
                .prompt(originalRequest.prompt().mutate()
                        .messages(List.of(new UserMessage("您的问题包含违禁内容，请修改后重新提问"))).build())
                .build();
    }

    // 使用更高效的多模式字符串匹配算法
    private boolean containsBannedWords(String text) {
        // 实现1: 使用正则表达式（适合中等数量违禁词）
        String pattern = String.join("|", BANNED_WORDS);
        return Pattern.compile(pattern).matcher(text).find();

    /*
    // 实现2: 使用Trie树（适合大量违禁词）
    Trie trie = new Trie();
    BANNED_WORDS.forEach(trie::insert);
    return trie.containsAny(text);
    */
    }
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        chatClientRequest = before(chatClientRequest, null);
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        return after(chatClientResponse, null);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        chatClientRequest = before(chatClientRequest, null);
        Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux,
            response -> after(response, null));
    }
}
