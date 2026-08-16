package com.smartshuttle.common.constant;

/**
 * 错误码常量
 */
public interface ErrorCode {
    
    // ========== 通用错误 ==========
    int SUCCESS = 200;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int INTERNAL_ERROR = 500;
    
    // ========== 认证错误 1001-1099 ==========
    int LOGIN_FAILED = 1001;
    int TOKEN_EXPIRED = 1002;
    int USER_DISABLED = 1003;
    int TOKEN_INVALID = 1004;
    
    // ========== 业务错误 2001-2099 ==========
    int DATA_EXISTS = 2001;
    int DATA_NOT_FOUND = 2002;
    int DATA_REFERENCED = 2003;
    int PARAM_ERROR = 2004;
    
    // ========== AI错误 3001-3099 ==========
    int AI_SERVICE_UNAVAILABLE = 3001;
    int AI_REQUEST_TIMEOUT = 3002;
    int AI_RATE_LIMITED = 3003;
    int AI_RESPONSE_ERROR = 3004;
}
