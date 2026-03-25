package com.ui.ailvyou.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器 的工厂 其实就是兜底策略，为了防止幻觉
 *
 */
public class TravelAppContextualQueryAugmenterFactory {

    /**
     * Prompt是一个基础类，代表一个完整的AI请求，包含：
     *
     * PromptTemplate是一个更高级的类，用于：动态生成Prompt对象
     *
     支持模板化内容（使用如Thymeleaf或类似语法）
     * @return
     */

    public static ContextualQueryAugmenter createInstance(){

        PromptTemplate promptTemplate = new PromptTemplate(
                """
         根据以下上下文回答用户问题：
         {context}
         
         用户问题：{query}
         
         如果问题与旅行无关，请回答：
         抱歉，我只能回答旅行相关知识，无法提供其它帮助
         """
        );


        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .promptTemplate(promptTemplate)
                .build();

    }


}
