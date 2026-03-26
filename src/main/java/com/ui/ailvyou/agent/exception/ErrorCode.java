package com.ui.ailvyou.agent.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "操作成功"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    
    // Agent 相关错误码
    AGENT_ALREADY_RUNNING(40001, "代理已经在运行中"),
    AGENT_STEP_ERROR(50002, "代理单步执行失败"),
    AGENT_MAX_STEPS_EXCEEDED(50003, "代理达到最大步数限制"),
    AGENT_STOPPED(50004, "代理已停止执行");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private  final String message;

    ErrorCode(int code,String message){
        this.code=code;
        this.message=message;
    }



}
