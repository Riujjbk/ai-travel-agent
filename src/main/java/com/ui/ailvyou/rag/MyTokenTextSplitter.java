package com.ui.ailvyou.rag;


import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 自定义分词器
 */
@Component
public class MyTokenTextSplitter {


    public List<Document> split(List<Document> documents){
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        return tokenTextSplitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents){
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(200,100,10,5000,true);
        return tokenTextSplitter.apply(documents);
    }

}
