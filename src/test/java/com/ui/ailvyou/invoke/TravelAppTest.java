package com.ui.ailvyou.invoke;


import com.ui.ailvyou.app.TravelApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class TravelAppTest {

    @Resource
    private TravelApp travelApp;



    @Test
    void testChat(){
        String chatId = UUID.randomUUID().toString();

        String  message = "我喜爱徒步，我想走太行山路线";
        String answer = travelApp.doChat(message, chatId);

        Assertions.assertNotNull(answer);  // 检查变量 answer 是否为非 null

        message = "我喜爱徒步，我想走太行山路线";
        answer = travelApp.doChat(message,chatId);

        Assertions.assertNotNull(answer);  // 检查变量 answer 是否为非 null

        message = "我要去哪里旅游";
        answer = travelApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);  // 检查变量 answer 是否为非 null


    }


    @Test
    void doChat() {
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();

        String  message = "你好，我想韩国赌场，但是预算有限";
        TravelApp.TravelReport travelReport = travelApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(travelReport);

    }

    @Test
    void testGenerateTripPlan() {
        String chatId = UUID.randomUUID().toString();

        // 测试正常情况 - 生成旅行计划
        Map<String, Object> variables = Map.of(
                "destination", "北京",
                "duration", "3",
                "budget", "5000",
                "interests", "文化,美食",
                "travelStyle","美食之旅",
                "travelers","学生"
        );

        TravelApp.TravelReport travelReport = travelApp.generateTripPlan(variables, chatId);
        Assertions.assertNotNull(travelReport);


    }

    @Test
    void generateTripPlan() {
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想北京有什么推荐景点吗？";
        String answer =  travelApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithTools() {
            // 测试联网搜索问题的答案
   //         testMessage("天津的著名景点，美食，推荐10个地点");

            // 测试网页抓取：恋爱案例分析
    //        testMessage("请从知乎(https://www.zhihu.com/)中搜索，看北京天安门升国旗的时间规划？");

            // 测试资源下载：图片下载
            testMessage("直接下载一张适合做手机壁纸的田园风景的图片为文件");

            // 测试终端操作：执行代码
    //        testMessage("执行 Python3 脚本来生成数据分析报告");

            // 测试文件操作：保存用户档案
    //        testMessage("保存我的旅游日记为文件");

            // 测试 PDF 生成
    //        testMessage("生成一份'天津7日游'PDF，内容包含美食，景点，旅店等");
        }

        private void testMessage(String message) {
            String chatId = UUID.randomUUID().toString();
            String answer = travelApp.doChatWithTool(message, chatId);
            Assertions.assertNotNull(answer);
        }


    @Test
    void doChatWithMcp() {

   //     String message = "从天津站前往天津海洋馆的路线";
        String chatId = UUID.randomUUID().toString();
        String answer = travelApp.doChatWithMcp("从天津站前往天津海洋馆的路线", chatId);
        Assertions.assertNotNull(answer);
    }
}
