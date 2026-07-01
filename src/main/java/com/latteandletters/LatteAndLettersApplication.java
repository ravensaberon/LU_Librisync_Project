package com.latteandletters;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableScheduling
public class LatteAndLettersApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(LatteAndLettersApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(LatteAndLettersApplication.class);
    }
}
