package com.ui.ailvyou.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UIManusTest {

    @Resource
    private  UIManus uiManus;

    @Test
    void run(){
        String userPrompt = """
               我想去天津旅游，请帮我找到一些推荐景点，
               先指定一份计划，后结合一些网络图片下载后插入到PDF中
               并以PDF格式输出 
                """;
        String result = uiManus.run(userPrompt);
        Assertions.assertNotNull(result);
    }


}