package com.onmarket.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@SecurityScheme(
        name = "BearerAuth",              // 스키마 이름
        type = SecuritySchemeType.HTTP,   // HTTP 인증 방식
        scheme = "bearer",                // Bearer 토큰
        bearerFormat = "JWT"              // JWT 사용
)
public class SwaggerConfig {


    @Bean
    public OpenAPI openAPI(){

//        SecurityScheme securityScheme = new SecurityScheme()
//                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
//                .in(SecurityScheme.In.HEADER).name("Authorization");
//        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(apiInfo());
//                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
//                .security(Arrays.asList(securityRequirement));
    }
    private Info apiInfo(){
        return new Info()
                .title("ONMarket API Document")
                .version("0.0.1")
                .description("ONMarket의 API 명세서입니다.");
    }

}
