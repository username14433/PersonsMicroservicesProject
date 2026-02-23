package org.rockend.person_service_.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
public class AppConfig {

    //Создаём бин для получения экземпляра класса Clock
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
