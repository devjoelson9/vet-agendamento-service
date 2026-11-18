package br.com.vet.agendamento.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ATENDIMENTO_EXCHANGE = "vet.events";
    public static final String ATENDIMENTO_QUEUE = "agendamento.atendimento";
    public static final String ATENDIMENTO_ROUTING_KEY = "atendimento.*";

    @Bean
    public TopicExchange atendimentoExchange() {
        return new TopicExchange(ATENDIMENTO_EXCHANGE, true, false);
    }

    @Bean
    public Queue atendimentoQueue() {
        return QueueBuilder.durable(ATENDIMENTO_QUEUE).build();
    }

    @Bean
    public Binding atendimentoBinding(Queue atendimentoQueue, TopicExchange atendimentoExchange) {
        return BindingBuilder.bind(atendimentoQueue)
                .to(atendimentoExchange)
                .with(ATENDIMENTO_ROUTING_KEY);
    }
}
