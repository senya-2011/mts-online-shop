package com.mts.online_shop.model;

/**
 * Admin view: Telegram handle and linked shop user display fields.
 */
public record TelegramLinkAdminItem(String telegramUsername, String userLogin, String userName) {
}
