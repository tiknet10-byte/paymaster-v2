package com.paymaster.backend.domain.valueobject; // فرض می‌شود پکیج این است
/**
 * **وضعیت قسط** (Installment Status).
 * وضعیت‌های مختلف یک قسط در طول چرخه پرداخت را تعریف می‌کند.
 */
public enum InstallmentStatus {
    /**
     * قسط هنوز سررسید نشده یا سررسید شده ولی هنوز پرداخت نشده است.
     */
    PENDING("در انتظار پرداخت", "warning"),

    /**
     * قسط به طور کامل (شامل اصل و جریمه) پرداخت شده است.
     * این وضعیت تسویه نهایی یک قسط را نشان می‌دهد.
     */
    PAID("پرداخت شده", "success"),

    /**
     * سررسید قسط گذشته و هنوز به طور کامل پرداخت نشده است.
     */
    OVERDUE("سررسید گذشته", "danger"),

    /**
     * بخشی از مبلغ قسط پرداخت شده، اما کل بدهی (شامل جریمه) تسویه نشده است.
     */
    PARTIALLY_PAID("پرداخت ناقص", "info"),

    /**
     * وضعیت مشابه PAID، نشان دهنده تسویه کامل.
     * (توجه: در طراحی‌های مرسوم، معمولاً از PAID یا COMPLETED به تنهایی استفاده می‌شود).
     */
    COMPLETED("تسویه نهایی", "primary"); // اصلاح شده برای رفع خطای ساختاری

    private final String persianName;
    private final String badgeClass;

    /**
     * سازنده برای اختصاص نام فارسی و کلاس CSS (برای نمایش Badge/Tag).
     * @param persianName نام فارسی وضعیت.
     * @param badgeClass کلاس CSS مربوطه.
     */
    InstallmentStatus(String persianName, String badgeClass) {
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