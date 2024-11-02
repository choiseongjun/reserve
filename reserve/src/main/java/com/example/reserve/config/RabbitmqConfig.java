package com.example.reserve.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    private static final String TOPIC_EXCHANGE_NAME = "spring-boot-exchange";
    private static final String QUEUE_NAME = "spring-boot";
    private static final String FANOUT_EXCHANGE_NAME = "pubsub-exchange";
    private static final String QUEUE_NAME_SUB1 = "sub1";
    private static final String QUEUE_NAME_SUB2 = "sub2";

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);   // Topic Exchange 타입
    }

    @Bean
    public Binding binding(TopicExchange topicExchange, Queue queue) {
        return BindingBuilder
                .bind(queue)
                .to(topicExchange)
                .with("default.*");
//                .with("hello.key.#");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    // Subscriber용 큐 2개 생성
    @Bean
    public Queue subQueue1() {
        return new Queue(QUEUE_NAME_SUB1, false);
    }
    @Bean
    public Queue subQueue2() {
        return new Queue(QUEUE_NAME_SUB2, false);
    }

    // FanoutExchange 생성
    @Bean
    public FanoutExchange pubsubExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE_NAME);
    }

    // 각 큐에 binding 설정
    @Bean
    public Binding pubsubBinding1(FanoutExchange pubsubExchange, Queue subQueue1) {
        return BindingBuilder.bind(subQueue1).to(pubsubExchange);
    }
    @Bean
    public Binding pubsubBinding2(FanoutExchange pubsubExchange, Queue subQueue2) {
        return BindingBuilder
                .bind(subQueue2)
                .to(pubsubExchange);
    }
    @Bean
    public Binding bindingSubQueue1(Queue subQueue1, TopicExchange topicExchange) {
        return BindingBuilder
                .bind(subQueue1)
                .to(topicExchange)
                .with("subQueue1.*");
    }
}