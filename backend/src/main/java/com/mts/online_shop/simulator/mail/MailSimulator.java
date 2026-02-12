package com.mts.online_shop.simulator.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailSimulator {

    private static final Logger log = LoggerFactory.getLogger(MailSimulator.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public MailSimulator(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderPaidEmail(String toEmail, Long orderId, float totalPrice) {
        if (toEmail.equals("artemdab228@mail.ru")) {
            String body = "Hello!\n\nYour order #" + orderId + " has been paid.\nTotal: " + totalPrice + "\n\nRegards,\nMTS demo Online Shop";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Your order has been paid");
            message.setText(body);

            log.info("Sending email from='{}' to='{}' subject='{}'", from, toEmail, message.getSubject());
            mailSender.send(message);
        }
    }
}

