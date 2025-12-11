package com.paymaster.backend.domain.service;

import com.paymaster.backend.domain.entity.Customer;
import com.paymaster.backend.domain.repository.CustomerRepository;
import com.paymaster.backend.domain.valueobject.CustomerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * **سرویس مدیریت مشتریان** (Customer Service).
 * مسئولیت منطق تجاری مربوط به مدیریت اطلاعات، جستجو و اعتبارسنجی مشتریان را بر عهده دارد.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * دریافت تمام مشتریان با مرتب‌سازی بر اساس زمان ایجاد (نزولی).
     * @return لیستی از مشتریان.
     */
    public List<Customer> findAll() {
        return customerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * دریافت مشتریان با قابلیت صفحه‌بندی.
     * @param page شماره صفحه (از صفر).
     * @param size تعداد آیتم‌ها در هر صفحه.
     * @return صفحه‌ای از مشتریان.
     */
    public Page<Customer> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return customerRepository.findAll(pageable);
    }

    /**
     * جستجوی مشتری بر اساس شناسه (ID).
     * @param id شناسه مشتری.
     * @return یک Optional شامل مشتری.
     */
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    /**
     * جستجوی مشتری بر اساس کد ملی.
     * @param nationalCode کد ملی مشتری.
     * @return یک Optional شامل مشتری.
     */
    public Optional<Customer> findByNationalCode(String nationalCode) {
        return customerRepository.findByNationalCode(nationalCode);
    }

    /**
     * جستجوی مشتری بر اساس شماره موبایل.
     * @param mobile شماره موبایل مشتری.
     * @return یک Optional شامل مشتری.
     */
    public Optional<Customer> findByMobile(String mobile) {
        return customerRepository.findByMobile(mobile);
    }

    /**
     * **جستجوی عمومی** مشتریان بر اساس کلمه کلیدی (در نام، کد ملی یا موبایل).
     * اگر کلمه کلیدی خالی باشد، تمام مشتریان با صفحه‌بندی برگردانده می‌شوند.
     * @param keyword کلمه کلیدی جستجو.
     * @param page شماره صفحه.
     * @param size تعداد در صفحه.
     * @return صفحه‌ای از مشتریان منطبق.
     */
    public Page<Customer> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (keyword == null || keyword.trim().isEmpty()) {
            return customerRepository.findAll(pageable);
        }
        return customerRepository.searchByKeyword(keyword.trim(), pageable);
    }

    /**
     * **جستجوی پیشرفته** مشتریان بر اساس فیلترهای مشخص.
     * @param name نام مشتری (بخشی از نام).
     * @param nationalCode کد ملی.
     * @param mobile شماره موبایل.
     * @param status وضعیت مشتری.
     * @param page شماره صفحه.
     * @param size تعداد در صفحه.
     * @return صفحه‌ای از مشتریان منطبق.
     */
    public Page<Customer> advancedSearch(String name, String nationalCode, String mobile,
                                          CustomerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return customerRepository.searchCustomers(name, nationalCode, mobile, status, pageable);
    }

    /**
     * بازیابی لیستی از مشتریان بر اساس وضعیت مشخص.
     * @param status وضعیت مشتری.
     * @return لیستی از مشتریان.
     */
    public List<Customer> findByStatus(CustomerStatus status) {
        return customerRepository.findByStatus(status);
    }

    /**
     * **ذخیره مشتری جدید**.
     * شامل اعتبارسنجی تکراری بودن کد ملی و موبایل قبل از ذخیره.
     * @param customer موجودیت مشتری.
     * @return مشتری ذخیره شده.
     * @throws IllegalArgumentException در صورت تکراری بودن کد ملی یا موبایل.
     */
    @Transactional
    public Customer save(Customer customer) {
        // اعتبارسنجی کد ملی تکراری (فقط برای ایجاد یا زمانی که ID null است)
        if (customer.getId() == null || customerRepository.findByNationalCode(customer.getNationalCode()).map(c -> !c.getId().equals(customer.getId())).orElse(false)) {
            if (customerRepository.existsByNationalCode(customer.getNationalCode())) {
                throw new IllegalArgumentException("خطا: کد ملی قبلاً ثبت شده است.");
            }
        }

        // اعتبارسنجی موبایل تکراری
        if (customer.getId() == null || customerRepository.findByMobile(customer.getMobile()).map(c -> !c.getId().equals(customer.getId())).orElse(false)) {
            if (customerRepository.existsByMobile(customer.getMobile())) {
                throw new IllegalArgumentException("خطا: شماره موبایل قبلاً ثبت شده است.");
            }
        }

        return customerRepository.save(customer);
    }

    /**
     * **بروزرسانی مشتری**.
     * اعتبارسنجی تکراری بودن کد ملی و موبایل را در نظر می‌گیرد.
     * @param id شناسه مشتری.
     * @param updatedCustomer اطلاعات به‌روز شده مشتری.
     * @return مشتری به‌روزرسانی شده.
     * @throws IllegalArgumentException در صورت یافت نشدن مشتری یا تکراری بودن اطلاعات.
     */
    @Transactional
    public Customer update(Long id, Customer updatedCustomer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("خطا: مشتری با شناسه " + id + " یافت نشد."));

        // بررسی کد ملی تکراری (اگر تغییر کرده باشد)
        if (! existing.getNationalCode().equals(updatedCustomer.getNationalCode())) {
            if (customerRepository.findByNationalCode(updatedCustomer.getNationalCode()).isPresent()) {
                throw new IllegalArgumentException("خطا: کد ملی قبلاً برای مشتری دیگری ثبت شده است.");
            }
        }

        // بررسی موبایل تکراری (اگر تغییر کرده باشد)
        if (!existing.getMobile().equals(updatedCustomer.getMobile())) {
            if (customerRepository.findByMobile(updatedCustomer.getMobile()).isPresent()) {
                throw new IllegalArgumentException("خطا: شماره موبایل قبلاً برای مشتری دیگری ثبت شده است.");
            }
        }

        // اعمال تغییرات
        existing.setFullName(updatedCustomer.getFullName());
        existing.setNationalCode(updatedCustomer.getNationalCode());
        existing.setMobile(updatedCustomer.getMobile());
        existing.setPhone(updatedCustomer.getPhone());
        existing.setEmail(updatedCustomer.getEmail());
        existing.setAddress(updatedCustomer.getAddress());
        existing.setPostalCode(updatedCustomer.getPostalCode());
        existing.setStatus(updatedCustomer.getStatus());
        existing.setNotes(updatedCustomer.getNotes());

        return customerRepository.save(existing);
    }

    /**
     * **حذف مشتری**.
     * مشتری تنها در صورتی حذف می‌شود که هیچ قرارداد مرتبطی نداشته باشد.
     * @param id شناسه مشتری.
     * @throws IllegalArgumentException در صورت یافت نشدن مشتری یا داشتن قرارداد.
     */
    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("خطا: مشتری با شناسه " + id + " یافت نشد."));

        // بررسی وجود قراردادها
        if (!customer.getContracts().isEmpty()) {
            throw new IllegalArgumentException("خطا: امکان حذف مشتری دارای قرارداد وجود ندارد. ابتدا قراردادها را حذف کنید.");
        }

        customerRepository.delete(customer);
    }

    /**
     * **تغییر وضعیت مشتری**.
     * @param id شناسه مشتری.
     * @param newStatus وضعیت جدید.
     * @return مشتری به‌روزرسانی شده.
     * @throws IllegalArgumentException در صورت یافت نشدن مشتری.
     */
    @Transactional
    public Customer changeStatus(Long id, CustomerStatus newStatus) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("خطا: مشتری با شناسه " + id + " یافت نشد."));
        customer.setStatus(newStatus);
        return customerRepository.save(customer);
    }

    /**
     * شمارش تعداد کل مشتریان.
     * @return تعداد کل مشتریان.
     */
    public long count() {
        return customerRepository.count();
    }

    /**
     * شمارش تعداد مشتریان فعال.
     * @return تعداد مشتریان فعال.
     */
    public long countActive() {
        return customerRepository.countByStatus(CustomerStatus.ACTIVE);
    }

    /**
     * بازیابی مشتریانی که حداقل یک قرارداد فعال دارند.
     * @return لیستی از مشتریان با قرارداد فعال.
     */
    public List<Customer> findCustomersWithActiveContracts() {
        return customerRepository.findCustomersWithActiveContracts();
    }

    /**
     * **اعتبارسنجی کد ملی ایرانی** (Iran National Code Validation).
     * الگوریتم ریاضی استاندارد برای بررسی صحت کد ملی.
     * @param nationalCode رشته 10 رقمی کد ملی.
     * @return true اگر کد ملی معتبر باشد.
     */
    public boolean isValidNationalCode(String nationalCode) {
        if (nationalCode == null || !nationalCode.matches("^\\d{10}$")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(nationalCode.charAt(i)) * (10 - i);
        }

        int remainder = sum % 11;
        int checkDigit = Character.getNumericValue(nationalCode.charAt(9));

        return (remainder < 2 && checkDigit == remainder) ||
               (remainder >= 2 && checkDigit == 11 - remainder);
    }
}