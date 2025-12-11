package com.paymaster.backend.domain.repository;

import com.paymaster.backend.domain.entity.Installment;
import com.paymaster.backend.domain.valueobject.InstallmentStatus;
import com.paymaster.backend.domain.valueobject.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * **ریپازیتوری اقساط** (Installment Repository).
 * امکان دسترسی و مدیریت داده‌های اقساط قراردادها در دیتابیس را فراهم می‌کند.
 */
@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    /**
     * بازیابی اقساط یک قرارداد مشخص، مرتب شده بر اساس شماره قسط به صورت صعودی.
     * @param contractId شناسه قرارداد.
     * @return لیستی از اقساط قرارداد.
     */
    List<Installment> findByContractIdOrderByInstallmentNumberAsc(Long contractId);

    /**
     * بازیابی اقساط یک قرارداد مشخص با قابلیت صفحه‌بندی.
     * @param contractId شناسه قرارداد.
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از اقساط قرارداد.
     */
    Page<Installment> findByContractId(Long contractId, Pageable pageable);

    /**
     * بازیابی اقساط بر اساس وضعیت مشخص.
     * @param status وضعیت قسط.
     * @return لیستی از اقساط.
     */
    List<Installment> findByStatus(InstallmentStatus status);

    /**
     * شمارش تعداد اقساط بر اساس وضعیت.
     * @param status وضعیت قسط.
     * @return تعداد اقساط.
     */
    long countByStatus(InstallmentStatus status);

    /**
     * بازیابی اقساط یک قرارداد مشخص با وضعیت خاص.
     * @param contractId شناسه قرارداد.
     * @param status وضعیت قسط.
     * @return لیستی از اقساط.
     */
    List<Installment> findByContractIdAndStatus(Long contractId, InstallmentStatus status);

    /**
     * بازیابی اقساطی که سررسید آن‌ها در یک تاریخ مشخص است و در وضعیت "در انتظار پرداخت" قرار دارند.
     * **نکته:** در این متد وضعیت 'PENDING' را برای جستجوی دقیق‌تر اضافه کردم.
     * @param dueDate تاریخ سررسید.
     * @param status وضعیت قسط.
     * @return لیستی از اقساط.
     */
    List<Installment> findByDueDateAndStatus(LocalDate dueDate, InstallmentStatus status);

    /**
     * بازیابی اقساط سررسید گذشته (معوق) که وضعیت آن‌ها "در انتظار پرداخت" است.
     * @param today تاریخ امروز.
     * @return لیستی از اقساط معوق.
     */
    @Query("SELECT i FROM Installment i WHERE i.dueDate < :today AND i.status = 'PENDING'")
    List<Installment> findOverdueInstallments(@Param("today") LocalDate today);

    /**
     * شمارش تعداد اقساط معوق.
     * @param today تاریخ امروز.
     * @return تعداد اقساط معوق.
     */
    @Query("SELECT COUNT(i) FROM Installment i WHERE i.dueDate < :today AND i.status = 'PENDING'")
    long countOverdueInstallments(@Param("today") LocalDate today);

    /**
     * بازیابی اقساطی که سررسید آن‌ها در هفته جاری است و در وضعیت "در انتظار پرداخت" قرار دارند.
     * @param startOfWeek تاریخ شروع هفته.
     * @param endOfWeek تاریخ پایان هفته.
     * @return لیستی از اقساط.
     */
    @Query("SELECT i FROM Installment i WHERE i.dueDate BETWEEN :startOfWeek AND :endOfWeek AND i.status = 'PENDING'")
    List<Installment> findInstallmentsDueThisWeek(@Param("startOfWeek") LocalDate startOfWeek, @Param("endOfWeek") LocalDate endOfWeek);

    /**
     * بازیابی اقساطی که سررسید آن‌ها در ماه جاری است و در وضعیت "در انتظار پرداخت" قرار دارند.
     * @param startOfMonth تاریخ شروع ماه.
     * @param endOfMonth تاریخ پایان ماه.
     * @return لیستی از اقساط.
     */
    @Query("SELECT i FROM Installment i WHERE i.dueDate BETWEEN :startOfMonth AND :endOfMonth AND i.status = 'PENDING'")
    List<Installment> findInstallmentsDueThisMonth(@Param("startOfMonth") LocalDate startOfMonth, @Param("endOfMonth") LocalDate endOfMonth);

    /**
     * محاسبه مجموع مبلغ پرداخت شده (paidAmount) برای تمام اقساطی که وضعیت آن‌ها "پرداخت شده" است.
     * **نکته:** وضعیت 'PAID' در کوئری استفاده شد.
     * @return مجموع مبلغ پرداخت شده.
     */
    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Installment i WHERE i.status = 'PAID'")
    Long sumPaidAmount();

    /**
     * محاسبه مجموع مبلغ باقیمانده (amount - paidAmount) اقساط معوق.
     * @param today تاریخ امروز.
     * @return مجموع مبلغ باقیمانده اقساط معوق.
     */
    @Query("SELECT COALESCE(SUM(i.amount - i.paidAmount), 0) FROM Installment i " +
            "WHERE i.dueDate < :today AND i.status = 'PENDING'")
    Long sumOverdueAmount(@Param("today") LocalDate today);

    /**
     * محاسبه مجموع جریمه‌های دریافتی (penaltyAmount).
     * @return مجموع جریمه‌های دریافتی.
     */
    @Query("SELECT COALESCE(SUM(i.penaltyAmount), 0) FROM Installment i WHERE i.penaltyAmount > 0")
    Long sumPenaltyAmount();

    /**
     * بازیابی اولین قسطی از یک قرارداد که هنوز "در انتظار پرداخت" است.
     * **نکته:** این متد از قرارداد نام‌گذاری شده Spring Data JPA برای دستیابی به عملکرد TOP 1 استفاده می‌کند.
     * @param contractId شناسه قرارداد.
     * @return یک Optional شامل اولین قسط پرداخت نشده.
     */
    Optional<Installment> findFirstByContractIdAndStatusOrderByInstallmentNumberAsc(Long contractId, InstallmentStatus status);


    /**
     * به‌روزرسانی وضعیت اقساط معوق (به وضعیت OVERDUE).
     * **نکته:** نیاز به استفاده از {@code @Modifying} برای کوئری‌های تغییردهنده (Update/Delete).
     * @param today تاریخ امروز.
     * @return تعداد ردیف‌های به‌روزرسانی شده.
     */
    @Modifying
    @Query("UPDATE Installment i SET i.status = 'OVERDUE' WHERE i.dueDate < :today AND i.status = 'PENDING'")
    int updateOverdueInstallments(@Param("today") LocalDate today);

    /**
     * خلاصه اقساط ماهانه (مبلغ کل) برای یک سال مشخص (برای نمودار).
     * **نکته:** توابع YEAR و MONTH وابسته به دیتابیس هستند.
     * @param year سال مورد نظر.
     * @return لیستی از آرایه‌ها شامل ماه و مجموع مبلغ قسط.
     */
    @Query("SELECT MONTH(i.dueDate) as month, SUM(i.amount) as total " +
            "FROM Installment i WHERE YEAR(i.dueDate) = :year " +
            "GROUP BY MONTH(i.dueDate) ORDER BY month")
    List<Object[]> getMonthlyInstallmentSummary(@Param("year") int year);

    /**
     * آمار پرداخت‌ها بر اساس روش پرداخت (تعداد و مجموع مبلغ پرداخت شده).
     * @return لیستی از آرایه‌ها شامل روش پرداخت، تعداد و مجموع مبلغ پرداخت شده.
     */
    @Query("SELECT i.paymentMethod, COUNT(i), SUM(i.paidAmount) " +
            "FROM Installment i WHERE i.status = 'PAID' AND i.paymentMethod IS NOT NULL " +
            "GROUP BY i.paymentMethod")
    List<Object[]> getPaymentMethodStatistics();
}