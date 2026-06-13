package com.mts.online_shop.camunda;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Commits Camunda identity updates in a separate transaction (WildFly/JTA safe).
 */
@Component
public class CamundaIdentityPasswordWriter {

    private final JdbcTemplate jdbcTemplate;

    public CamundaIdentityPasswordWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertShopUser(String userId, String bareBcryptHash, String displayName) {
        String normalizedId = normalizeId(userId);
        String stored = CamundaBcryptPasswordEncryptor.wrapForCamundaDatabase(bareBcryptHash);
        String firstName = displayName != null && !displayName.isBlank() ? displayName : normalizedId;
        int updated = jdbcTemplate.update(
                "UPDATE act_id_user SET pwd_ = ?, salt_ = NULL, attempts_ = 0, lock_exp_time_ = NULL, "
                        + "rev_ = COALESCE(rev_, 0) + 1 WHERE LOWER(id_) = ?",
                stored,
                normalizedId);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO act_id_user (id_, rev_, first_, last_, email_, pwd_, salt_, attempts_, lock_exp_time_) "
                            + "VALUES (?, 1, ?, '', ?, ?, NULL, 0, NULL)",
                    normalizedId,
                    firstName,
                    normalizedId + "@mts.local",
                    stored);
        }
    }

    /** @deprecated use {@link #upsertShopUser} */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int writeShopBcryptHash(String userId, String bareBcryptHash) {
        upsertShopUser(userId, bareBcryptHash, userId);
        return 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockUser(String userId) {
        jdbcTemplate.update(
                "UPDATE act_id_user SET attempts_ = 0, lock_exp_time_ = NULL WHERE LOWER(id_) = ?",
                normalizeId(userId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int unlockAllLockedUsers() {
        return jdbcTemplate.update(
                "UPDATE act_id_user SET attempts_ = 0, lock_exp_time_ = NULL "
                        + "WHERE attempts_ > 0 OR lock_exp_time_ IS NOT NULL");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int clearLegacySalts() {
        return jdbcTemplate.update(
                "UPDATE act_id_user SET salt_ = NULL WHERE salt_ IS NOT NULL AND salt_ <> ''");
    }

    private static String normalizeId(String userId) {
        return userId.trim().toLowerCase(Locale.ROOT);
    }
}
