package com.patternrun;

import com.patternrun.content.ContentSeedProperties;
import com.patternrun.execution.ExecutionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ContentSeedProperties.class, ExecutionProperties.class})
public class PatternRunApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatternRunApplication.class, args);
    }
}