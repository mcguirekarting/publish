package com.manhattan.reconciliation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration for email sending functionality.
 */
@Configuration
public class EmailConfig {
    
    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;
    
    @Value("${spring.mail.port:587}")
    private int mailPort;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    @Value("${spring.mail.password:}")
    private String mailPassword;
    
    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String mailAuth;
    
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String mailStartTls;
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        
        if (mailUsername != null && !mailUsername.isEmpty()) {
            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);
        }
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", mailAuth);
        props.put("mail.smtp.starttls.enable", mailStartTls);
        props.put("mail.debug", "true"); // Set to false in production
        
        return mailSender;
    }
}