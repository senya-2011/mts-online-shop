package com.mts.online_shop.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TelegramLinkBulkRequest {

    private List<Entry> items = new ArrayList<>();

    @Getter
    @Setter
    public static class Entry {
        private Long userId;
        private String telegramUsername;
    }
}
