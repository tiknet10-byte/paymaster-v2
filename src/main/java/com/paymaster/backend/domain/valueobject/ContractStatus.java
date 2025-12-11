package com.paymaster.backend.domain.valueobject;

/**
 * **وضعیت قرارداد** (Contract Status).
 * وضعیت‌های مختلف یک قرارداد در طول چرخه حیات آن را تعریف می‌کند.
 */
public enum ContractStatus {
    /**
     * قرارداد در حال ایجاد است و هنوز فعال نشده است.
     */
    DRAFT("پیش‌نویس", "secondary"),

    /**
     * قرارداد فعال است و اقساط آن باید پرداخت شوند.
     */
    ACTIVE("فعال", "success"),

    /**
     * تمام اقساط قرارداد به طور کامل پرداخت و تسویه شده‌اند.
     */
    COMPLETED("تسویه شده", "info"),

    /**
     * قرارداد فعال بوده و حداقل یک قسط سررسید گذشته و پرداخت نشده دارد.
     */
    OVERDUE("معوق", "danger"),

    /**
     * قرارداد بنا به دلایلی (مانند فسخ) لغو شده است.
     */
    CANCELLED("لغو شده", "dark");

    private final String persianName;
    private final String badgeClass;

    /**
     * سازنده برای اختصاص نام فارسی و کلاس CSS (برای نمایش Badge/Tag).
     * @param persianName نام فارسی وضعیت.
     * @param badgeClass کلاس CSS مربوطه.
     */
    ContractStatus(String persianName, String badgeClass) {
        this.persianName = persianName;
        this.badgeClass = badgeClass;
    }

    /**
     * دریافت نام فارسی وضعیت.
     * @return نام فارسی.
     */
    public String getPersianName() {
        return persianName;
    }

    /**
     * دریافت کلاس CSS برای نمایش بصری وضعیت (مانند رنگ Badge در فرانت‌اند).
     * @return کلاس CSS.
     */
    public String getBadgeClass() {
        return badgeClass;
    }
}