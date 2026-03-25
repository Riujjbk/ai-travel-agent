package com.ui.ailvyou.agent;

import com.ui.ailvyou.agent.exception.BusinessException;
import com.ui.ailvyou.agent.exception.ErrorCode;
import com.ui.ailvyou.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 这个应用的作用是啥？？？
 *
 * 抽象基础代理类，用于管理代理状态，和执行流程
 *
 * 提供状态转换，内存管理和基于步骤的执行循环的基础功能
 * 让子类实现 step 方法
 */
@Data
@Slf4j
public abstract class BaseAgent {


    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;



    // 状态  初始态
    private AgentState state = AgentState.IDLE;


    // 执行控制  如何判断运行结束
    private int maxSteps = 10;
    private int currentStep = 0;


    //LLM   聊天模型
    private ChatClient  chatClient;


    // 基于内存的维护，这里可以使用向量数据库？ 还是本身就封装了，
    private List<Message>  messages = new ArrayList<>();


    /**
     * 运行代理
     *
     */
    public String run(String userPrompt){
        if(this.state != AgentState.IDLE){
            throw new BusinessException(ErrorCode.SUCCESS,"代理已经在运行中");
        }
        if (StringUtil.isEmpty(userPrompt)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户提示不能为空");
        }

        // 更改状态
        state = AgentState.RUNNING;

        //执行任务，记录消息上下文
        messages.add(new UserMessage(userPrompt));

        // 保存结果
        List<String> results = new ArrayList<>();

        // 循环执行
//        while (currentStep < maxSteps){
//            String result = step();
//            if (StringUtil.isEmpty(result)){
//                break;
//            }
//            results.add(result);
//            currentStep++;
//        }

        try {
            for (int i = 0; i < maxSteps&&state != AgentState.COMPLETED; i++){
                int stepNumber = i+1;
                currentStep = stepNumber;
                log.info("第{}步",stepNumber);
                String stepResult = step();

                String result = "step"+stepNumber+":"+ stepResult;

                results.add(result);

            }
            if (currentStep >= maxSteps){
                state = AgentState.COMPLETED;
                results.add("Terminated: Reached max steps ("+maxSteps+")");
            }
            //
            return String.join("\n",results);
        }catch (Exception e){
            state = AgentState.FAILED;
            log.error("Error running agent",e);
            return new BusinessException(ErrorCode.SYSTEM_ERROR,"系统异常").getMessage();

        }finally {
            // 清理资源
            this.cleaup();
        }

    }




    /**
     * 运行代理(流式输出)
     *
     */
    public SseEmitter runSteam(String userPrompt) {

        // 创建SseEmitter对象 设置较长的超时时间
        SseEmitter emitter = new SseEmitter(300000L);


        //  使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("代理已经在运行中" + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isEmpty(userPrompt)) {
                    emitter.send("用户提示不能为空");
                    emitter.complete();
                    return;
                }

                // 更改状态
                state = AgentState.RUNNING;

                //执行任务，记录消息上下文
                messages.add(new UserMessage(userPrompt));

                // 保存结果
                //List<String> results = new ArrayList<>();

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.COMPLETED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("第{}步", stepNumber);
                        // 单步执行
                        String stepResult = step();

                        String result = "step" + stepNumber + ":" + stepResult;

                        emitter.send(result);

                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.COMPLETED;
                        emitter.send("Terminated: Reached max steps (" + maxSteps + ")");
                    }
                    //
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.FAILED;
                    log.error("Error running agent", e);
                    try {
                        emitter.send("执行错误" + e.getMessage());
                        emitter.complete();
                    } catch (Exception e1) {
                        emitter.completeWithError(e1);
                    }
                } finally {
                    // 清理资源
                    this.cleaup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }

        });


        // 设置超时和完成回调
        emitter.onTimeout(() -> {
           this.state = AgentState.FAILED;
           this.cleaup();
           log.warn("SSE connection timed out");
        });

        emitter.onCompletion(()->{
            if (this.state == AgentState.RUNNING){
                this.state = AgentState.COMPLETED;
            }
            this.cleaup();
            log.info("SSE connection completed");
        });
        return emitter;
    }

    /**
     * 执行单个步骤 返回值,每一次调用ai的生成结果
     */
    public abstract String step();


    /**
     * 清理资源  ？？什么内存，上下文的清理
     */
    protected void cleaup(){
        // 重写
    }
}
