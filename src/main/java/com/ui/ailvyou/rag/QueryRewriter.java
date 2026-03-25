package com.ui.ailvyou.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;


/**
 * 优化用户提示词
 */
@Component
public class QueryRewriter {


    private final QueryTransformer queryTransformer;

    public QueryRewriter(ChatModel dashscopeChatModel){
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 为常量赋值 ，创建查询重写转换器
         queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder).build();

    }

    /**
     * 执行查询重写
     */

    public String rewrite(String prompt){
        Query query = new Query(prompt);
        // 执行查询重写
        Query rewrittenQuery = queryTransformer.transform(query);
        // 返回重写后的查询文本
        return rewrittenQuery.text();
    }



}
