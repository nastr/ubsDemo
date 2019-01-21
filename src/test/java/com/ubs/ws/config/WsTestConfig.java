package com.ubs.ws.config;

import com.ubs.ws.service.XChange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Profile(value = "test")
@Configuration
public class WsTestConfig {

    @Bean
    @Primary
    public XChange xChangeService() {
        return mock(XChange.class);
    }
}
