package com.mts.online_shop.service;

import com.mts.messaging.contracts.TelegramNotificationEnvelope;
import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.messaging.MqttNotificationPublisher;
import com.mts.online_shop.model.TelegramLinkAdminItem;
import com.mts.online_shop.model.User;
import com.mts.online_shop.model.UserTelegramLink;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.repository.UserTelegramLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class TelegramLinkService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserTelegramLinkRepository telegramLinkRepository;
    private final MqttNotificationPublisher mqttNotificationPublisher;

    public TelegramLinkService(
            UserRepository userRepository,
            UserTelegramLinkRepository telegramLinkRepository,
            MqttNotificationPublisher mqttNotificationPublisher
    ) {
        this.userRepository = userRepository;
        this.telegramLinkRepository = telegramLinkRepository;
        this.mqttNotificationPublisher = mqttNotificationPublisher;
    }

    @Transactional
    public void linkTelegramForUser(Long userId, String rawUsername) {
        String username = normalizeUsername(rawUsername);
        if (username.isBlank()) {
            throw new BadRequestException("telegramUsername is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found: " + userId));

        var existing = telegramLinkRepository.findByTelegramUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            if (!existing.get().getUser().getId().equals(userId)) {
                throw new BadRequestException("This Telegram username is already linked to another account");
            }
            publishLinked(user.getId(), username);
            return;
        }

        UserTelegramLink link = new UserTelegramLink();
        link.setUser(user);
        link.setTelegramUsername(username);
        telegramLinkRepository.save(link);

        publishLinked(user.getId(), username);
    }

    @Transactional(readOnly = true)
    public List<TelegramLinkAdminItem> listLinkedAccountsForAdmin() {
        return telegramLinkRepository.findAllWithUserOrderByLogin().stream()
                .map(l -> new TelegramLinkAdminItem(
                        "@" + l.getTelegramUsername(),
                        l.getUser().getLogin(),
                        l.getUser().getName()
                ))
                .toList();
    }

    private void publishLinked(Long userId, String telegramUsername) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        mqttNotificationPublisher.publish(new TelegramNotificationEnvelope(
                TelegramNotificationEnvelope.TYPE_VERIFICATION,
                userId,
                telegramUsername,
                null,
                code
        ));
    }

    public static String normalizeUsername(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceFirst("^@", "").toLowerCase();
    }
}
