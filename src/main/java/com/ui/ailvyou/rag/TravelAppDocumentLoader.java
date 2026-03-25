package com.ui.ailvyou.rag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器  将文件的形式转为另一中放到内存 ，中间涉及切片等操作吗？？？？
 *
 * 有问题
 * 填字游戏
 */
@Component
@Slf4j
public class TravelAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;


    TravelAppDocumentLoader(ResourcePatternResolver resourcePatternResolver){
        this.resourcePatternResolver = resourcePatternResolver;
    }


    // 分割结果到内存中
    public List<Document>  loadDocuments(){

        List<Document> allDocument = new ArrayList<>();


        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig  config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeBlockquote(false)
                        .withIncludeCodeBlock(false)
                        .withAdditionalMetadata("filename",filename)
                        .build();


                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocument.addAll(reader.get());
            }


        }catch (IOException e){
            log.error("Failed to load documents", e);
        }
        return allDocument;
    }


}
