package com.paymaster.backend.presentation.controller;

import com.paymaster.backend.domain.service.CalculationService;
import com.paymaster.backend.domain.service.CustomerService;
import com.paymaster.backend.domain.service.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * کنترلر API برای درخواست‌های AJAX
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final CustomerService customerService;
    private final CalculationService calculationService;
    private final DateUtils dateUtils;

    /**
     * بررسی تکراری نبودن کد ملی
     */
    @GetMapping("/check-national-code")
    public ResponseEntity<Map<String, Object>> checkNationalCode(
            @RequestParam String nationalCode,
            @RequestParam(required = false) Long excludeId) {

        Map<String, Object> result = new HashMap<>();

        // اعتبارسنجی فرمت
        if (!customerService.isValidNationalCode(nationalCode)) {
            result.put("valid", false);
            result.put("message", "کد ملی نامعتبر است");
            return ResponseEntity.ok(result);
        }

        // بررسی تکراری نبودن
        var existing = customerService.findByNationalCode(nationalCode);
        if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
            result.put("valid", false);
            result.put("message", "کد ملی قبلاً ثبت شده است");
            return ResponseEntity.ok(result);
        }

        result.put("valid", true);
        result.put("message", "کد ملی معتبر است");
        return ResponseEntity.ok(result);
    }

    /**
     * بررسی تکراری نبودن موبایل
     */
    @GetMapping("/check-mobile")
    public ResponseEntity<Map<String, Object>> checkMobile(
            @RequestParam String mobile,
            @RequestParam(required = false) Long excludeId) {

        Map<String, Object> result = new HashMap<>();

        // بررسی فرمت
        if (!mobile.matches("^09\\d{9}$")) {
            result.put("valid", false);
            result.put("message", "شماره موبایل نامعتبر است");
            return ResponseEntity.ok(result);
        }

        // بررسی تکراری نبودن
        var existing = customerService.findByMobile(mobile);
        if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
            result.put("valid", false);
            result.put("message", "شماره موبایل قبلاً ثبت شده است");
            return ResponseEntity.ok(result);
        }

        result.put("valid", true);
        result.put("message", "شماره موبایل معتبر است");
        return ResponseEntity.ok(result);
    }

    /**
     * محاسبه اقساط
     */
    @GetMapping("/calculate-installments")
    public ResponseEntity<Map<String, Object>> calculateInstallments(
            @RequestParam Long principal,
            @RequestParam Double rate,
            @RequestParam Integer months) {

        Map<String, Object> result = new HashMap<>();

        long interest = calculationService.calculateSimpleInterest(principal, rate, months);
        long total = principal + interest;
        long installmentAmount = calculationService.calculateInstallmentAmount(total, months);

        result.put("principal", principal);
        result.put("interest", interest);
        result.put("total", total);
        result.put("installmentAmount", installmentAmount);
        result.put("principalFormatted", String.format("%,d", principal));
        result.put("interestFormatted", String.format("%,d", interest));
        result.put("totalFormatted", String.format("%,d", total));
        result.put("installmentFormatted", String.format("%,d", installmentAmount));

        return ResponseEntity.ok(result);
    }

    /**
     * تبدیل تاریخ شمسی به میلادی
     */
    @GetMapping("/convert-date")
    public ResponseEntity<Map<String, Object>> convertDate(@RequestParam String persianDate) {
        Map<String, Object> result = new HashMap<>();

        var gregorianDate = dateUtils.toGregorianDate(persianDate);
        if (gregorianDate != null) {
            result.put("valid", true);
            result.put("gregorian", gregorianDate.toString());
            result.put("persian", persianDate);
            result.put("persianFull", dateUtils.toPersianDateFull(gregorianDate));
        } else {
            result.put("valid", false);
            result.put("message", "تاریخ نامعتبر است");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * جستجوی مشتری
     */
    @GetMapping("/search-customers")
    public ResponseEntity<?> searchCustomers(@RequestParam String q) {
        var customers = customerService.search(q, 0, 10).getContent();
        return ResponseEntity.ok(customers.stream().map(c -> Map.of(
                "id", c.getId(),
                "fullName", c.getFullName(),
                "nationalCode", c.getNationalCode(),
                "mobile", c.getMobile()
        )).toList());
    }
}