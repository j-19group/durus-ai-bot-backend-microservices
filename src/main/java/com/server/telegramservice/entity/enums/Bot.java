package com.server.telegramservice.entity.enums;

public enum Bot {

    PHOTO_BOT("ИИ Фотокарточка"),
    VIDEO_BOT("ИИ Видеокарточка"),
    CARD_2_PLUS_1("ИИ 2+1 (фото/видеокарточка)"),
    FITTING_ROOM("ИИ примерочная"),
    CALORIE_COUNTER("ИИ калорий"),
    FOOD_RECOGNIZER("ИИ определить еду по фото"),
    CONSULTANT("ИИ консультант"),
    SELLER("ИИ продавец (услуги маркетологов)"),
    DIETITIAN("ИИ диетолог"),
    CONTENT_MANAGER("ИИ контент-менеджер"),
    ARTIST("ИИ художник"),
    PSYCHOLOGIST("ИИ психолог"),
    FORTUNE_TELLER("ИИ гадалка"),
    TARGETOLOG("ИИ ТАРГЕТОЛОГ"),
    MARKETOLOG("ИИ Маркетолог"),
    NUMEROLOGIST("ИИ нумеролог");

    private final String displayName;

    Bot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

