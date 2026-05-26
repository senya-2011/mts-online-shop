package com.mts.online_shop.repository;

import com.mts.online_shop.model.UserTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserTelegramLinkRepository extends JpaRepository<UserTelegramLink, Long> {

    @Query("select l from UserTelegramLink l join fetch l.user u order by u.login, l.telegramUsername")
    List<UserTelegramLink> findAllWithUserOrderByLogin();

    List<UserTelegramLink> findByUserId(Long userId);

    Optional<UserTelegramLink> findByTelegramUsernameIgnoreCase(String telegramUsername);

    boolean existsByTelegramUsernameIgnoreCase(String telegramUsername);

    void deleteByUserIdAndTelegramUsernameIgnoreCase(Long userId, String telegramUsername);
}
