package com.ada.proj.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger UI 등 일부 클라이언트는 multipart 요청에서 JSON 파트(@RequestPart 객체)를
 * Content-Type: application/json이 아닌 text/plain 등으로 전송한다.
 * 이 경우 기본 설정에서는 HttpMediaTypeNotSupportedException(→500)이 발생하므로,
 * Jackson 컨버터가 해당 Content-Type도 JSON으로 읽을 수 있도록 허용 범위를 넓힌다.
 */
@Configuration
public class MultipartJsonPartConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                List<MediaType> supported = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
                supported.add(MediaType.TEXT_PLAIN);
                supported.add(MediaType.APPLICATION_OCTET_STREAM);
                jacksonConverter.setSupportedMediaTypes(supported);
            }
        }
    }
}
