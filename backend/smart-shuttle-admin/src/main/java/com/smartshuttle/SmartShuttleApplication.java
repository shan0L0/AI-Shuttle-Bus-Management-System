package com.smartshuttle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * 智能车厂管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.smartshuttle.**.mapper")
@EnableAsync
@EnableScheduling
public class SmartShuttleApplication {
    
    public static void main(String[] args) {
        // 打印所有类路径，看驱动在哪
        String classpath = System.getProperty("java.class.path");
        System.out.println("===== 运行时类路径 =====");
        Arrays.stream(classpath.split(System.getProperty("path.separator")))
                .filter(path -> path.contains("mysql") || path.contains("postgresql"))
                .forEach(System.out::println);
        SpringApplication.run(SmartShuttleApplication.class, args);
        System.out.println("""
                
                ╔═══════════════════════════════════════════════════════════════╗
                ║                                                               ║
                ║      智能车厂管理系统 - AI赋能版 启动成功！                         ║
                ║                                                               ║
                ║      API文档: http://localhost:8080/doc.html                   ║
                ║      前端地址: http://localhost:5173                            ║
                ║                                                               ║
                ╚═══════════════════════════════════════════════════════════════╝
                
                """);
    }
}
