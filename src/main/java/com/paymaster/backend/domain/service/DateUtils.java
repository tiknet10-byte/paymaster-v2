package com.paymaster.backend.domain.service;

import com.github.mfathi91.time.PersianDate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * **ابزار تبدیل تاریخ شمسی و میلادی** (Date Utility).
 * این کلاس مسئولیت تبدیل تاریخ‌های میلادی به شمسی و انجام عملیات تاریخ‌محور را بر عهده دارد.
 */
@Component
public class DateUtils {

    private static final String[] PERSIAN_MONTHS = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
    private static final String[] PERSIAN_DAYS = {"شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"};

    /**
     * تبدیل تاریخ میلادی (LocalDate) به شمسی (فرمت: 1403/09/20).
     * @param gregorianDate تاریخ میلادی.
     * @return رشته تاریخ شمسی فرمت شده.
     */
    public String toPersianDate(LocalDate gregorianDate) {
        if (gregorianDate == null) return "";
        PersianDate persianDate = PersianDate.fromGregorian(gregorianDate);
        return String.format("%04d/%02d/%02d", persianDate.getYear(), persianDate.getMonthValue(), persianDate.getDayOfMonth());
    }

    /**
     * تبدیل تاریخ میلادی به شمسی با نام ماه (فرمت: 20 آذر 1403).
     * @param gregorianDate تاریخ میلادی.
     * @return رشته تاریخ شمسی فرمت شده با نام ماه.
     */
    public String toPersianDateWithMonthName(LocalDate gregorianDate) {
        if (gregorianDate == null) return "";
        PersianDate persianDate = PersianDate.fromGregorian(gregorianDate);
        // از آرایه ماه‌ها استفاده می‌شود (این آرایه از اندیس 0 شروع می‌شود، در حالی که ماه از 1 است).
        return String.format("%d %s %d", persianDate.getDayOfMonth(), PERSIAN_MONTHS[persianDate.getMonthValue() - 1], persianDate.getYear());
    }

    /**
     * تبدیل تاریخ میلادی به شمسی کامل (فرمت: سه‌شنبه 20 آذر 1403).
     * @param gregorianDate تاریخ میلادی.
     * @return رشته تاریخ شمسی فرمت شده به همراه نام روز هفته.
     */
    public String toPersianDateFull(LocalDate gregorianDate) {
        if (gregorianDate == null) return "";
        PersianDate persianDate = PersianDate.fromGregorian(gregorianDate);

        // 1. دریافت روز هفته میلادی (1=دوشنبه تا 7=یکشنبه)
        int dayOfWeek = gregorianDate.getDayOfWeek().getValue();

        // 2. تبدیل روز هفته میلادی به شمسی
        // روز هفته میلادی: دوشنبه (1) ... یکشنبه (7)
        // روز هفته شمسی: شنبه (0) ... جمعه (6)
        // شنبه (اولین روز هفته شمسی) در تقویم میلادی 6 است (شنبه=6).
        // (dayOfWeek % 7) gives 0 for Sunday(7), 1 for Monday(1)... 6 for Saturday(6).
        // ما شنبه را به عنوان اندیس 0 نیاز داریم.
        int persianDayIndex;
        if (dayOfWeek == DayOfWeek.SATURDAY.getValue()) {
            persianDayIndex = 0; // شنبه
        } else if (dayOfWeek == DayOfWeek.SUNDAY.getValue()) {
            persianDayIndex = 1; // یکشنبه
        } else {
            // دوشنبه (1) تا جمعه (5) در میلادی = دوشنبه (2) تا جمعه (6) در آرایه شمسی
            // 1 (دوشنبه میلادی) + 1 = 2 (دوشنبه شمسی)
            // 5 (جمعه میلادی) + 1 = 6 (جمعه شمسی)
            persianDayIndex = dayOfWeek + 1;
        }

        return String.format("%s %d %s %d", PERSIAN_DAYS[persianDayIndex], persianDate.getDayOfMonth(), PERSIAN_MONTHS[persianDate.getMonthValue() - 1], persianDate.getYear());
    }

    /**
     * تبدیل تاریخ شمسی (رشته) به میلادی (LocalDate).
     * فرمت ورودی: YYYY/MM/DD
     * @param persianDateStr رشته تاریخ شمسی.
     * @return تاریخ میلادی (LocalDate) یا null در صورت خطا.
     */
    public LocalDate toGregorianDate(String persianDateStr) {
        if (persianDateStr == null || persianDateStr.isEmpty()) return null;
        String[] parts = persianDateStr.split("/");
        if (parts.length != 3) return null;
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            PersianDate persianDate = PersianDate.of(year, month, day);
            return persianDate.toGregorian();
        } catch (Exception e) {
            // در صورت عدم تطابق فرمت یا تاریخ نامعتبر
            return null;
        }
    }

    /**
     * تبدیل LocalDateTime به شمسی با ساعت (فرمت: 1403/09/20 - 14:30).
     * @param dateTime تاریخ و زمان میلادی.
     * @return رشته تاریخ و زمان شمسی.
     */
    public String toPersianDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        String datePart = toPersianDate(dateTime.toLocalDate());
        String timePart = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        return datePart + " - " + timePart;
    }

    /**
     * دریافت تاریخ شمسی امروز (فرمت: 1403/09/20).
     * @return رشته تاریخ شمسی امروز.
     */
    public String getTodayPersian() {
        return toPersianDate(LocalDate.now());
    }

    /**
     * دریافت نام ماه شمسی (از 1 تا 12).
     * @param month شماره ماه شمسی (1 تا 12).
     * @return نام ماه شمسی.
     */
    public String getPersianMonthName(int month) {
        if (month < 1 || month > 12) return "";
        return PERSIAN_MONTHS[month - 1];
    }

    /**
     * دریافت سال شمسی جاری.
     * @return سال شمسی (مثلاً 1404).
     */
    public int getCurrentPersianYear() {
        return PersianDate.fromGregorian(LocalDate.now()).getYear();
    }

    /**
     * دریافت ماه شمسی جاری (1 تا 12).
     * @return شماره ماه شمسی جاری.
     */
    public int getCurrentPersianMonth() {
        return PersianDate.fromGregorian(LocalDate.now()).getMonthValue();
    }

    /**
     * اضافه کردن تعداد مشخصی ماه به یک تاریخ میلادی بر اساس تقویم شمسی.
     * (عملیات بر روی تاریخ شمسی انجام شده و نتیجه به میلادی برگردانده می‌شود)
     * @param gregorianDate تاریخ میلادی شروع.
     * @param months تعداد ماه‌هایی که باید اضافه شود.
     * @return تاریخ میلادی جدید.
     */
    public LocalDate addMonthsToPersianDate(LocalDate gregorianDate, int months) {
        if (gregorianDate == null) return null;
        PersianDate persianDate = PersianDate.fromGregorian(gregorianDate);
        PersianDate newDate = persianDate.plusMonths(months);
        return newDate.toGregorian();
    }
}