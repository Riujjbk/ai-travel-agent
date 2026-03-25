package com.ui.ailvyou.prompt;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MyPromptTemplate {

    // Other methods to be discussed later
    private static final Logger logger = LoggerFactory.getLogger(MyPromptTemplate.class);

    @Value("classpath:prompt/trip-planner.prompt")
    private Resource tripPlannerResource;

    @Value("classpath:prompt/travel-review-generator.prompt")
    private Resource travelReviewResource;



//    public SystemPromptTemplate createPromptFromResource() {
//        // 2. 使用资源文件创建SystemPromptTemplate
//        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(travelReviewResource);
//
//        // 3. 为模板中的占位符提供变量值，并创建最终的Message
//        // 假设 system-message.st 文件内容为：”You are a helpful assistant that specializes in {domain}. Your name is {name}.”
//       Map<String, Object> variables = Map.of();
 //  Message systemMessage = systemPromptTemplate.createMessage(variables);
//
//        // 4. 可以将此systemMessage与其他Message（如UserMessage）组合，构建完整的Prompt
//        // Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
//        return systemPromptTemplate;
//    }

}
