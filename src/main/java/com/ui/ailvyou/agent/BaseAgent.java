package com.ui.ailvyou.agent;

import cn.hutool.core.collection.CollUtil;
import com.ui.ailvyou.agent.exception.BusinessException;
import com.ui.ailvyou.agent.exception.ErrorCode;
import com.ui.ailvyou.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
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

    // 状态 初始态
    private AgentState state = AgentState.IDLE;

    // 执行控制 如何判断运行结束
    private int maxSteps = 10;
    private int currentStep = 0;

    // LLM 聊天模型
    private ChatClient chatClient;

    // 基于内存的维护
    private List<Message> messages = new ArrayList<>();

    // 步骤历史 (用于循环检测)
    private List<String> stepHistory = new ArrayList<>();
    private final int MAX_IDENTICAL_STEPS = 3;

    /**
     * 添加消息到上下文
     */
    public void addMessage(Message message) {
        if (message != null) {
            this.messages.add(message);
        }
    }

    /**
     * 记录并检查循环
     * 
     * @param fingerprint 步骤的特征值
     * @return 是否陷入循环
     */
    protected boolean recordAndCheckLoop(String fingerprint) {
        if (StrUtil.isBlank(fingerprint)) {
            return false;
        }

        stepHistory.add(fingerprint);

        // 检查最后 N 个步骤是否完全一致
        if (stepHistory.size() >= MAX_IDENTICAL_STEPS) {
            int size = stepHistory.size();
            boolean allIdentical = true;
            for (int i = 1; i < MAX_IDENTICAL_STEPS; i++) {
                if (!fingerprint.equals(stepHistory.get(size - 1 - i))) {
                    allIdentical = false;
                    break;
                }
            }
            return allIdentical;
        }
        return false;
    }

    /**
     * 获取最近的消息内容 (用于日志或摘要)
     */
    public String getLatestMessageContent() {
        if (CollUtil.isEmpty(messages)) {
            return "";
        }
        Message last = CollUtil.getLast(messages);
        return last.getText();
    }

    /**
     * 运行代理
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_RUNNING);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户提示内容不能为空");
        }

        // 更改状态
        state = AgentState.RUNNING;
        log.info("Agent [{}] 开始执行任务: {}", name, userPrompt);

        // 执行任务，记录消息上下文
        addMessage(new UserMessage(userPrompt));

        // 保存结果
        List<String> results = new ArrayList<>();

        try {
            for (int i = 0; i < maxSteps && state == AgentState.RUNNING; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Agent [{}] 第 {}/{} 步开始执行", name, stepNumber, maxSteps);

                String stepResult = step();
                results.add("Step " + stepNumber + ": " + stepResult);

                // 检查子类是否在 step() 中修改了状态为 COMPLETED
                if (state == AgentState.COMPLETED) {
                    log.info("Agent [{}] 在第 {} 步提前完成任务", name, stepNumber);
                    break;
                }
            }

            if (currentStep >= maxSteps && state == AgentState.RUNNING) {
                state = AgentState.COMPLETED;
                String limitMsg = "Terminated: Reached max steps (" + maxSteps + ")";
                log.warn("Agent [{}] {}", name, limitMsg);
                results.add(limitMsg);
            }

            return String.join("\n\n", results);
        } catch (BusinessException e) {
            state = AgentState.FAILED;
            log.error("Agent [{}] 执行业务异常: {}", name, e.getMessage());
            throw e;
        } catch (Exception e) {
            state = AgentState.FAILED;
            log.error("Agent [{}] 执行系统异常: ", name, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代理执行失败: " + e.getMessage());
        } finally {
            this.cleaup();
        }
    }

    /**
     * 运行代理(流式输出)
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter对象 设置较长的超时时间 (10分钟)
        SseEmitter emitter = new SseEmitter(600000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send(SseEmitter.event().name("error").data("代理已在运行中"));
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send(SseEmitter.event().name("error").data("用户提示内容不能为空"));
                    emitter.complete();
                    return;
                }

                state = AgentState.RUNNING;
                log.info("Agent [{}] 开始流式执行任务: {}", name, userPrompt);
                addMessage(new UserMessage(userPrompt));

                for (int i = 0; i < maxSteps && state == AgentState.RUNNING; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Agent [{}] 第 {}/{} 步开始执行 (SSE)", name, stepNumber, maxSteps);

                    // 发送当前步数状态
                    emitter.send(SseEmitter.event().name("step_start").data("Step " + stepNumber));

                    String stepResult = step();

                    // 发送当前步骤结果
                    emitter.send(SseEmitter.event().name("step_result").data(stepResult));

                    if (state == AgentState.COMPLETED) {
                        log.info("Agent [{}] 在第 {} 步提前完成任务 (SSE)", name, stepNumber);
                        break;
                    }
                }

                if (currentStep >= maxSteps && state == AgentState.RUNNING) {
                    state = AgentState.COMPLETED;
                    String limitMsg = "Terminated: Reached max steps (" + maxSteps + ")";
                    emitter.send(SseEmitter.event().name("info").data(limitMsg));
                }

                emitter.send(SseEmitter.event().name("completed").data("任务执行完成"));
                emitter.complete();
            } catch (Exception e) {
                state = AgentState.FAILED;
                log.error("Agent [{}] 流式执行发生异常: ", name, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("执行失败: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                this.cleaup();
            }
        });

        emitter.onTimeout(() -> {
            log.warn("Agent [{}] SSE connection timed out", name);
            this.state = AgentState.FAILED;
            this.cleaup();
        });

        emitter.onCompletion(() -> {
            log.info("Agent [{}] SSE connection completed", name);
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.COMPLETED;
            }
            this.cleaup();
        });

        return emitter;
    }

    /**
     * 执行单个步骤 返回值,每一次调用ai的生成结果
     */
    public abstract String step();

    /**
     * 清理资源 ？？什么内存，上下文的清理
     */
    protected void cleaup() {
        // 重写
    }
}
