package com.ui.ailvyou.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MultiQueryExpanderDemoTest {


    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;



    @Test
    void testExpand() {
        List<Query> expand = multiQueryExpanderDemo.expand("太原有什么好玩的？太原在哪里等等");
        assertNotNull(expand);
    }
}