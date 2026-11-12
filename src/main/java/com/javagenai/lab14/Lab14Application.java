package com.javagenai.lab14;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.javagenai.lab14.tools.EnterpriseTools;

@SpringBootApplication
public class Lab14Application {

    public static void main(String[] args) {
        SpringApplication.run(Lab14Application.class, args);
    }

    @Bean
    ToolCallbackProvider enterpriseToolCallbacks(EnterpriseTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
