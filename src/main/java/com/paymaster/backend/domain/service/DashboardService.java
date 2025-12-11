package com.paymaster.backend.domain.service;

import com.paymaster.backend.domain.entity.Contract;
import com.paymaster.backend.domain.entity.Installment;
import com.paymaster.backend.domain.service.DateUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * **سرویس داشبورد و آمار** (Dashboard Service).
 * مسئولیت جمع‌آوری و ارائه آمارهای کلیدی سیستم را بر عهده دارد.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerService customerService;
    private final ContractService contractService;
    private final InstallmentService installmentService;
    private final CalculationService calculationService;
    private final DateUtils dateUtils;

    /**
     * **دریافت آمار کلی داشبورد**.
     * این متد داده‌های آماری را از سرویس‌های مختلف جمع‌آوری و در قالب {@code DashboardStats} ارائه می‌کند.
     * @return شیء حاوی آمار کل سیستم.
     */
    public DashboardStats getDashboardStats() {
        return DashboardStats.builder()
                // آمار مشتریان
                .totalCustomers(customerService.count())
                .activeCustomers(customerService.countActive())
                // آمار قراردادها
                .totalContracts(contractService.count())
                .activeContracts(contractService.countActive())
                // آمار اقساط و مبالغ
                .overdueInstallments(installmentService.countOverdue())
                .totalReceivable(contractService.sumActiveContractsAmount()) // کل مطالبات (مبلغ کل قراردادهای فعال)
                .totalReceived(installmentService.sumPaidAmount())          // کل مبلغ وصول شده
                .totalOverdue(installmentService.sumOverdueAmount())        // مجموع مبلغ باقیمانده اقساط معوق
                .totalPenalty(installmentService.sumPenaltyAmount())        // مجموع جریمه‌های دریافتی
                // تاریخ
                .todayPersianDate(dateUtils.getTodayPersian())
                .build();
    }

    /**
     * دریافت لیستی از اقساطی که سررسید آن‌ها این هفته است.
     * @return لیستی از اقساط.
     */
    public List<Installment> getUpcomingInstallments() {
        return installmentService.findInstallmentsDueThisWeek();
    }

    /**
     * دریافت لیستی از اقساط سررسید گذشته (معوق).
     * @return لیستی از اقساط معوق.
     */
    public List<Installment> getOverdueInstallments() {
        return installmentService.findOverdueInstallments();
    }

    /**
     * دریافت لیستی از قراردادهایی که حداقل یک قسط معوق دارند.
     * @return لیستی از قراردادهای معوق.
     */
    public List<Contract> getOverdueContracts() {
        return contractService.findOverdueContracts();
    }

    /**
     * **کلاس داده آمار داشبورد** (Data Class for Dashboard Statistics).
     * شامل فیلدهای آماری و متدهای کمکی برای محاسبات ساده و فرمت‌دهی.
     */
    @Data
    @Builder
    public static class DashboardStats {
        private long totalCustomers;
        private long activeCustomers;
        private long totalContracts;
        private long activeContracts;
        private long overdueInstallments;
        private long totalReceivable; // کل مطالبات
        private long totalReceived;   // وصول شده
        private long totalOverdue;    // مبلغ معوق
        private long totalPenalty;    // جریمه دریافتی
        private String todayPersianDate;

        /**
         * محاسبه **درصد وصول مطالبات** (Collection Percentage).
         * (کل وصول شده / کل مطالبات) × 100
         * @return درصد (بین 0 تا 100).
         */
        public int getCollectionPercentage() {
            if (totalReceivable <= 0) return 0;
            // استفاده از 100L برای جلوگیری از تبدیل زود هنگام به int در محاسبه
            return (int) Math.min(100, (totalReceived * 100L) / totalReceivable);
        }

        /**
         * فرمت مبلغ کل مطالبات (Total Receivable) با جداکننده هزارگان.
         * @return رشته فرمت شده.
         */
        public String getTotalReceivableFormatted() {
            return String.format("%,d", totalReceivable);
        }

        /**
         * فرمت مبلغ وصول شده (Total Received) با جداکننده هزارگان.
         * @return رشته فرمت شده.
         */
        public String getTotalReceivedFormatted() {
            return String.format("%,d", totalReceived);
        }

        /**
         * فرمت مبلغ معوق (Total Overdue) با جداکننده هزارگان.
         * @return رشته فرمت شده.
         */
        public String getTotalOverdueFormatted() {
            return String.format("%,d", totalOverdue);
        }
    }
}