package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "neis.api")
public class NeisProperties {
    private String key;
    private String baseUrl = "https://open.neis.go.kr/hub";
    private String officeCode = "B10";
    private String schoolCode = "7010536";
}
