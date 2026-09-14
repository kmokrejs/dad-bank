package dev.dadbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DadBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(DadBankApplication.class, args);
    }
}
