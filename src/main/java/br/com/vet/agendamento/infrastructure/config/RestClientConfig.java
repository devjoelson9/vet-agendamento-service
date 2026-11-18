package br.com.vet.agendamento.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * O Spring Boot 4 não disponibiliza um {@link RestClient.Builder} apenas com o
 * starter webmvc. Como o {@code IdentidadeClient} depende desse bean, ele é
 * declarado explicitamente aqui.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
