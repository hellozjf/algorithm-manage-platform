package com.zrar.algorithm;

import com.zrar.algorithm.service.DockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @author Jingfeng Zhou
 */
@SpringBootApplication
@EnableJpaAuditing
@Profile("!unittest")
public class AlgorithmManagePlatformApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AlgorithmManagePlatformApplication.class, args);
    }

    @Autowired
    private DockerService dockerService;

    @Override
    public void run(String... args) throws Exception {
        dockerService.init();
    }
}
