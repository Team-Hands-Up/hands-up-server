package com.back.handsUp.config;


import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class JasyptConfig {
    @Value("${jasypt.encryptor.password}")
    private String jasyptPassword;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor(){
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
//        config.setPassword(getJasyptEncryptorPassword());
        config.setPassword(jasyptPassword);
        config.setPoolSize("1");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setStringOutputType("base64");
        config.setKeyObtentionIterations("1000");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        encryptor.setConfig(config);
        return encryptor;
    }

    private String getJasyptEncryptorPassword() {

        try {
            ClassPathResource resource = new ClassPathResource("jasypt/jasypt-encryptor-password.txt");
            String password = Files.readAllLines(Paths.get(resource.getURI())).stream()
                    .collect(Collectors.joining(""));
//            log.info("jasypt password: "+password);
            return password;
        } catch (IOException e) {
            throw new RuntimeException("Not found Jasypt password file.");
        }
    }
}
