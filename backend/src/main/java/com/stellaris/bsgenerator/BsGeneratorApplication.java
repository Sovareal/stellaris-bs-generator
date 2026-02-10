package com.stellaris.bsgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BsGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BsGeneratorApplication.class, args);
    }
}
