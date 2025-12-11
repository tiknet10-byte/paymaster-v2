package com.paymaster.backend.domain.service;

import com.paymaster.backend.domain.entity.Contract;
import com.paymaster.backend.domain.entity.Installment;
import com.paymaster.backend.domain.repository.ContractRepository;
import com.paymaster.backend.domain.repository.InstallmentRepository;
import com.paymaster.backend.domain.valueobject.ContractStatus;
import com.paymaster.backend.domain.valueobject.InstallmentStatus;
import com.paymaster.backend.domain.valueobject.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * **سرویس مدیریت اقساط** (Installment Service).
 * مسئولیت منطق تجاری مربوط به مدیریت، ثبت پرداخت و به‌روزرسانی وضعیت اقساط را بر عهده دارد.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstallmentService {

    private final InstallmentRepository installmentRepository;
    private final ContractRepository contractRepository;
    private final CalculationService calculationService;

    /**
     * دریافت تمام اقساط، مرتب شده بر اساس تاریخ سررسید (صعودی).
     * @return لیستی از اقساط.
     */
    public List<Installment> findAll() {
        return installmentRepository.findAll(Sort.by(Sort.Direction.ASC, "dueDate"));
    }

    /**
     * جستجوی قسط بر اساس شناسه (ID).
     * @param id شناسه قسط.
     * @return یک Optional شامل قسط.
     */
    public Optional<Installment> findById(Long id) {
        return installmentRepository.findById(id);
    }

    /**
     * بازیابی اقساط یک قرارداد مشخص، مرتب شده بر اساس شماره قسط.
     * @param contractId شناسه قرارداد.
     * @return لیستی از اقساط قرارداد.
     */
    public List<Installment> findByContractId(Long contractId) {
        return installmentRepository.findByContractIdOrderByInstallmentNumberAsc(contractId);
    }

    /**
     * بازیابی اقساط سررسید گذشته (معوق) که وضعیت آن‌ها "در انتظار پرداخت" است.
     * @return لیستی از اقساط معوق.
     */
    public List<Installment> findOverdueInstallments() {
        return installmentRepository.findOverdueInstallments(LocalDate.now());
    }

    /**
     * بازیابی اقساطی که سررسید آن‌ها در هفته جاری است (از امروز تا ۷ روز آینده).
     * @return لیستی از اقساط سررسید این هفته.
     */
    public List<Installment> findInstallmentsDueThisWeek() {
        LocalDate today = LocalDate.now();
        // تاریخ پایان هفته (امروز + ۶ روز یا ۷ روز آینده، بسته به منطق)
        // برای پیدا کردن اقساط 'این هفته' بهتر است از تاریخ شروع و پایان هفته شمسی استفاده شود.
        // با فرض اینکه منطق تاریخ در DateUtils پیاده شده است، اینجا صرفاً یک بازه 7 روزه را در نظر می‌گیریم.
        LocalDate endOfWeek = today.plusDays(7);
        return installmentRepository.findInstallmentsDueThisWeek(today, endOfWeek);
    }

    /**
     * **ثبت پرداخت قسط**.
     * این عملیات شامل محاسبه جریمه، به‌روزرسانی مبلغ پرداخت شده و تعیین وضعیت جدید قسط است.
     *
     * @param installmentId شناسه قسط.
     * @param paidAmount مبلغی که مشتری پرداخت کرده است.
     * @param paymentMethod روش پرداخت.
     * @param receiptNumber شماره رسید/پیگیری.
     * @param notes توضیحات.
     * @return قسط به‌روزرسانی شده.
     * @throws IllegalArgumentException در صورت یافت نشدن قسط یا پرداخت قبلی.
     */
    @Transactional
    public Installment payInstallment(Long installmentId, Long paidAmount, PaymentMethod paymentMethod, String receiptNumber, String notes) {

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("خطا: قسط یافت نشد"));

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new IllegalArgumentException("خطا: این قسط قبلاً به طور کامل پرداخت شده است.");
        }

        // 1. محاسبه جریمه تاخیر
        long penalty = 0;
        if (installment.isOverdue()) {
            // از متد داخلی قسط برای محاسبه جریمه بر اساس نرخ قرارداد استفاده می‌شود
            penalty = installment.calculatePenalty();
        }

        // 2. محاسبه مبلغ کل بدهی (باقیمانده قسط + جریمه)
        // مبلغ اصلی قسط باقی‌مانده: amount - paidAmount
        // مبلغ کل بدهی: (amount - paidAmount) + penalty
        long totalDue = (installment.getAmount() - installment.getPaidAmount()) + penalty;

        if (paidAmount < 0) {
            throw new IllegalArgumentException("خطا: مبلغ پرداخت شده نمی‌تواند منفی باشد.");
        }
        if (paidAmount == 0) {
            throw new IllegalArgumentException("خطا: مبلغ پرداخت شده صفر است. اگر پرداخت کامل است، از QuickPay استفاده کنید.");
        }

        // 3. به‌روزرسانی فیلدهای قسط
        installment.setPaidAmount(installment.getPaidAmount() + paidAmount);

        // جریمه محاسبه شده را در فیلد مربوطه ذخیره می‌کنیم
        installment.setPenaltyAmount(installment.getPenaltyAmount() + penalty);

        installment.setPaymentDate(LocalDateTime.now());
        installment.setPaymentMethod(paymentMethod);
        installment.setReceiptNumber(receiptNumber);
        installment.setNotes(notes);

        // 4. تعیین وضعیت جدید
        // اگر کل مبلغ بدهی (اصلی + جریمه) پرداخت شده باشد.
        if (installment.getPaidAmount() >= installment.getAmount() + installment.getPenaltyAmount()) {
            installment.setStatus(InstallmentStatus.PAID);
        } else if (installment.getPaidAmount() > 0) {
            installment.setStatus(InstallmentStatus.PARTIALLY_PAID);
        } else {
            // در غیر این صورت، وضعیت PENDING/OVERDUE (که در background job به‌روزرسانی می‌شود) حفظ می‌شود.
        }

        installment = installmentRepository.save(installment);

        // 5. بررسی تکمیل قرارداد (در صورت لزوم)
        checkContractCompletion(installment.getContract().getId());

        return installment;
    }

    /**
     * **پرداخت سریع** (Quick Pay).
     * پرداخت مبلغ باقیمانده قسط (اصل + جریمه‌های انباشته) به صورت نقدی و بدون جزئیات.
     * @param installmentId شناسه قسط.
     * @return قسط به‌روزرسانی شده.
     */
    @Transactional
    public Installment quickPay(Long installmentId) {
        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new IllegalArgumentException("خطا: قسط یافت نشد"));

        // محاسبه مبلغ باقیمانده کل (شامل جریمه‌های انباشته شده)
        long totalRemainingDue = installment.getRemainingAmount();

        // اگر قبلاً پرداخت کامل شده باشد
        if (totalRemainingDue <= 0 && installment.getStatus() == InstallmentStatus.PAID) {
            throw new IllegalArgumentException("خطا: این قسط قبلاً به طور کامل پرداخت شده است.");
        }

        // فراخوانی متد اصلی پرداخت برای تسویه کامل
        return payInstallment(installmentId, totalRemainingDue, PaymentMethod.CASH, "QUICKPAY-" + System.currentTimeMillis(), "تسویه سریع قسط");
    }

    /**
     * **بررسی تکمیل قرارداد**.
     * در صورتی که تمام اقساط یک قرارداد پرداخت شده باشند، وضعیت قرارداد را به COMPLETED تغییر می‌دهد.
     * @param contractId شناسه قرارداد.
     */
    private void checkContractCompletion(Long contractId) {
        Contract contract = contractRepository.findById(contractId).orElse(null);

        // اگر قرارداد یافت نشد یا قبلاً COMPLETED شده است، برگرد
        if (contract == null || contract.getStatus() == ContractStatus.COMPLETED) return;

        // بررسی اینکه آیا تمام اقساط وضعیت PAID دارند
        // تنها InstallmentStatus.PAID باید چک شود زیرا COMPLETED در این Enum تعریف نشده است.
        boolean allPaid = contract.getInstallments().stream()
                .allMatch(i -> i.getStatus() == InstallmentStatus.PAID);

        if (allPaid) {
            // اگر تمام اقساط پرداخت شده‌اند، وضعیت قرارداد را به COMPLETED تغییر بده
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
        }
    }

    /**
     * **به‌روزرسانی وضعیت اقساط معوق** (تغییر وضعیت از PENDING به OVERDUE).
     * @return تعداد اقساطی که وضعیت آن‌ها به‌روزرسانی شده است.
     */
    @Transactional
    public int updateOverdueInstallments() {
        return installmentRepository.updateOverdueInstallments(LocalDate.now());
    }

    /**
     * بازیابی **اولین قسط** (بر اساس شماره) یک قرارداد که هنوز "در انتظار پرداخت" است.
     * @param contractId شناسه قرارداد.
     * @return اولین قسط پرداخت نشده.
     */
    public Optional<Installment> findFirstPendingInstallment(Long contractId) {
        // استفاده از متد اصلاح شده در Repository (که یک Optional برمی‌گرداند)
        return installmentRepository.findFirstByContractIdAndStatusOrderByInstallmentNumberAsc(contractId, InstallmentStatus.PENDING);
    }

    /**
     * شمارش تعداد اقساط سررسید گذشته (معوق).
     * @return تعداد اقساط معوق.
     */
    public long countOverdue() {
        return installmentRepository.countOverdueInstallments(LocalDate.now());
    }

    /**
     * محاسبه مجموع مبلغ پرداخت شده (paidAmount) در سیستم.
     * @return مجموع مبلغ پرداخت شده.
     */
    public long sumPaidAmount() {
        Long sum = installmentRepository.sumPaidAmount();
        return sum != null ? sum : 0;
    }

    /**
     * محاسبه مجموع مبلغ باقیمانده (اصلی) اقساط معوق.
     * @return مجموع مبلغ باقیمانده اقساط معوق.
     */
    public long sumOverdueAmount() {
        Long sum = installmentRepository.sumOverdueAmount(LocalDate.now());
        return sum != null ? sum : 0;
    }

    /**
     * محاسبه مجموع جریمه‌های دریافتی (penaltyAmount).
     * @return مجموع جریمه‌ها.
     */
    public long sumPenaltyAmount() {
        Long sum = installmentRepository.sumPenaltyAmount();
        return sum != null ? sum : 0;
    }
}