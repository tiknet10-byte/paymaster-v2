package com.paymaster.backend.domain.entity;

import com.paymaster.backend.domain.valueobject.ContractStatus;
import com.paymaster.backend.domain.valueobject.InstallmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * موجودیت **قرارداد** (Contract Entity).
 * شامل اطلاعات مالی اصلی قرارداد، وضعیت، و ارتباط با مشتری و لیست اقساط.
 */
@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends BaseEntity {

    /**
     * شماره منحصر به فرد قرارداد.
     */
    @Column(name = "contract_number", unique = true, nullable = false, length = 20)
    @NotBlank(message = "شماره قرارداد الزامی است")
    @Size(max = 20, message = "شماره قرارداد نمی‌تواند بیشتر از 20 کاراکتر باشد")
    private String contractNumber;

    /**
     * مبلغ اصلی (وام) قرارداد.
     */
    @NotNull(message = "مبلغ اصلی الزامی است")
    @Min(value = 1000000, message = "حداقل مبلغ 1,000,000 ریال است")
    @Column(name = "principal_amount", nullable = false)
    private Long principalAmount;

    /**
     * نرخ سود سالانه قرارداد (درصد).
     */
    @NotNull(message = "نرخ سود الزامی است")
    @DecimalMin(value = "0.0", message = "نرخ سود نمی‌تواند منفی باشد")
    @DecimalMax(value = "100.0", message = "نرخ سود نمی‌تواند بیشتر از 100% باشد")
    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    /**
     * مبلغ کل سود محاسبه شده.
     */
    @Column(name = "interest_amount")
    private Long interestAmount;

    /**
     * مبلغ کل قرارداد (اصل + سود).
     */
    @NotNull(message = "مبلغ کل الزامی است")
    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    /**
     * تعداد کل اقساط.
     */
    @NotNull(message = "تعداد اقساط الزامی است")
    @Min(value = 1, message = "حداقل 1 قسط الزامی است")
    @Max(value = 60, message = "حداکثر 60 قسط مجاز است")
    @Column(name = "installment_count", nullable = false)
    private Integer installmentCount;

    /**
     * مبلغ هر قسط.
     */
    @Column(name = "installment_amount")
    private Long installmentAmount;

    /**
     * تاریخ شروع قرارداد.
     */
    @NotNull(message = "تاریخ شروع الزامی است")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * تاریخ پایان قرارداد.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * نرخ جریمه دیرکرد روزانه (درصد).
     * مقدار پیش‌فرض: 0.5 درصد.
     */
    @DecimalMin(value = "0.0", message = "نرخ جریمه نمی‌تواند منفی باشد")
    @Column(name = "penalty_rate")
    @Builder.Default
    private Double penaltyRate = 0.5;

    /**
     * وضعیت فعلی قرارداد.
     * مقدار پیش‌فرض: DRAFT (پیش‌نویس).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ContractStatus status = ContractStatus.DRAFT;

    /**
     * توضیحات تکمیلی قرارداد.
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "توضیحات نمی‌تواند بیشتر از 500 کاراکتر باشد")
    private String description;

    // --- روابط ---

    /**
     * ارتباط با مشتری (Many-to-One).
     * هر قرارداد متعلق به یک مشتری است.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "مشتری قرارداد الزامی است")
    private Customer customer;

    /**
     * لیست اقساط مربوط به این قرارداد (One-to-Many).
     * شامل تمامی عملیات Cascade (حذف، به‌روزرسانی و ...).
     * مرتب‌سازی بر اساس تاریخ سررسید (dueDate) به صورت صعودی.
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dueDate ASC")
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();

    // --- متدهای کمکی (Helper Methods) ---

    /**
     * محاسبه مبلغ باقیمانده برای پرداخت.
     * @return مبلغ کل منهای مجموع مبالغ پرداخت شده.
     */
    public Long getRemainingAmount() {
        if (totalAmount == null) return 0L;
        long paidAmount = installments.stream()
                .mapToLong(Installment::getPaidAmount)
                .sum();
        return totalAmount - paidAmount;
    }

    /**
     * تعداد اقساطی که وضعیت آن‌ها "پرداخت شده" است.
     * @return تعداد اقساط پرداخت شده.
     */
    public int getPaidInstallmentsCount() {
        return (int) installments.stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                .count();
    }

    /**
     * محاسبه درصد پیشرفت پرداخت کل مبلغ قرارداد.
     * @return درصد (بین 0 تا 100).
     */
    public int getProgressPercentage() {
        if (totalAmount == null || totalAmount == 0) return 0;

        long paidAmount = installments.stream()
                .mapToLong(Installment::getPaidAmount)
                .sum();

        // جلوگیری از خطای تقسیم بر صفر و اطمینان از نتیجه صحیح
        return (int) Math.min(100, (paidAmount * 100L) / totalAmount);
    }
}