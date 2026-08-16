// 文件位置：smart-shuttle-ai/src/test/java/com/smartshuttle/ai/TestConfig.java
package com.smartshuttle.ai.routeOptimizeAdvice;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * AI模块独立的测试配置
 * 不依赖主模块的启动类
 */
@SpringBootApplication
@Import(DynamicDataSourceAutoConfiguration.class)  // 关键：导入动态数据源自动配置
public class TestConfig {
    // AI模块独立的测试配置
    // 可以根据需要导入特定的配置
}