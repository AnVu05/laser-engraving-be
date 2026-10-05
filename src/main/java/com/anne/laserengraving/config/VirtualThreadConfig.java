package com.anne.laserengraving.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cấu hình Executor tận dụng Virtual Threads (Java 21 Project Loom)
 * Cho phép xử lý đồng thời nhiều ảnh và các tác vụ tính toán I/O mà không làm cạn kiệt pool luồng nền tảng (OS threads)
 */
@Configuration
public class VirtualThreadConfig {

    @Bean(name = "laserVirtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
