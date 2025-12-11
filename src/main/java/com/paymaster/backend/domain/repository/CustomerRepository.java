package com.paymaster.backend.domain.repository;

import com.paymaster.backend.domain.entity.Customer;
import com.paymaster.backend.domain.valueobject.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * **ریپازیتوری مشتریان** (Customer Repository).
 * امکان دسترسی و مدیریت داده‌های مشتری در دیتابیس را فراهم می‌کند.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * جستجوی مشتری بر اساس کد ملی.
     * @param nationalCode کد ملی مشتری.
     * @return یک Optional شامل مشتری پیدا شده (در صورت وجود).
     */
    Optional<Customer> findByNationalCode(String nationalCode);

    /**
     * جستجوی مشتری بر اساس شماره موبایل.
     * @param mobile شماره موبایل مشتری.
     * @return یک Optional شامل مشتری پیدا شده (در صورت وجود).
     */
    Optional<Customer> findByMobile(String mobile);

    /**
     * بررسی وجود مشتری با کد ملی مشخص.
     * @param nationalCode کد ملی.
     * @return true اگر وجود داشته باشد.
     */
    boolean existsByNationalCode(String nationalCode);

    /**
     * بررسی وجود مشتری با شماره موبایل مشخص.
     * @param mobile شماره موبایل.
     * @return true اگر وجود داشته باشد.
     */
    boolean existsByMobile(String mobile);

    /**
     * بازیابی لیستی از مشتریان بر اساس وضعیت (Status).
     * @param status وضعیت مشتری.
     * @return لیستی از مشتریان.
     */
    List<Customer> findByStatus(CustomerStatus status);

    /**
     * جستجو بر اساس بخشی از نام کامل مشتری (بدون حساسیت به حروف کوچک/بزرگ).
     * @param name بخشی از نام مشتری.
     * @return لیستی از مشتریان منطبق.
     */
    List<Customer> findByFullNameContainingIgnoreCase(String name);

    /**
     * جستجوی پیشرفته مشتریان بر اساس فیلترهای مختلف با قابلیت صفحه‌بندی.
     * **نکته:** استفاده از پارامترهای اختیاری (NULL) در JPQL برای جستجوی دینامیک.
     * @param name بخشی از نام (جستجوی LIKE).
     * @param nationalCode کد ملی (جستجوی Exact Match).
     * @param mobile شماره موبایل (جستجوی Exact Match).
     * @param status وضعیت مشتری.
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از مشتریان مطابق با فیلترها.
     */
    @Query("SELECT c FROM Customer c WHERE " +
            "(:name IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:nationalCode IS NULL OR c.nationalCode = :nationalCode) AND " +
            "(:mobile IS NULL OR c.mobile = :mobile) AND " +
            "(:status IS NULL OR c.status = :status)")
    Page<Customer> searchCustomers(
            @Param("name") String name,
            @Param("nationalCode") String nationalCode,
            @Param("mobile") String mobile,
            @Param("status") CustomerStatus status,
            Pageable pageable);

    /**
     * شمارش تعداد مشتریان بر اساس وضعیت.
     * @param status وضعیت مشتری.
     * @return تعداد مشتریان با وضعیت مشخص.
     */
    long countByStatus(CustomerStatus status);

    /**
     * بازیابی لیستی از مشتریانی که حداقل یک قرارداد فعال دارند.
     * @return لیستی از مشتریان دارای قرارداد فعال.
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.contracts con WHERE con.status = 'ACTIVE'")
    List<Customer> findCustomersWithActiveContracts();

    /**
     * بازیابی لیستی از مشتریانی که هیچ قراردادی ندارند.
     * @return لیستی از مشتریان بدون قرارداد.
     */
    @Query("SELECT c FROM Customer c WHERE c.contracts IS EMPTY")
    List<Customer> findCustomersWithoutContracts();

    /**
     * جستجوی عمومی مشتریان بر اساس کلمه کلیدی در نام، کد ملی یا شماره موبایل.
     * @param keyword کلمه کلیدی جستجو.
     * @param pageable اطلاعات صفحه‌بندی.
     * @return صفحه‌ای از مشتریان منطبق.
     */
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.nationalCode LIKE CONCAT('%', :keyword, '%') OR " +
            "c.mobile LIKE CONCAT('%', :keyword, '%')")
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}