package com.smartshuttle.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 创建分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 设置分页参数
        paginationInterceptor.setOverflow(true);  // 超过最大页数时回到第一页
        paginationInterceptor.setMaxLimit(1000L); // 单页最大记录数

        // 添加分页插件
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
