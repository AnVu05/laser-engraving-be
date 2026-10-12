package com.anne.laserengraving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaserEngravingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaserEngravingApplication.class, args);
    }

}
