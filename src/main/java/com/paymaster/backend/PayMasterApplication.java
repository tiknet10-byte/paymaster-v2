package com.paymaster.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * **کلاس اصلی برنامه پی‌مستر** (Main Application Class).
 * این کلاس نقطه ورود برنامه کاربردی Spring Boot است که سیستم مدیریت اقساط را اجرا می‌کند.
 */
@SpringBootApplication
public class PayMasterApplication {

    /**
     * متد اصلی (main) که برنامه Spring Boot را راه‌اندازی می‌کند.
     * @param args آرگومان‌های خط فرمان.
     */
    public static void main(String[] args) {
        SpringApplication.run(PayMasterApplication.class, args);

        // نمایش پیام راه‌اندازی موفقیت‌آمیز در کنسول
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  🚀 PayMaster Started Successfully!    ║");
        System.out.println("║  📍 http://localhost:8081 (Default)   ║");
        System.out.println("║  📊 H2 Console: /h2-console           ║");
        System.out.println("╚════════════════════════════════════════╝");
        // نکته: پورت پیش‌فرض Spring Boot معمولاً 8081 است مگر اینکه در application.properties تنظیم شده باشد.
    }
}