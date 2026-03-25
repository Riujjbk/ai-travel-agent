package com.ui.ailvyou.agent.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {

    // 需改参数 无状态等！！！！！
    SUCCESS(0, "已运行"),
    PARAMS_ERROR(40000, "请求参数不能为空"),
    NOT_LOGIN_ERROR(40100, "无状态"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");

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
