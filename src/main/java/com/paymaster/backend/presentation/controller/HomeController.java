package com.paymaster.backend.presentation.controller;
import com.paymaster.backend.domain.service.CustomerService;
import com.paymaster.backend.domain.entity.Contract;
import com.paymaster.backend.domain.entity.Customer;
import com.paymaster.backend.domain.entity.Installment;
import com.paymaster.backend.domain.service.*;
import com.paymaster.backend.domain.valueobject.ContractStatus;
import com.paymaster.backend.domain.valueobject.CustomerStatus;
import com.paymaster.backend.domain.valueobject.PaymentMethod;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * **کنترلر اصلی برنامه** (Home Controller).
 * مسئولیت مدیریت تمام صفحات، مسیریابی (Routing) و درخواست‌های اصلی برنامه را بر عهده دارد (با فرض استفاده از Thymeleaf یا JSP).
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CustomerService customerService;
    private final ContractService contractService;
    private final InstallmentService installmentService;
    private final DashboardService dashboardService;
    private final DateUtils dateUtils;
    private final CalculationService calculationService;

    // ==================== داشبورد (Dashboard) ====================

    /**
     * **صفحه اصلی - داشبورد**.
     * نمایش آمار کلی، اقساط معوق و اقساط سررسید نزدیک.
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardService.DashboardStats stats = dashboardService.getDashboardStats();
        List<Installment> upcomingInstallments = dashboardService.getUpcomingInstallments();
        List<Installment> overdueInstallments = dashboardService.getOverdueInstallments();
        List<Contract> overdueContracts = dashboardService.getOverdueContracts();

        model.addAttribute("stats", stats);
        model.addAttribute("upcomingInstallments", upcomingInstallments);
        model.addAttribute("overdueInstallments", overdueInstallments);
        model.addAttribute("overdueContracts", overdueContracts);
        model.addAttribute("dateUtils", dateUtils);

        return "dashboard";
    }

    // ==================== مدیریت مشتریان (Customers) ====================

    /**
     * **لیست مشتریان**.
     * جستجوی عمومی و نمایش با صفحه‌بندی.
     */
    @GetMapping("/customers")
    public String listCustomers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Customer> customersPage = customerService.search(keyword, page, size);

        model.addAttribute("customers", customersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customersPage.getTotalPages());
        model.addAttribute("totalItems", customersPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("dateUtils", dateUtils);

        return "customers/list";
    }

    /**
     * **فرم ایجاد مشتری جدید**.
     */
    @GetMapping("/customers/new")
    public String newCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("statuses", CustomerStatus.values());
        model.addAttribute("isEdit", false);
        return "customers/form";
    }

    /**
     * **ذخیره مشتری جدید یا به‌روزرسانی مشتری موجود**.
     */
    @PostMapping("/customers/save")
    public String saveCustomer(
            @Valid @ModelAttribute Customer customer,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("statuses", CustomerStatus.values());
            model.addAttribute("isEdit", customer.getId() != null);
            return "customers/form"; // بازگشت به فرم با خطاها
        }

        try {
            if (customer.getId() == null) {
                customerService.save(customer);
                redirectAttributes.addFlashAttribute("successMessage", "مشتری با موفقیت ثبت شد.");
            } else {
                customerService.update(customer.getId(), customer);
                redirectAttributes.addFlashAttribute("successMessage", "اطلاعات مشتری به‌روزرسانی شد.");
            }
        } catch (IllegalArgumentException e) {
            // در صورت خطاهای منطقی (مانند تکراری بودن کد ملی)
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statuses", CustomerStatus.values());
            model.addAttribute("isEdit", customer.getId() != null);
            return "customers/form";
        }

        return "redirect:/customers";
    }

    /**
     * **فرم ویرایش مشتری**.
     */
    @GetMapping("/customers/edit/{id}")
    public String editCustomerForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id)
                .map(customer -> {
                    model.addAttribute("customer", customer);
                    model.addAttribute("statuses", CustomerStatus.values());
                    model.addAttribute("isEdit", true);
                    return "customers/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "خطا: مشتری یافت نشد.");
                    return "redirect:/customers";
                });
    }

    /**
     * **مشاهده جزئیات مشتری**.
     * نمایش اطلاعات مشتری و لیست قراردادهای مرتبط.
     */
    @GetMapping("/customers/view/{id}")
    public String viewCustomer(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id)
                .map(customer -> {
                    List<Contract> contracts = contractService.findByCustomerId(id);
                    model.addAttribute("customer", customer);
                    model.addAttribute("contracts", contracts);
                    model.addAttribute("dateUtils", dateUtils);
                    model.addAttribute("calculationService", calculationService);
                    return "customers/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "خطا: مشتری یافت نشد.");
                    return "redirect:/customers";
                });
    }

    /**
     * **حذف مشتری**.
     */
    @PostMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "مشتری با موفقیت حذف شد.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customers";
    }

    // ==================== مدیریت قراردادها (Contracts) ====================

    /**
     * **لیست قراردادها**.
     * فیلتر بر اساس وضعیت و صفحه‌بندی.
     */
    @GetMapping("/contracts")
    public String listContracts(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Contract> contractsPage;
        if (status != null) {
            // فیلتر بر اساس وضعیت
            contractsPage = contractService.findByStatus(status, page, size);
        } else {
            // نمایش همه
            contractsPage = contractService.findAll(page, size);
        }

        model.addAttribute("contracts", contractsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contractsPage.getTotalPages());
        model.addAttribute("totalItems", contractsPage.getTotalElements());
        model.addAttribute("statuses", ContractStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("dateUtils", dateUtils);
        model.addAttribute("calculationService", calculationService);

        return "contracts/list";
    }

    /**
     * **فرم ایجاد قرارداد جدید**.
     * انتخاب مشتری از لیست مشتریان فعال.
     */
    @GetMapping("/contracts/new")
    public String newContractForm(
            @RequestParam(required = false) Long customerId,
            Model model) {

        List<Customer> customers = customerService.findByStatus(CustomerStatus.ACTIVE);

        model.addAttribute("customers", customers);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("todayPersian", dateUtils.getTodayPersian());
        model.addAttribute("todayGregorian", LocalDate.now());

        return "contracts/form";
    }

    /**
     * **ذخیره قرارداد جدید** و تولید اقساط مربوطه.
     */
    @PostMapping("/contracts/save")
    public String saveContract(
            @RequestParam Long customerId,
            @RequestParam Long principalAmount,
            @RequestParam Double interestRate,
            @RequestParam Integer installmentCount,
            @RequestParam String startDatePersian,
            @RequestParam(defaultValue = "0.5") Double penaltyRate,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. تبدیل تاریخ شمسی ورودی به میلادی
            LocalDate startDate = dateUtils.toGregorianDate(startDatePersian);
            if (startDate == null) {
                throw new IllegalArgumentException("خطا: تاریخ شروع قرارداد نامعتبر است.");
            }

            // 2. ایجاد قرارداد
            Contract contract = contractService.createContract(
                    customerId, principalAmount, interestRate,
                    installmentCount, startDate, penaltyRate, description);

            redirectAttributes.addFlashAttribute("successMessage",
                    "قرارداد " + contract.getContractNumber() + " با موفقیت ایجاد شد.");

            return "redirect:/contracts/view/" + contract.getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // بازگشت به فرم جدید در صورت خطا
            return "redirect:/contracts/new?customerId=" + customerId;
        }
    }

    /**
     * **مشاهده جزئیات قرارداد** و لیست اقساط.
     */
    @GetMapping("/contracts/view/{id}")
    public String viewContract(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return contractService.findById(id)
                .map(contract -> {
                    List<Installment> installments = installmentService.findByContractId(id);
                    model.addAttribute("contract", contract);
                    model.addAttribute("installments", installments);
                    model.addAttribute("dateUtils", dateUtils);
                    model.addAttribute("calculationService", calculationService);
                    model.addAttribute("paymentMethods", PaymentMethod.values());
                    return "contracts/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "خطا: قرارداد یافت نشد.");
                    return "redirect:/contracts";
                });
    }

    /**
     * **لغو قرارداد**.
     */
    @PostMapping("/contracts/cancel/{id}")
    public String cancelContract(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        try {
            Contract contract = contractService.cancelContract(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "قرارداد " + contract.getContractNumber() + " لغو شد.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/contracts/view/" + id;
    }

    // ==================== مدیریت اقساط (Installments) ====================

    /**
     * **لیست اقساط معوق** (سررسید گذشته).
     */
    @GetMapping("/installments/overdue")
    public String overdueInstallments(Model model) {
        List<Installment> overdueList = installmentService.findOverdueInstallments();

        model.addAttribute("installments", overdueList);
        model.addAttribute("dateUtils", dateUtils);
        model.addAttribute("calculationService", calculationService);
        model.addAttribute("title", "اقساط معوق");

        return "installments/list";
    }

    /**
     * **لیست اقساط سررسید این هفته**.
     */
    @GetMapping("/installments/upcoming")
    public String upcomingInstallments(Model model) {
        List<Installment> upcomingList = installmentService.findInstallmentsDueThisWeek();

        model.addAttribute("installments", upcomingList);
        model.addAttribute("dateUtils", dateUtils);
        model.addAttribute("calculationService", calculationService);
        model.addAttribute("title", "اقساط سررسید این هفته");

        return "installments/list";
    }

    /**
     * **ثبت پرداخت قسط**.
     */
    @PostMapping("/installments/pay/{id}")
    public String payInstallment(
            @PathVariable Long id,
            @RequestParam Long paidAmount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String receiptNumber,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        Long contractId = null;
        try {
            Installment installment = installmentService.payInstallment(
                    id, paidAmount, paymentMethod, receiptNumber, notes);
            contractId = installment.getContract().getId();

            redirectAttributes.addFlashAttribute("successMessage",
                    "پرداخت قسط شماره " + installment.getInstallmentNumber() + " ثبت شد.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // هدایت به صفحه جزئیات قرارداد
        return (contractId != null) ? "redirect:/contracts/view/" + contractId : "redirect:/dashboard";
    }

    /**
     * **پرداخت سریع قسط** (تسویه کامل با مبلغ باقیمانده).
     */
    @PostMapping("/installments/quick-pay/{id}")
    public String quickPayInstallment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long contractId = null;
        try {
            Installment installment = installmentService.quickPay(id);
            contractId = installment.getContract().getId();
            redirectAttributes.addFlashAttribute("successMessage",
                    "قسط شماره " + installment.getInstallmentNumber() + " با موفقیت تسویه شد.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        // هدایت به صفحه جزئیات قرارداد
        return (contractId != null) ? "redirect:/contracts/view/" + contractId : "redirect:/dashboard";
    }

    // ==================== گزارش‌ها (Reports) ====================

    /**
     * **صفحه گزارش‌ها**.
     * نمایش آمار و امکانات گزارش‌گیری.
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        DashboardService.DashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("dateUtils", dateUtils);
        return "reports/index";
    }

    // ==================== API برای محاسبات AJAX ====================

    /**
     * **کلاس نتیجه محاسبات** (برای پاسخ‌های JSON).
     */
    public record CalculationResult(
            long principal,
            long interest,
            long total,
            long installment,
            int months
    ) {
        // متدهای کمکی برای فرمت‌دهی (برای استفاده مستقیم در JSON)
        public String getPrincipalFormatted() {
            return String.format("%,d", principal);
        }

        public String getInterestFormatted() {
            return String.format("%,d", interest);
        }

        public String getTotalFormatted() {
            return String.format("%,d", total);
        }

        public String getInstallmentFormatted() {
            return String.format("%,d", installment);
        }
    }

    /**
     * **محاسبه اقساط** (API برای درخواست‌های AJAX).
     */
    @GetMapping("/api/calculate")
    @ResponseBody
    public CalculationResult calculateInstallment(
            @RequestParam Long principal,
            @RequestParam Double rate,
            @RequestParam Integer months) {

        long interest = calculationService.calculateSimpleInterest(principal, rate, months);
        long total = principal + interest;
        long installment = calculationService.calculateInstallmentAmount(total, months);

        return new CalculationResult(principal, interest, total, installment, months);
    }
    // **تغییر کوچک مورد نیاز:** افزودن کامنت مستندسازی /** * Retrieves a single user profile from the database. * @param {Request} req The Express request object, including the user ID. * @param {Response} res The Express response object. */ class UserController { async getUser(req, res) { // ... لاجیک موجود } }
}