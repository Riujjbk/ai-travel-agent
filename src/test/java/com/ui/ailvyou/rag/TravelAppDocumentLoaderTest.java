package com.ui.ailvyou.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class TravelAppDocumentLoaderTest {


    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;


    @Test
    void contextLoads() {
        travelAppDocumentLoader.loadDocuments();
    }

}