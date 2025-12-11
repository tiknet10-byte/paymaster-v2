package com.paymaster.backend.presentation.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest; // اگر از Jakarta EE استفاده می‌کنید

/**
 * **مدیریت سراسری خطاها** (Global Exception Handler).
 * این کلاس برای مدیریت استثناهای (Exception) پرتاب شده توسط تمام کنترلرهای برنامه استفاده می‌شود.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * **مدیریت خطاهای عمومی** (General Exception Handler).
     * استثناهای پیش‌بینی نشده (مانند خطاهای سیستمی یا Runtime) را مدیریت کرده و به صفحه خطای عمومی هدایت می‌کند.
     *
     * @param e استثنای رخ داده.
     * @param model مدل برای افزودن پیام خطا.
     * @return نام ویو (View) خطای عمومی.
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        // ثبت خطا در لاگ (باید از یک Logger استفاده شود، اما در اینجا فقط پیام اضافه می‌شود)
        System.err.println("Unexpected error: " + e.getMessage());

        model.addAttribute("errorMessage", "خطای غیرمنتظره‌ای رخ داد: " + e.getMessage());
        // فرض می‌شود یک ویو به نام "error.html" وجود دارد.
        return "error";
    }

    /**
     * **مدیریت خطاهای IllegalArgumentException** (خطای ورودی نامعتبر).
     * این خطاها معمولاً ناشی از اعتبارسنجی منطق کسب‌وکار هستند و کاربر باید به صفحه داشبورد/قبلی هدایت شود.
     *
     * @param e استثنای IllegalArgumentException.
     * @param redirectAttributes برای انتقال پیام خطا بعد از Redirect.
     * @return دستور Redirect به داشبورد.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, RedirectAttributes redirectAttributes) {
        System.err.println("IllegalArgumentException: " + e.getMessage());

        // اضافه کردن پیام خطا به عنوان Flash Attribute تا پس از Redirect قابل دسترسی باشد.
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        // هدایت کاربر به صفحه داشبورد یا صفحه اصلی.
        return "redirect:/dashboard";
    }

    /**
     * **مدیریت خطاهای 404 (صفحه یافت نشد)**
     * (اختیاری: برای Spring MVC، این مورد معمولاً توسط متد یا کانفیگ پیش‌فرض مدیریت می‌شود، اما برای کنترل بیشتر می‌توان آن را اضافه کرد)
     *
     * @param request درخواست HTTP
     * @param model مدل برای افزودن پیام خطا
     * @return نام ویو (View) خطای 404
     */
    /*
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", "صفحه‌ای که به دنبال آن بودید، یافت نشد (404).");
        return "error-404";
    }
    */
}