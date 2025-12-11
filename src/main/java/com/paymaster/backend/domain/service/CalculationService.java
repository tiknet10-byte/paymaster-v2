package com.paymaster.backend.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * **سرویس محاسبات مالی** (Calculation Service).
 * مسئولیت محاسبه سود، اقساط، جریمه‌ها و تبدیل واحد پول را بر عهده دارد.
 */
@Service
public class CalculationService {

    /**
     * محاسبه **سود ساده** (Simple Interest).
     * فرمول: سود = اصل × نرخ سود سالانه (درصد) × مدت (ماه) / 1200
     * @param principal مبلغ اصل وام.
     * @param annualRate نرخ سود سالانه (به درصد، مثال: 18.0).
     * @param months مدت زمان قرارداد (به ماه).
     * @return مبلغ سود محاسبه شده (به ریال).
     */
    public long calculateSimpleInterest(long principal, double annualRate, int months) {
        if (principal <= 0 || annualRate <= 0 || months <= 0) return 0;

        BigDecimal principalBD = BigDecimal.valueOf(principal);
        // تبدیل نرخ درصد به ضریب (مثلاً 18 / 100)
        BigDecimal rateBD = BigDecimal.valueOf(annualRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal monthsBD = BigDecimal.valueOf(months);
        BigDecimal twelve = BigDecimal.valueOf(12);

        // Interest = Principal * (Rate / 100) * (Months / 12)
        BigDecimal interest = principalBD
                .multiply(rateBD)
                .multiply(monthsBD)
                .divide(twelve, 0, RoundingMode.HALF_UP); // گرد کردن به نزدیک‌ترین عدد صحیح ریال

        return interest.longValue();
    }

    /**
     * محاسبه **مبلغ کل** قرارداد (اصل + سود).
     * @param principal مبلغ اصل.
     * @param annualRate نرخ سود سالانه (به درصد).
     * @param months مدت زمان قرارداد (به ماه).
     * @return مبلغ کل (Total Amount) به ریال.
     */
    public long calculateTotalAmount(long principal, double annualRate, int months) {
        long interest = calculateSimpleInterest(principal, annualRate, months);
        return principal + interest;
    }

    /**
     * محاسبه **مبلغ هر قسط** (Installment Amount).
     * @param totalAmount مبلغ کل (اصل + سود).
     * @param installmentCount تعداد کل اقساط.
     * @return مبلغ هر قسط (با گرد کردن).
     */
    public long calculateInstallmentAmount(long totalAmount, int installmentCount) {
        if (installmentCount <= 0) return totalAmount;
        // از BigDecimal برای گرد کردن دقیق استفاده می‌کنیم
        return BigDecimal.valueOf(totalAmount)
                .divide(BigDecimal.valueOf(installmentCount), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * محاسبه **سهم اصل** (Principal Portion) در هر قسط (به صورت مساوی).
     * @param principal مبلغ اصل کل.
     * @param installmentCount تعداد کل اقساط.
     * @return سهم اصل در هر قسط (با گرد کردن).
     */
    public long calculatePrincipalPortion(long principal, int installmentCount) {
        if (installmentCount <= 0) return principal;
        return BigDecimal.valueOf(principal)
                .divide(BigDecimal.valueOf(installmentCount), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * محاسبه **سهم سود** (Interest Portion) در هر قسط (به صورت مساوی).
     * @param interest مبلغ کل سود.
     * @param installmentCount تعداد کل اقساط.
     * @return سهم سود در هر قسط (با گرد کردن).
     */
    public long calculateInterestPortion(long interest, int installmentCount) {
        if (installmentCount <= 0) return interest;
        return BigDecimal.valueOf(interest)
                .divide(BigDecimal.valueOf(installmentCount), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * محاسبه **جریمه تأخیر** (Penalty).
     * جریمه = مبلغ باقیمانده قسط × نرخ جریمه روزانه (درصد) × تعداد روز تأخیر
     * @param remainingAmount مبلغ باقیمانده قسط (مبلغی که جریمه بر آن اعمال می‌شود).
     * @param dailyPenaltyRate نرخ جریمه روزانه (به درصد، مثال: 0.5).
     * @param delayDays تعداد روزهای تأخیر.
     * @return مبلغ جریمه محاسبه شده (به ریال).
     */
    public long calculatePenalty(long remainingAmount, double dailyPenaltyRate, long delayDays) {
        if (delayDays <= 0 || remainingAmount <= 0 || dailyPenaltyRate <= 0) return 0;

        BigDecimal remaining = BigDecimal.valueOf(remainingAmount);
        // تبدیل نرخ درصد به ضریب (مثلاً 0.5 / 100)
        BigDecimal rate = BigDecimal.valueOf(dailyPenaltyRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal days = BigDecimal.valueOf(delayDays);

        // Penalty = Remaining Amount * (Daily Rate / 100) * Delay Days
        BigDecimal penalty = remaining.multiply(rate).multiply(days);

        return penalty.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * محاسبه **روزهای تأخیر** بین تاریخ سررسید و تاریخ پرداخت.
     * @param dueDate تاریخ سررسید قسط.
     * @param paymentDate تاریخ واقعی پرداخت.
     * @return تعداد روزهای تأخیر (0 اگر زودتر پرداخت شده یا در زمان مقرر باشد).
     */
    public long calculateDelayDays(LocalDate dueDate, LocalDate paymentDate) {
        if (dueDate == null || paymentDate == null) return 0;
        if (paymentDate.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, paymentDate);
        }
        return 0;
    }

    /**
     * محاسبه **روزهای تأخیر تا امروز**.
     * @param dueDate تاریخ سررسید قسط.
     * @return تعداد روزهای تأخیر تا تاریخ امروز.
     */
    public long calculateDelayDaysUntilNow(LocalDate dueDate) {
        return calculateDelayDays(dueDate, LocalDate.now());
    }

    /**
     * محاسبه **مبلغ تسویه زودهنگام** (Early Settlement Amount).
     * مبلغ تسویه = اصل باقیمانده + (سود باقیمانده × (1 - نرخ تخفیف))
     * @param remainingPrincipal اصل باقیمانده برای پرداخت.
     * @param remainingInterest سود باقیمانده محاسبه نشده.
     * @param discountRate نرخ تخفیف سود (به درصد، مثال: 10.0).
     * @return مبلغ کل تسویه زودهنگام (با گرد کردن).
     */
    public long calculateEarlySettlementAmount(long remainingPrincipal, long remainingInterest, double discountRate) {
        if (remainingPrincipal < 0 || remainingInterest < 0) return 0;
        if (discountRate < 0) discountRate = 0;
        if (discountRate > 100) discountRate = 100;

        BigDecimal principal = BigDecimal.valueOf(remainingPrincipal);
        BigDecimal interest = BigDecimal.valueOf(remainingInterest);
        // تبدیل نرخ تخفیف درصد به ضریب (مثلاً 10 / 100)
        BigDecimal discountFactor = BigDecimal.valueOf(discountRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Discounted Interest = remainingInterest * (1 - discountFactor)
        BigDecimal discountedInterest = interest.multiply(BigDecimal.ONE.subtract(discountFactor));

        // Total Settlement = Principal + Discounted Interest
        return principal.add(discountedInterest).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * **فرمت کردن مبلغ به ریال** با جداکننده هزارگان.
     * @param amount مبلغ (به ریال).
     * @return رشته فرمت شده (مثال: 1,500,000 ریال).
     */
    public String formatCurrency(long amount) {
        return String.format("%,d", amount) + " ریال";
    }

    /**
     * **فرمت کردن مبلغ به تومان** با جداکننده هزارگان.
     * @param amount مبلغ (به ریال).
     * @return رشته فرمت شده (مثال: 150,000 تومان).
     */
    public String formatCurrencyToman(long amount) {
        return String.format("%,d", amount / 10) + " تومان";
    }

    /**
     * **تبدیل ریال به تومان**.
     * @param rial مبلغ به ریال.
     * @return مبلغ به تومان.
     */
    public long rialToToman(long rial) {
        return rial / 10;
    }

    /**
     * **تبدیل تومان به ریال**.
     * @param toman مبلغ به تومان.
     * @return مبلغ به ریال.
     */
    public long tomanToRial(long toman) {
        return toman * 10;
    }

    /**
     * محاسبه **درصد پیشرفت** (Progress Percentage).
     * @param paid مبلغ پرداخت شده.
     * @param total مبلغ کل.
     * @return درصد پیشرفت (بین 0 تا 100).
     */
    public int calculateProgressPercentage(long paid, long total) {
        if (total <= 0) return 0;
        // از Math.min استفاده می‌شود تا درصد از 100 تجاوز نکند
        return (int) Math.min(100, (paid * 100L) / total);
    }
}