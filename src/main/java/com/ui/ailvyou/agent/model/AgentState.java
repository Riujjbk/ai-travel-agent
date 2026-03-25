package com.ui.ailvyou.agent.model;

/**
 * 代理执行状态的枚举类
 */
public enum AgentState {


    /**
     * 空闲状态
     */
    IDLE,

    /**
     * 执行中
     */
    RUNNING,

    /**
     * 执行完成
     */
    COMPLETED,

    /**
     * 执行失败
     */
    FAILED

}
