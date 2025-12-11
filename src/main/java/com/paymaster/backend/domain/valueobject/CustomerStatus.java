package com.paymaster.backend.domain.valueobject;

/**
 * **وضعیت مشتری** (Customer Status).
 * وضعیت‌های مختلف یک مشتری در سیستم را تعریف می‌کند.
 */
public enum CustomerStatus {
    /**
     * مشتری فعال است و می‌تواند قراردادهای جدید ایجاد کند.
     */
    ACTIVE("فعال", "success"),

    /**
     * مشتری غیرفعال است (مثلاً پس از تسویه تمام قراردادها).
     */
    INACTIVE("غیرفعال", "secondary"),

    /**
     * مشتری به دلیل نقض قوانین یا سابقه بد پرداخت، مسدود شده است.
     */
    BLOCKED("مسدود", "danger"),

    /**
     * مشتری در فرآیند ثبت‌نام یا اعتبارسنجی اولیه است.
     */
    PENDING("در انتظار", "warning");

    private final String persianName;
    private final String badgeClass;

    /**
     * سازنده برای اختصاص نام فارسی و کلاس CSS (برای نمایش Badge/Tag).
     * @param persianName نام فارسی وضعیت.
     * @param badgeClass کلاس CSS مربوطه.
     */
    CustomerStatus(String persianName, String badgeClass) {
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