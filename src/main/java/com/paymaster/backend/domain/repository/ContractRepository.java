package com.paymaster.backend.domain.repository;

import com.paymaster.backend.domain.entity.Contract;
import com.paymaster.backend.domain.valueobject.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * **ریپازیتوری قراردادها** (Contract Repository).
 * امکان دسترسی و مدیریت داده‌های قرارداد در دیتابیس را فراهم می‌کند.
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * جستجوی قرارداد بر اساس شماره قرارداد.
     * @param contractNumber شماره قرارداد.
     * @return یک Optional شامل قرارداد پیدا شده (در صورت وجود).
     */
    Optional<Contract> findByContractNumber(String contractNumber);

    /**
     * بررسی وجود یک قرارداد با شماره مشخص.
     * @param contractNumber شماره قرارداد.
     * @return true اگر وجود داشته باشد.
     */
    boolean existsByContractNumber(String contractNumber);

    /**
     * بازیابی تمام قراردادهای مرتبط با یک مشتری.
     * @param customerId شناسه مشتری.
     * @return لیستی از قراردادها.
     */
    List<Contract> findByCustomerId(Long customerId);

    /**
     * بازیابی قراردادهای مرتبط با یک مشتری با قابلیت صفحه‌بندی.
     * @param customerId شناسه مشتری.
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از قراردادها.
     */
    Page<Contract> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * بازیابی تمام قراردادها بر اساس وضعیت.
     * @param status وضعیت قرارداد.
     * @return لیستی از قراردادها.
     */
    List<Contract> findByStatus(ContractStatus status);

    /**
     * بازیابی قراردادها بر اساس وضعیت با قابلیت صفحه‌بندی.
     * @param status وضعیت قرارداد.
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از قراردادها.
     */
    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);

    /**
     * شمارش تعداد قراردادها بر اساس وضعیت.
     * @param status وضعیت قرارداد.
     * @return تعداد قراردادها.
     */
    long countByStatus(ContractStatus status);

    /**
     * بازیابی قراردادهایی که تاریخ شروع آن‌ها بین دو تاریخ مشخص باشد.
     * @param startDate تاریخ شروع بازه.
     * @param endDate تاریخ پایان بازه.
     * @return لیستی از قراردادها.
     */
    List<Contract> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * محاسبه مجموع مبلغ کل (Total Amount) قراردادهای فعال.
     * @return مجموع مبالغ.
     */
    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM Contract c WHERE c.status = 'ACTIVE'")
    Long sumTotalAmountOfActiveContracts();

    /**
     * محاسبه مجموع اصل سرمایه (Principal Amount) قراردادهای فعال.
     * @return مجموع مبالغ.
     */
    @Query("SELECT COALESCE(SUM(c.principalAmount), 0) FROM Contract c WHERE c.status = 'ACTIVE'")
    Long sumPrincipalAmountOfActiveContracts();

    /**
     * محاسبه مجموع سود (Interest Amount) قراردادهای فعال.
     * @return مجموع مبالغ.
     */
    @Query("SELECT COALESCE(SUM(c.interestAmount), 0) FROM Contract c WHERE c.status = 'ACTIVE'")
    Long sumInterestAmountOfActiveContracts();

    /**
     * بازیابی قراردادهایی که تاریخ پایان (End Date) آن‌ها امروز است و فعال هستند.
     * **نکته:** برای مقایسه وضعیت Enum در JPQL باید نام آن (String) استفاده شود.
     * @param today تاریخ امروز.
     * @return لیستی از قراردادهای سررسید شده امروز.
     */
    @Query("SELECT c FROM Contract c WHERE c.endDate = :today AND c.status = 'ACTIVE'")
    List<Contract> findContractsDueToday(@Param("today") LocalDate today);

    /**
     * جستجوی پیشرفته قراردادها بر اساس فیلترهای مختلف.
     * **نکته:** استفاده از پارامترهای اختیاری (NULL) در JPQL برای جستجوی دینامیک.
     * @param contractNumber شماره قرارداد (جستجوی LIKE).
     * @param customerId شناسه مشتری.
     * @param status وضعیت قرارداد.
     * @param startDate تاریخ شروع (بزرگتر مساوی).
     * @param endDate تاریخ پایان (کوچکتر مساوی).
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از قراردادها مطابق با فیلترها.
     */
    @Query("SELECT c FROM Contract c WHERE " +
            "(:contractNumber IS NULL OR c.contractNumber LIKE CONCAT('%', :contractNumber, '%')) AND " +
            "(:customerId IS NULL OR c.customer.id = :customerId) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:startDate IS NULL OR c.startDate >= :startDate) AND " +
            "(:endDate IS NULL OR c.startDate <= :endDate)")
    Page<Contract> searchContracts(
            @Param("contractNumber") String contractNumber,
            @Param("customerId") Long customerId,
            @Param("status") ContractStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * بازیابی آخرین شماره قرارداد ثبت شده.
     * **نکته:** استفاده از متد نام‌گذاری شده Spring Data JPA برای دستیابی به عملکرد LIMIT 1/TOP 1.
     * @return آخرین شماره قرارداد (در صورت وجود).
     */
    Optional<String> findFirstByOrderByContractNumberDesc();


    /**
     * بازیابی قراردادهای معوق (Overdue) - قراردادهایی که فعال هستند و حداقل یک قسط معوق (سررسید گذشته و در انتظار پرداخت) دارند.
     * @param today تاریخ امروز.
     * @return لیستی از قراردادهای معوق.
     */
    @Query("SELECT DISTINCT c FROM Contract c JOIN c.installments i " +
            "WHERE c.status = 'ACTIVE' AND i.status = 'PENDING' AND i.dueDate < :today")
    List<Contract> findOverdueContracts(@Param("today") LocalDate today);
}