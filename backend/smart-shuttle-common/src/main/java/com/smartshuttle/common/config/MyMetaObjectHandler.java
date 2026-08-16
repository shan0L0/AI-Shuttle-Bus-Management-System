package com.smartshuttle.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 字段自动填充
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        
        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 填充逻辑删除标志
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        
        // 填充创建人（从ThreadLocal获取当前用户ID）
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
            this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
        }
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        
        // 填充更新人
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
        }
    }
    
    /**
     * 获取当前用户ID（从ThreadLocal或SecurityContext）
     */
    private Long getCurrentUserId() {
        // 这里可以从SecurityContextHolder获取当前用户
        // 简化处理，后续可以完善
        return UserContextHolder.getUserId();
    }
}

/**
 * 用户上下文持有者
 */
class UserContextHolder {
    
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }
    
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }
    
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
