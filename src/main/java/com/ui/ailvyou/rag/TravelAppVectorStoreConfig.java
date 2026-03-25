package com.ui.ailvyou.rag;



import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


/**
 * 1. 加载文档
 * 2. 转为向量
 * 启动使默认的吗，自动加载bean?????
 */
@Configuration
public class TravelAppVectorStoreConfig {


    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter  myTokenTextSplitter;

    @Resource
    private  MyKeyWordEnricher  myKeyWordEnricher;

    @Bean
    VectorStore TravelAppVectorStore(EmbeddingModel  embeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        List<Document> documents = travelAppDocumentLoader.loadDocuments();
        // 分词
        //List<Document> split = myTokenTextSplitter.splitCustomized(documents);
        // 进行元数据标注 ai
        List<Document> documents1 = myKeyWordEnricher.enrichDoucments(documents);

        // 加载文档，转为向量
        simpleVectorStore.add(documents1);
        return simpleVectorStore;
    }




}
