package com.mts.online_shop.repository;


import com.mts.online_shop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginIgnoreCaseOrEmailIgnoreCase(String login, String email);
    boolean existsByLoginIgnoreCase(String login);
    boolean existsByEmailIgnoreCase(String email);
}
