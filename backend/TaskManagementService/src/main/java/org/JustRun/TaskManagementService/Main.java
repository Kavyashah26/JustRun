package org.JustRun.TaskManagementService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello , starting TaskManagement Service!");
        SpringApplication.run(Main.class,args);
    }
}