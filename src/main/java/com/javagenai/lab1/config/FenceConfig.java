package com.javagenai.lab1.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(LabPropertiesConfig.class)
public class FenceConfig {

    @Bean(name = "llmFence")
    Executor llmFence(LabPropertiesConfig lab) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("llm-fence-");
        ex.setCorePoolSize(lab.fenceSize());
        ex.setMaxPoolSize(lab.fenceSize());
        ex.setQueueCapacity(0);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ex.initialize();
        return ex;
    }
}
