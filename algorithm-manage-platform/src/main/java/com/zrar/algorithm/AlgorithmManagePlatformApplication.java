package com.zrar.algorithm;

import com.zrar.algorithm.service.DockerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.annotation.PostConstruct;

/**
 * @author Jingfeng Zhou
 */
@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class AlgorithmManagePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgorithmManagePlatformApplication.class, args);
    }

    @Autowired
    private DockerService dockerService;

    @PostConstruct
    public void run() {
        dockerService.init();
    }
}
