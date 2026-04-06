package com.trading.common.model;

import com.trading.common.constants.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 错误码，0 表示成功
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS, "success", null);
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS, "success", data);
    }
    
    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 失败响应（带数据）
     */
    public static <T> Result<T> error(int code, String message, T data) {
        return new Result<>(code, message, data);
    }
    
    /**
     * 系统错误响应
     */
    public static <T> Result<T> systemError() {
        return error(ErrorCode.SYSTEM_ERROR, "System error");
    }
    
    /**
     * 系统错误响应（带消息）
     */
    public static <T> Result<T> systemError(String message) {
        return error(ErrorCode.SYSTEM_ERROR, message);
    }
    
    /**
     * 参数错误响应
     */
    public static <T> Result<T> invalidParameter(String message) {
        return error(ErrorCode.INVALID_PARAMETER, message);
    }
    
    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized() {
        return error(ErrorCode.UNAUTHORIZED, "Unauthorized");
    }
    
    /**
     * 禁止访问响应
     */
    public static <T> Result<T> forbidden() {
        return error(ErrorCode.FORBIDDEN, "Forbidden");
    }
    
    /**
     * 资源未找到响应
     */
    public static <T> Result<T> notFound(String message) {
        return error(ErrorCode.NOT_FOUND, message);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS;
    }
}
