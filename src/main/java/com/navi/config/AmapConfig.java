package com.navi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    private String apiKey;
    private String jsApiKey;
}
