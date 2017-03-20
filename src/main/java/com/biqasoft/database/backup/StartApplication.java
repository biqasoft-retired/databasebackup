package com.biqasoft.database.backup;

import com.biqasoft.microservice.communicator.interfaceimpl.annotation.EnableMicroserviceCommunicator;
import com.biqasoft.storage.DefaultStorageService;
import com.biqasoft.storage.s3.DefaultS3FileRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(value = "com.biqasoft", excludeFilters = {@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.biqasoft.entity"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {DefaultStorageService.class, DefaultS3FileRepository.class})})
@Configuration
@EnableScheduling
@EnableAutoConfiguration(exclude={MongoDataAutoConfiguration.class, MongoAutoConfiguration.class,
        SecurityAutoConfiguration.class})
@EnableMicroserviceCommunicator
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }

}
