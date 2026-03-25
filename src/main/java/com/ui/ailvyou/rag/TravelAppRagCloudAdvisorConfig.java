package com.ui.ailvyou.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 使用第三方的云知识库的处理结果，进行文档搜索
 *
 *
 */
//@Configuration
@Slf4j
public class TravelAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;


   // @Bean
    public Advisor travelAppRagCloudAdvisor() {
        if (dashScopeApiKey == null || dashScopeApiKey.isBlank()) {
            throw new IllegalArgumentException("DashScope API key must be configured");
        }
        try {
            // 初始化 DashScope API 客户端：用于访问阿里云的 DashScope 大模型服务。
            // new DashScopeApi(dashScopeApiKey); 需要填充参数，使用构造器创建
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(dashScopeApiKey).build();
        final String KNOWLEDGE_INDEX = "旅游攻略";
        //  不了解的东西   ？？ 文档检索器配置 创建 DashScopeDocumentRetriever，用于从指定的知识库索
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(
                dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName(KNOWLEDGE_INDEX)
                        .build());
        // 使用 RetrievalAugmentationAdvisor.builder() 将文档检索器集成到 Advisor 中。 先创建需要进行的文件对象dashScopeDocumentRetriever
            //Advisor 的作用是在生成答案时，先检索知识库中的相关内容，再结合大模型生成更准确的回答（如旅游攻略推荐）
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(dashScopeDocumentRetriever)
                // 自动错误处理逻辑  ！！！！！！
                .queryAugmenter(TravelAppContextualQueryAugmenterFactory.createInstance())
                .build();

    } catch (Exception e) {
        log.error("Failed to initialize DashScope RAG advisor", e);
        throw new RuntimeException("Failed to initialize DashScope RAG advisor", e);
    }
    }

}
