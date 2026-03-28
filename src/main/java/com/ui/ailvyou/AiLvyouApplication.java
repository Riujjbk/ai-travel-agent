package com.ui.ailvyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AiLvyouApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLvyouApplication.class, args);
    }

}



