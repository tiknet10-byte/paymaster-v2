package com.paymaster.backend.domain.entity;

import com.paymaster.backend.domain.valueobject.InstallmentStatus;
import com.paymaster.backend.domain.valueobject.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * موجودیت **قسط** (Installment Entity).
 * جزئیات مربوط به هر قسط از یک قرارداد مشخص.
 */
@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installment extends BaseEntity {

    /**
     * شماره ترتیبی قسط در قرارداد.
     */
    @NotNull(message = "شماره قسط الزامی است")
    @Min(value = 1, message = "شماره قسط باید مثبت باشد")
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    /**
     * مبلغ کل قسط (اصل + سود).
     */
    @NotNull(message = "مبلغ قسط الزامی است")
    @Min(value = 0, message = "مبلغ قسط نمی‌تواند منفی باشد")
    @Column(name = "amount", nullable = false)
    private Long amount;

    /**
     * سهم بخش اصلی بدهی (Principal) در این قسط.
     */
    @Column(name = "principal_portion")
    private Long principalPortion;

    /**
     * سهم سود (Interest) در این قسط.
     */
    @Column(name = "interest_portion")
    private Long interestPortion;

    /**
     * تاریخ سررسید (Due Date) قسط.
     */
    @NotNull(message = "تاریخ سررسید الزامی است")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * مبلغی که تا کنون برای این قسط پرداخت شده است.
     * مقدار پیش‌فرض: 0.
     */
    @Column(name = "paid_amount")
    @Builder.Default
    private Long paidAmount = 0L;

    /**
     * مبلغ جریمه دیرکرد (Penalty) محاسبه شده.
     * مقدار پیش‌فرض: 0.
     */
    @Column(name = "penalty_amount")
    @Builder.Default
    private Long penaltyAmount = 0L;

    /**
     * تاریخ و زمان انجام پرداخت (در صورت پرداخت).
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * روش پرداخت استفاده شده.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    /**
     * شماره رسید یا پیگیری پرداخت.
     */
    @Column(name = "receipt_number", length = 50)
    @Size(max = 50, message = "شماره رسید نمی‌تواند بیشتر از 50 کاراکتر باشد")
    private String receiptNumber;

    /**
     * وضعیت فعلی قسط.
     * مقدار پیش‌فرض: PENDING (در انتظار پرداخت).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;

    /**
     * یادداشت‌های مربوط به این قسط.
     */
    @Column(name = "notes", length = 500)
    @Size(max = 500, message = "یادداشت‌ها نمی‌تواند بیشتر از 500 کاراکتر باشد")
    private String notes;

    // --- روابط ---

    /**
     * قرارداد والد این قسط (Many-to-One).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    // --- متدهای کمکی (Helper Methods) ---

    /**
     * محاسبه تعداد روزهای تأخیر (در صورتی که قسط عقب افتاده باشد و پرداخت نشده یا ناقص باشد).
     * @return تعداد روزهای تأخیر. اگر قسط پرداخت شده باشد یا سررسید نرسیده باشد، 0 باز می‌گردد.
     */
    public long getDelayDays() {
        // وضعیت PAID نشان دهنده تسویه کامل است
        if (status == InstallmentStatus.PAID) {
            return 0;
        }

        // اگر وضعیت PARTIALLY_PAID است، هنوز بدهی وجود دارد و ممکن است معوق باشد

        LocalDate today = LocalDate.now();
        // چک می‌کنیم که آیا سررسید گذشته است یا خیر
        if (today.isAfter(dueDate)) {
            // اگر قسط پرداخت نشده یا ناقص پرداخت شده باشد، روزهای تأخیر محاسبه می‌شود.
            // (باید مطمئن شوید که dueDate در این نقطه null نیست)
            return ChronoUnit.DAYS.between(dueDate, today);
        }
        return 0;
    }

    /**
     * محاسبه مبلغ جریمه دیرکرد (Penalty) بر اساس نرخ قرارداد و روزهای تأخیر.
     * جریمه تنها بر روی مبلغ باقیمانده اعمال می‌شود.
     * @return مبلغ جریمه محاسبه شده.
     */
    public Long calculatePenalty() {
        long delayDays = getDelayDays();
        // اطمینان از وجود قرارداد و روزهای تأخیر
        if (delayDays > 0 && contract != null && contract.getPenaltyRate() != null) {
            // نرخ جریمه از درصد به ضریب تبدیل می‌شود
            double penaltyRate = contract.getPenaltyRate() / 100.0;
            // مبلغ باقی‌مانده برای محاسبه جریمه (مبلغ کل قسط منهای مبلغ پرداخت شده)
            long principalForPenalty = amount - paidAmount;

            // محاسبه جریمه (مبلغ * نرخ روزانه * روزهای تأخیر)
            // از Math.round استفاده می‌کنیم تا به نزدیک‌ترین عدد صحیح گرد شود.
            return Math.round(principalForPenalty * penaltyRate * delayDays);
        }
        return 0L;
    }

    /**
     * مبلغ باقیمانده کل (شامل اصل قسط پرداخت نشده + جریمه‌های انباشته شده).
     * توجه: این متد فرض می‌کند penaltyAmount جریمه انباشته شده است.
     * @return مبلغ کل باقیمانده.
     */
    public Long getRemainingAmount() {
        // مبلغ اصلی قسط باقی‌مانده + جریمه‌های انباشته شده
        return (amount - paidAmount) + penaltyAmount;
    }

    /**
     * بررسی می‌کند که آیا قسط سررسید شده و هنوز به طور کامل پرداخت نشده است.
     * @return true اگر سررسید گذشته و وضعیت PAID نباشد.
     */
    public boolean isOverdue() {
        return status != InstallmentStatus.PAID && status != InstallmentStatus.COMPLETED && LocalDate.now().isAfter(dueDate);
    }
}