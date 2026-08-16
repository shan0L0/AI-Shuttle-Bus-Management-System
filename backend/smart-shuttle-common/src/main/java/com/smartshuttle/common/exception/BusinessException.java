package com.smartshuttle.common.exception;

import com.smartshuttle.common.constant.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }
    
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }

}
