package com.mts.online_shop.repository;

import com.mts.online_shop.model.UserTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTelegramLinkRepository extends JpaRepository<UserTelegramLink, Long> {

    List<UserTelegramLink> findByUserId(Long userId);

    Optional<UserTelegramLink> findByTelegramUsernameIgnoreCase(String telegramUsername);

    boolean existsByTelegramUsernameIgnoreCase(String telegramUsername);

    void deleteByUserIdAndTelegramUsernameIgnoreCase(Long userId, String telegramUsername);
}
