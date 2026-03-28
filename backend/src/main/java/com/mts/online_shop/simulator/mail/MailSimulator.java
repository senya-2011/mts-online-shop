package com.mts.online_shop.simulator.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MailSimulator {

    private static final Logger log = LoggerFactory.getLogger(MailSimulator.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public MailSimulator(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderPaidEmail(String toEmail, Long orderId, BigDecimal totalPrice) {
        if (toEmail.equals("artemdab228@mail.ru")) {
            String body = "Hello!\n\nYour order #" + orderId + " has been paid.\nTotal: " + totalPrice + "\n\nRegards,\nMTS demo Online Shop";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Order Paid - #" + orderId);
            message.setText(body);

            mailSender.send(message);
        }
    }

    public void sendOrderConfirmation(String toEmail, String orderNumber) {
        log.info("Sending order confirmation to {} for order {}", toEmail, orderNumber);
        // Simulate email sending
        if (toEmail != null && orderNumber != null) {
            log.info("Order confirmation sent successfully");
        }
    }

    public void sendOrderCancellation(String toEmail, String orderNumber) {
        log.info("Sending order cancellation to {} for order {}", toEmail, orderNumber);
        // Simulate email sending
        if (toEmail != null && orderNumber != null) {
            log.info("Order cancellation sent successfully");
        }
    }
}

