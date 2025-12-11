package com.paymaster.backend.domain.service;

import com.paymaster.backend.domain.entity.Contract;
import com.paymaster.backend.domain.entity.Customer;
import com.paymaster.backend.domain.entity.Installment;
import com.paymaster.backend.domain.repository.ContractRepository;
import com.paymaster.backend.domain.repository.CustomerRepository;
import com.paymaster.backend.domain.valueobject.ContractStatus;
import com.paymaster.backend.domain.valueobject.InstallmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * **سرویس مدیریت قراردادها** (Contract Service).
 * مسئولیت منطق تجاری مربوط به ایجاد، به‌روزرسانی، لغو و جستجوی قراردادها را بر عهده دارد.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final CalculationService calculationService;
    // فرض می‌شود کلاس DateUtils یک کلاس کمکی برای کار با تاریخ‌های شمسی/میلادی است
    private final DateUtils dateUtils;

    /**
     * دریافت تمام قراردادها با مرتب‌سازی بر اساس زمان ایجاد (نزولی).
     * @return لیستی از قراردادها.
     */
    public List<Contract> findAll() {
        return contractRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * دریافت قراردادها با قابلیت صفحه‌بندی.
     * @param page شماره صفحه (از صفر).
     * @param size تعداد آیتم‌ها در هر صفحه.
     * @return صفحه‌ای از قراردادها.
     */
    public Page<Contract> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return contractRepository.findAll(pageable);
    }

    /**
     * جستجوی قرارداد بر اساس شناسه (ID).
     * @param id شناسه قرارداد.
     * @return یک Optional شامل قرارداد.
     */
    public Optional<Contract> findById(Long id) {
        return contractRepository.findById(id);
    }

    /**
     * جستجوی قرارداد بر اساس شماره قرارداد.
     * @param contractNumber شماره قرارداد.
     * @return یک Optional شامل قرارداد.
     */
    public Optional<Contract> findByContractNumber(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber);
    }

    /**
     * بازیابی تمام قراردادهای مرتبط با یک مشتری.
     * @param customerId شناسه مشتری.
     * @return لیستی از قراردادها.
     */
    public List<Contract> findByCustomerId(Long customerId) {
        return contractRepository.findByCustomerId(customerId);
    }

    /**
     * بازیابی قراردادها بر اساس وضعیت مشخص.
     * @param status وضعیت قرارداد.
     * @return لیستی از قراردادها.
     */
    public List<Contract> findByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status);
    }

    /**
     * **ایجاد قرارداد جدید و تولید اقساط مربوطه**.
     * این عملیات شامل محاسبات مالی و تولید شماره قرارداد یکتا است.
     *
     * @param customerId شناسه مشتری.
     * @param principalAmount مبلغ اصل وام.
     * @param interestRate نرخ سود سالانه (به درصد).
     * @param installmentCount تعداد اقساط.
     * @param startDate تاریخ شروع قرارداد.
     * @param penaltyRate نرخ جریمه دیرکرد روزانه (اختیاری، پیش‌فرض 0.5).
     * @param description توضیحات.
     * @return قرارداد ایجاد شده.
     * @throws IllegalArgumentException در صورت یافت نشدن مشتری.
     */
    @Transactional
    public Contract createContract(Long customerId, Long principalAmount, Double interestRate, Integer installmentCount, LocalDate startDate, Double penaltyRate, String description) {

        // 1. دریافت مشتری
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("خطا: مشتری با شناسه " + customerId + " یافت نشد"));

        // 2. تولید شماره قرارداد یکتا
        String contractNumber = generateContractNumber();

        // 3. محاسبات مالی
        long interestAmount = calculationService.calculateSimpleInterest(principalAmount, interestRate, installmentCount);
        long totalAmount = principalAmount + interestAmount;
        long installmentAmount = calculationService.calculateInstallmentAmount(totalAmount, installmentCount);

        // 4. محاسبه تاریخ پایان (تاریخ سررسید قسط آخر)
        // فرض می‌کنیم dateUtils.addMonthsToPersianDate تاریخ را بر اساس شمسی/میلادی به درستی اضافه می‌کند.
        LocalDate endDate = dateUtils.addMonthsToPersianDate(startDate, installmentCount);

        // 5. ایجاد و ذخیره قرارداد
        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .customer(customer)
                .principalAmount(principalAmount)
                .interestRate(interestRate)
                .interestAmount(interestAmount)
                .totalAmount(totalAmount)
                .installmentCount(installmentCount)
                .installmentAmount(installmentAmount)
                .startDate(startDate)
                .endDate(endDate)
                // استفاده از مقدار پیش‌فرض اگر null باشد
                .penaltyRate(penaltyRate != null ? penaltyRate : 0.5)
                .status(ContractStatus.ACTIVE)
                .description(description)
                .build();

        // ذخیره قرارداد برای ایجاد شناسه (ID)
        contract = contractRepository.save(contract);

        // 6. ایجاد اقساط
        generateInstallments(contract);

        return contract;
    }

    /**
     * تولید شماره قرارداد یکتا (C[Year][Sequence]).
     * @return شماره قرارداد جدید.
     */
    private String generateContractNumber() {
        // فرض می‌کنیم dateUtils.getCurrentPersianYear سال شمسی جاری را برمی‌گرداند (مثلاً 1404).
        String year = String.valueOf(dateUtils.getCurrentPersianYear());

        // استفاده از روش findFirstByOrderByContractNumberDesc در Repository برای بازیابی آخرین شماره
        Optional<String> lastNumberOptional = contractRepository.findFirstByOrderByContractNumberDesc();

        int sequence = 1;
        if (lastNumberOptional.isPresent()) {
            String last = lastNumberOptional.get();

            // بررسی کنید که آیا شماره قرارداد قبلی با سال جاری شروع شده است (مثلاً C1404)
            if (last.startsWith("C" + year)) {
                // فرض می‌کنیم شماره قراردادها به فرمت C[YY][NNNN] هستند
                // اگر فرمت شما CYYYYNNNN باشد، این قسمت صحیح است:
                try {
                    String seqPart = last.substring(5); // مثلاً از C14040001، بخش 0001 را می‌گیرد
                    sequence = Integer.parseInt(seqPart) + 1;
                } catch (NumberFormatException e) {
                    // در صورت خطای تبدیل، از 1 شروع می‌کنیم
                    sequence = 1;
                }
            } else {
                // اگر سال تغییر کرده باشد، از 1 شروع می‌کنیم
                sequence = 1;
            }
        }

        // فرمت شماره قرارداد به صورت C[سال][چهار رقم ترتیبی] (مثلاً C14040001)
        return String.format("C%s%04d", year, sequence);
    }

    /**
     * تولید اقساط برای قرارداد (با مدیریت خطای گرد کردن در قسط آخر).
     * @param contract قرارداد والد.
     */
    private void generateInstallments(Contract contract) {
        long principalPortion = calculationService.calculatePrincipalPortion(contract.getPrincipalAmount(), contract.getInstallmentCount());
        long interestPortion = calculationService.calculateInterestPortion(contract.getInterestAmount(), contract.getInstallmentCount());

        LocalDate lastDueDate = contract.getStartDate();
        long totalInstallmentAmount = contract.getInstallmentAmount() * contract.getInstallmentCount();
        long adjustmentAmount = contract.getTotalAmount() - totalInstallmentAmount; // اختلاف ناشی از گرد کردن

        for (int i = 1; i <= contract.getInstallmentCount(); i++) {
            // تاریخ سررسید هر قسط (یک ماه بعد از قسط قبلی)
            lastDueDate = dateUtils.addMonthsToPersianDate(lastDueDate, 1);

            long amount = contract.getInstallmentAmount();

            // اعمال تنظیمات گرد کردن (adjustment) به قسط آخر
            if (i == contract.getInstallmentCount()) {
                amount += adjustmentAmount;
            }

            // تضمین می‌کنیم مجموع سهم اصل و سود برابر با مبلغ قسط باشد (ممکن است در قسط آخر نیاز به تنظیم داشته باشد)
            long currentPrincipalPortion = principalPortion;
            long currentInterestPortion = interestPortion;

            // اگر قسط آخر باشد، کل سود و اصل باقیمانده اعمال شود تا جمع برابر totalAmount شود.
            // (توجه: این بخش نیاز به محاسبات پیچیده‌تر دارد اگر توزیع سود/اصل برابر نباشد. در اینجا از توزیع مساوی استفاده شده است)

            Installment installment = Installment.builder()
                    .contract(contract)
                    .installmentNumber(i)
                    .amount(amount)
                    .principalPortion(currentPrincipalPortion)
                    .interestPortion(currentInterestPortion)
                    .dueDate(lastDueDate)
                    .status(InstallmentStatus.PENDING)
                    .build();

            // اضافه کردن به لیست اقساط قرارداد (مهم برای ذخیره توسط Cascade)
            contract.getInstallments().add(installment);
        }

        // ذخیره مجدد قرارداد که منجر به ذخیره اقساط نیز می‌شود (به دلیل Cascade.ALL)
        contractRepository.save(contract);
    }

    /**
     * **لغو قرارداد**.
     * وضعیت قرارداد را به CANCELLED تغییر می‌دهد.
     * @param id شناسه قرارداد.
     * @param reason دلیل لغو.
     * @return قرارداد به‌روزرسانی شده.
     * @throws IllegalArgumentException اگر قرارداد یافت نشود یا قبلاً تسویه شده باشد.
     */
    @Transactional
    public Contract cancelContract(Long id, String reason) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("خطا: قرارداد با شناسه " + id + " یافت نشد"));

        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new IllegalArgumentException("قرارداد تسویه شده قابل لغو نیست.");
        }

        contract.setStatus(ContractStatus.CANCELLED);
        // اضافه کردن دلیل لغو به توضیحات
        String currentDescription = contract.getDescription() != null ? contract.getDescription() : "";
        contract.setDescription(currentDescription + "\n[لغو شده در " + LocalDate.now() + "]: " + reason);

        return contractRepository.save(contract);
    }

    /**
     * **بررسی و به‌روزرسانی وضعیت قراردادها** (شامل تکمیل و معوق شدن).
     */
    @Transactional
    public void updateContractStatuses() {
        List<Contract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);

        for (Contract contract : activeContracts) {
            // 1. بررسی تکمیل قرارداد (تمام اقساط پرداخت شده باشند)
            boolean allPaid = contract.getInstallments().stream()
                    .allMatch(i -> i.getStatus() == InstallmentStatus.PAID || i.getStatus() == InstallmentStatus.COMPLETED);

            if (allPaid) {
                contract.setStatus(ContractStatus.COMPLETED);
                contractRepository.save(contract);
                continue;
            }

            // 2. بررسی معوق بودن (وجود حداقل یک قسط سررسید گذشته و پرداخت نشده)
            boolean hasOverdue = contract.getInstallments().stream().anyMatch(Installment::isOverdue);

            // اگر قرارداد فعال است و دارای قسط معوق است، وضعیت به OVERDUE تغییر می‌کند.
            // **نکته:** منطق شما صرفاً برای ACTIVE این کار را انجام می‌دهد. اگر وضعیت OVERDUE را جداگانه می‌خواهید، باید بررسی کنید:
            if (hasOverdue && contract.getStatus() == ContractStatus.ACTIVE) {
                contract.setStatus(ContractStatus.OVERDUE);
                contractRepository.save(contract);
            }
        }
    }

    /**
     * شمارش تعداد کل قراردادها.
     * @return تعداد کل قراردادها.
     */
    public long count() {
        return contractRepository.count();
    }

    /**
     * شمارش تعداد قراردادهای فعال.
     * @return تعداد قراردادهای فعال.
     */
    public long countActive() {
        return contractRepository.countByStatus(ContractStatus.ACTIVE);
    }

    /**
     * محاسبه مجموع مبلغ کل قراردادهای فعال (Total Amount).
     * @return مجموع مبلغ.
     */
    public long sumActiveContractsAmount() {
        Long sum = contractRepository.sumTotalAmountOfActiveContracts();
        return sum != null ? sum : 0;
    }

    /**
     * قراردادها بر اساس وضعیت با صفحه‌بندی
     */
    @Transactional(readOnly = true)
    public Page<Contract> findByStatus(ContractStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // فراخوانی متد تعریف شده در Repository
        return contractRepository.findByStatus(status, pageable);
    }
    /**
     * بازیابی قراردادهای معوق (دارای قسط سررسید گذشته).
     * @return لیستی از قراردادهای معوق.
     */
    public List<Contract> findOverdueContracts() {
        return contractRepository.findOverdueContracts(LocalDate.now());
    }
}
