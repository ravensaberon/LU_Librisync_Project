package com.lulibrisync;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableScheduling
public class LuLibrisyncApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(LuLibrisyncApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(LuLibrisyncApplication.class);
    }
}
