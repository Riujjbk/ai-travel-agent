package com.ui.ailvyou.controller;

import com.ui.ailvyou.agent.UIManus;
import com.ui.ailvyou.app.TravelApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TravelApp travelApp;

    @Resource
    private ToolCallback[] callbacks;

    @Resource
    private ChatModel dashscopeChatModel;



    @GetMapping("/travel/chat/sync")
    public String doChatWithTravelAppSync(String message,String chatId){
        return travelApp.doChat(message,chatId);
    }


    @GetMapping(value = "/travel/chat/sse" ,produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithTravelAppSSE(String message,String chatId){
        return travelApp.doChatWithStream(message,chatId);
    }


    /**
     * 流式调用Manus 超级智能体
     *
     */
    public SseEmitter doChatWithManus(String message,String chatId){
        UIManus  uiManus = new UIManus(callbacks,dashscopeChatModel);
        return uiManus.runSteam(message);
    }

}
