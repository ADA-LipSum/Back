package com.ada.proj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 관리자용 테스트 도구 페이지를 app.admin.path 경로에서만 서빙한다.
 * static 디렉터리 밖(classpath:/admin-console/)에 두어 실제 폴더명으로는 접근할 수 없게 한다.
 */
@Configuration
public class AdminResourceConfig implements WebMvcConfigurer {

    private final AdminAccessProperties adminAccessProperties;

    public AdminResourceConfig(AdminAccessProperties adminAccessProperties) {
        this.adminAccessProperties = adminAccessProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(adminAccessProperties.getPath() + "/**")
                .addResourceLocations("classpath:/admin-console/");
    }
}
