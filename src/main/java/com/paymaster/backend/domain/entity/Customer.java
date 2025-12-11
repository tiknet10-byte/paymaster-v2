package com.paymaster.backend.domain.entity;

import com.paymaster.backend.domain.valueobject.ContractStatus;
import com.paymaster.backend.domain.valueobject.CustomerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * موجودیت **مشتری** (Customer Entity).
 * شامل اطلاعات شناسایی و ارتباطی کامل مشتریان سیستم.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    /**
     * نام و نام خانوادگی کامل مشتری.
     */
    @NotBlank(message = "نام مشتری الزامی است")
    @Size(min = 2, max = 100, message = "نام باید بین 2 تا 100 کاراکتر باشد")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * کد ملی منحصر به فرد مشتری.
     */
    @NotBlank(message = "کد ملی الزامی است")
    @Pattern(regexp = "^\\d{10}$", message = "کد ملی باید 10 رقم باشد")
    @Column(name = "national_code", unique = true, nullable = false, length = 10)
    private String nationalCode;

    /**
     * شماره موبایل مشتری.
     */
    @NotBlank(message = "شماره موبایل الزامی است")
    @Pattern(regexp = "^09\\d{9}$", message = "شماره موبایل نامعتبر است (مانند 0912xxxxxxx)")
    @Column(name = "mobile", nullable = false, length = 11)
    private String mobile;

    /**
     * شماره تلفن ثابت مشتری.
     */
    @Column(name = "phone", length = 11)
    private String phone;

    /**
     * آدرس ایمیل مشتری.
     */
    @Email(message = "فرمت ایمیل نامعتبر است")
    @Column(name = "email")
    private String email;

    /**
     * آدرس محل سکونت/کاری مشتری.
     */
    @Column(name = "address", length = 500)
    @Size(max = 500, message = "آدرس نمی‌تواند بیشتر از 500 کاراکتر باشد")
    private String address;

    /**
     * کد پستی 10 رقمی.
     */
    @Column(name = "postal_code", length = 10)
    @Pattern(regexp = "^\\d{10}$", message = "کد پستی باید 10 رقم باشد")
    private String postalCode;

    /**
     * وضعیت فعلی مشتری در سیستم.
     * مقدار پیش‌فرض: ACTIVE (فعال).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * یادداشت‌ها و توضیحات داخلی مربوط به مشتری.
     */
    @Column(name = "notes", length = 1000)
    @Size(max = 1000, message = "یادداشت‌ها نمی‌تواند بیشتر از 1000 کاراکتر باشد")
    private String notes;

    // --- روابط ---

    /**
     * لیست قراردادهای مرتبط با این مشتری (One-to-Many).
     * شامل تمامی عملیات Cascade (حذف، به‌روزرسانی و ...).
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Contract> contracts = new ArrayList<>();

    // --- متدهای کمکی (Helper Methods) ---

    /**
     * تعداد قراردادهایی که وضعیت آن‌ها "فعال" (ACTIVE) است.
     * @return تعداد قراردادهای فعال.
     */
    public int getActiveContractsCount() {
        return (int) contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .count();
    }

    /**
     * مجموع کل مبلغ باقیمانده (بدهی) مشتری از تمام قراردادهای فعال.
     * از متد {@code getRemainingAmount()} در موجودیت Contract استفاده می‌شود.
     * @return مجموع بدهی مشتری.
     */
    public Long getTotalDebt() {
        return contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .mapToLong(Contract::getRemainingAmount)
                .sum();
    }
}