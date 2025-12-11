/**
 * PayMaster - Installment Management System
 * Main JavaScript File
 * Version: 1.0.0
 */

(function() {
    'use strict';

    // ==========================================
    // 1.GLOBAL CONFIGURATION
    // ==========================================
    
    const CONFIG = {
        apiBaseUrl: '/api',
        debounceDelay: 300,
        animationDuration: 300,
        toastDuration: 5000,
        dateFormat: 'YYYY/MM/DD',
        currency: 'ریال',
        locale: 'fa-IR'
    };

    // ==========================================
    // 2.UTILITY FUNCTIONS
    // ==========================================

    const Utils = {
        /**
         * فرمت کردن اعداد به فارسی با جداکننده هزارگان
         */
        formatNumber: function(num) {
            if (num === null || num === undefined || isNaN(num)) return '۰';
            return new Intl.NumberFormat(CONFIG.locale).format(num);
        },

        /**
         * فرمت کردن مبلغ به ریال
         */
        formatCurrency: function(amount) {
            return this.formatNumber(amount) + ' ' + CONFIG.currency;
        },

        /**
         * فرمت کردن مبلغ به تومان
         */
        formatToman: function(amount) {
            return this.formatNumber(Math.round(amount / 10)) + ' تومان';
        },

        /**
         * تبدیل اعداد انگلیسی به فارسی
         */
        toPersianDigits: function(str) {
            const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
            return str.toString().replace(/\d/g, d => persianDigits[d]);
        },

        /**
         * تبدیل اعداد فارسی به انگلیسی
         */
        toEnglishDigits: function(str) {
            return str.replace(/[۰-۹]/g, d => '۰۱۲۳۴۵۶۷۸۹'.indexOf(d));
        },

        /**
         * اعتبارسنجی کد ملی
         */
        validateNationalCode: function(code) {
            if (!code || code.length !== 10 || !/^\d{10}$/.test(code)) {
                return false;
            }

            // بررسی کدهای تکراری
            if (/^(\d)\1{9}$/.test(code)) {
                return false;
            }

            let sum = 0;
            for (let i = 0; i < 9; i++) {
                sum += parseInt(code.charAt(i)) * (10 - i);
            }

            const remainder = sum % 11;
            const checkDigit = parseInt(code.charAt(9));

            return (remainder < 2 && checkDigit === remainder) ||
                   (remainder >= 2 && checkDigit === 11 - remainder);
        },

        /**
         * اعتبارسنجی شماره موبایل
         */
        validateMobile: function(mobile) {
            return /^09\d{9}$/.test(mobile);
        },

        /**
         * اعتبارسنجی تاریخ شمسی
         */
        validatePersianDate: function(dateStr) {
            if (!/^\d{4}\/\d{2}\/\d{2}$/.test(dateStr)) {
                return false;
            }

            const parts = dateStr.split('/');
            const year = parseInt(parts[0]);
            const month = parseInt(parts[1]);
            const day = parseInt(parts[2]);

            if (year < 1300 || year > 1500) return false;
            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false;
            if (month > 6 && day > 30) return false;
            if (month === 12 && day > 29) return false;

            return true;
        },

        /**
         * Debounce function
         */
        debounce: function(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        },

        /**
         * نمایش Toast Message
         */
        showToast: function(message, type = 'info') {
            const toastContainer = document.getElementById('toastContainer') || this.createToastContainer();

            const toast = document.createElement('div');
            toast.className = `toast align-items-center text-white bg-${type} border-0 show`;
            toast.setAttribute('role', 'alert');
            toast.innerHTML = `
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="bi bi-${this.getToastIcon(type)} me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            `;

            toastContainer.appendChild(toast);

            setTimeout(() => {
                toast.classList.remove('show');
                setTimeout(() => toast.remove(), CONFIG.animationDuration);
            }, CONFIG.toastDuration);
        },

        createToastContainer: function() {
            const container = document.createElement('div');
            container.id = 'toastContainer';
            // استفاده از position-fixed برای اطمینان از نمایش صحیح
            container.className = 'toast-container position-fixed bottom-0 start-0 p-3';
            container.style.zIndex = '1100';
            document.body.appendChild(container);
            return container;
        },

        getToastIcon: function(type) {
            const icons = {
                success: 'check-circle-fill',
                danger: 'exclamation-triangle-fill',
                warning: 'exclamation-circle-fill',
                info: 'info-circle-fill'
            };
            return icons[type] || icons.info;
        },

        /**
         * تایید عملیات
         */
        confirm: function(message, callback) {
            if (window.confirm(message)) {
                callback();
            }
        },

        /**
         * کپی به کلیپ‌بورد
         */
        copyToClipboard: function(text) {
            navigator.clipboard.writeText(text).then(() => {
                this.showToast('کپی شد!', 'success');
            }).catch(() => {
                this.showToast('خطا در کپی کردن', 'danger');
            });
        }
    };

    // ==========================================
    // 3.API SERVICE
    // ==========================================

    const ApiService = {
        /**
         * درخواست GET
         */
        get: async function(url, params = {}) {
            const queryString = new URLSearchParams(params).toString();
            const fullUrl = queryString ? `${CONFIG.apiBaseUrl}${url}?${queryString}` : `${CONFIG.apiBaseUrl}${url}`;

            try {
                const response = await fetch(fullUrl);
                if (!response.ok) throw new Error('Network response was not ok');
                return await response.json();
            } catch (error) {
                console.error('API GET Error:', error);
                throw error;
            }
        },

        /**
         * درخواست POST
         */
        post: async function(url, data = {}) {
            try {
                const response = await fetch(`${CONFIG.apiBaseUrl}${url}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data)
                });
                if (!response.ok) throw new Error('Network response was not ok');
                return await response.json();
            } catch (error) {
                console.error('API POST Error:', error);
                throw error;
            }
        },

        /**
         * بررسی کد ملی
         */
        checkNationalCode: async function(nationalCode, excludeId = null) {
            const params = { nationalCode };
            if (excludeId) params.excludeId = excludeId;
            return await this.get('/check-national-code', params);
        },

        /**
         * بررسی موبایل
         */
        checkMobile: async function(mobile, excludeId = null) {
            const params = { mobile };
            if (excludeId) params.excludeId = excludeId;
            return await this.get('/check-mobile', params);
        },

        /**
         * محاسبه اقساط
         */
        calculateInstallments: async function(principal, rate, months) {
            return await this.get('/calculate-installments', { principal, rate, months });
        },

        /**
         * تبدیل تاریخ
         */
        convertDate: async function(persianDate) {
            return await this.get('/convert-date', { persianDate });
        },

        /**
         * جستجوی مشتری
         */
        searchCustomers: async function(query) {
            return await this.get('/search-customers', { q: query });
        }
    };

    // ==========================================
    // 4.FORM VALIDATION
    // ==========================================

    const FormValidator = {
        /**
         * راه‌اندازی اعتبارسنجی فرم
         */
        init: function() {
            const forms = document.querySelectorAll('form.needs-validation');

            forms.forEach(form => {
                form.addEventListener('submit', function(event) {
                    if (!form.checkValidity()) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated');
                });
            });

            // اعتبارسنجی کد ملی
            this.setupNationalCodeValidation();

            // اعتبارسنجی موبایل
            this.setupMobileValidation();

            // اعتبارسنجی تاریخ شمسی
            this.setupPersianDateValidation();
        },

        /**
         * اعتبارسنجی کد ملی
         */
        setupNationalCodeValidation: function() {
            const nationalCodeInput = document.getElementById('nationalCode');
            if (!nationalCodeInput) return;

            const statusEl = document.getElementById('nationalCodeStatus');
            const customerId = document.querySelector('input[name="id"]')?.value;

            const validateCode = Utils.debounce(async function() {
                const code = nationalCodeInput.value.trim();

                if (code.length !== 10) {
                    if (statusEl) statusEl.innerHTML = '';
                    nationalCodeInput.classList.remove('is-valid', 'is-invalid');
                    return;
                }

                // اعتبارسنجی محلی
                if (!Utils.validateNationalCode(code)) {
                    if (statusEl) statusEl.innerHTML = '<i class="bi bi-x-lg text-danger"></i>';
                    nationalCodeInput.classList.remove('is-valid');
                    nationalCodeInput.classList.add('is-invalid');
                    return;
                }

                // بررسی از سرور
                try {
                    const result = await ApiService.checkNationalCode(code, customerId);
                    if (result.valid) {
                        if (statusEl) statusEl.innerHTML = '<i class="bi bi-check-lg text-success"></i>';
                        nationalCodeInput.classList.remove('is-invalid');
                        nationalCodeInput.classList.add('is-valid');
                    } else {
                        if (statusEl) statusEl.innerHTML = '<i class="bi bi-x-lg text-danger"></i>';
                        nationalCodeInput.classList.remove('is-valid');
                        nationalCodeInput.classList.add('is-invalid');
                    }
                } catch (error) {
                    console.error('National code validation error:', error);
                }
            }, CONFIG.debounceDelay);

            nationalCodeInput.addEventListener('input', validateCode);
            nationalCodeInput.addEventListener('blur', validateCode);
        },

        /**
         * اعتبارسنجی موبایل
         */
        setupMobileValidation: function() {
            const mobileInput = document.getElementById('mobile');
            if (!mobileInput) return;

            const customerId = document.querySelector('input[name="id"]')?.value;

            const validateMobile = Utils.debounce(async function() {
                const mobile = mobileInput.value.trim();

                if (!Utils.validateMobile(mobile)) {
                    mobileInput.classList.remove('is-valid');
                    if (mobile.length > 0) {
                        mobileInput.classList.add('is-invalid');
                    }
                    return;
                }

                try {
                    const result = await ApiService.checkMobile(mobile, customerId);
                    if (result.valid) {
                        mobileInput.classList.remove('is-invalid');
                        mobileInput.classList.add('is-valid');
                    } else {
                        mobileInput.classList.remove('is-valid');
                        mobileInput.classList.add('is-invalid');
                    }
                } catch (error) {
                    console.error('Mobile validation error:', error);
                }
            }, CONFIG.debounceDelay);

            mobileInput.addEventListener('input', validateMobile);
            mobileInput.addEventListener('blur', validateMobile);
        },

        /**
         * اعتبارسنجی تاریخ شمسی
         */
        setupPersianDateValidation: function() {
            const dateInputs = document.querySelectorAll('input[name*="Persian"], input[name*="Date"]');

            dateInputs.forEach(input => {
                input.addEventListener('blur', function() {
                    const value = this.value.trim();
                    if (value && !Utils.validatePersianDate(value)) {
                        this.classList.add('is-invalid');
                    } else {
                        this.classList.remove('is-invalid');
                    }
                });
            });
        }
    };

    // ==========================================
    // 5.CONTRACT CALCULATOR
    // ==========================================

    const ContractCalculator = {
        chart: null,

        /**
         * راه‌اندازی ماشین‌حساب قرارداد
         */
        init: function() {
            const principalInput = document.getElementById('principalAmount');
            const rateInput = document.getElementById('interestRate');
            const monthsSelect = document.getElementById('installmentCount');
            const calculateBtn = document.getElementById('btnCalculate');

            if (!principalInput || !rateInput || !monthsSelect) return;

            const calculate = () => this.calculate();

            principalInput.addEventListener('input', Utils.debounce(calculate, CONFIG.debounceDelay));
            rateInput.addEventListener('input', Utils.debounce(calculate, CONFIG.debounceDelay));
            monthsSelect.addEventListener('change', calculate);

            if (calculateBtn) {
                calculateBtn.addEventListener('click', calculate);
            }

            // محاسبه اولیه
            this.calculate();
        },

        /**
         * محاسبه و نمایش نتایج
         */
        calculate: async function() {
            const principal = parseInt(document.getElementById('principalAmount')?.value) || 0;
            const rate = parseFloat(document.getElementById('interestRate')?.value) || 0;
            const months = parseInt(document.getElementById('installmentCount')?.value) || 0;

            if (principal <= 0 || months <= 0) {
                this.clearResults();
                return;
            }

            try {
                const data = await ApiService.calculateInstallments(principal, rate, months);
                this.displayResults(data);
                this.updateChart(data.principal, data.interest);
            } catch (error) {
                console.error('Calculation error:', error);
                // محاسبه محلی در صورت خطا
                this.calculateLocally(principal, rate, months);
            }
        },

        /**
         * محاسبه محلی (بدون API)
         */
        calculateLocally: function(principal, rate, months) {
            const interest = Math.round((principal * rate * months) / (12 * 100));
            const total = principal + interest;
            const installment = Math.round(total / months);

            const data = {
                principal: principal,
                interest: interest,
                total: total,
                installmentAmount: installment
            };

            this.displayResults(data);
            this.updateChart(principal, interest);
        },

        /**
         * نمایش نتایج
         */
        displayResults: function(data) {
            const elements = {
                principal: document.getElementById('calcPrincipal'),
                interest: document.getElementById('calcInterest'),
                total: document.getElementById('calcTotal'),
                installment: document.getElementById('calcInstallment')
            };

            if (elements.principal) {
                elements.principal.textContent = Utils.formatCurrency(data.principal);
            }
            if (elements.interest) {
                elements.interest.textContent = Utils.formatCurrency(data.interest);
            }
            if (elements.total) {
                elements.total.textContent = Utils.formatCurrency(data.total);
            }
            if (elements.installment) {
                elements.installment.textContent = Utils.formatCurrency(data.installmentAmount);
            }
        },

        /**
         * پاک کردن نتایج
         */
        clearResults: function() {
            const ids = ['calcPrincipal', 'calcInterest', 'calcTotal', 'calcInstallment'];
            ids.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.textContent = '۰ ' + CONFIG.currency;
            });
        },

        /**
         * بروزرسانی نمودار
         */
        updateChart: function(principal, interest) {
            const ctx = document.getElementById('pieChart');
            if (!ctx) return;

            if (this.chart) {
                this.chart.destroy();
            }

            this.chart = new Chart(ctx.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: ['اصل سرمایه', 'سود'],
                    datasets: [{
                        data: [principal, interest],
                        backgroundColor: ['#3498db', '#f39c12'],
                        borderWidth: 0,
                        hoverOffset: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                font: {
                                    family: 'Vazirmatn',
                                    size: 12
                                },
                                padding: 20
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return context.label + ': ' + Utils.formatCurrency(context.raw);
                                }
                            }
                        }
                    },
                    cutout: '60%'
                }
            });
        }
    };

    // ==========================================
    // 6.DATA TABLE ENHANCEMENTS
    // ==========================================

    const DataTable = {
        /**
         * راه‌اندازی جداول
         */
        init: function() {
            this.setupSearch();
            this.setupRowSelection();
            this.setupSorting();
        },

        /**
         * جستجوی زنده در جدول
         */
        setupSearch: function() {
            const searchInput = document.querySelector('.table-search-input');
            const table = document.querySelector('.table tbody');

            if (!searchInput || !table) return;

            searchInput.addEventListener('input', Utils.debounce(function() {
                const query = this.value.toLowerCase().trim();
                const rows = table.querySelectorAll('tr');

                rows.forEach(row => {
                    const text = row.textContent.toLowerCase();
                    row.style.display = text.includes(query) ? '' : 'none';
                });
            }, CONFIG.debounceDelay));
        },

        /**
         * انتخاب ردیف‌ها
         */
        setupRowSelection: function() {
            const checkboxes = document.querySelectorAll('.row-checkbox');
            const selectAll = document.querySelector('.select-all-checkbox');

            if (!selectAll) return;

            selectAll.addEventListener('change', function() {
                checkboxes.forEach(cb => {
                    cb.checked = this.checked;
                });
            });
        },

        /**
         * مرتب‌سازی جدول
         */
        setupSorting: function() {
            const sortableHeaders = document.querySelectorAll('th[data-sortable]');

            sortableHeaders.forEach(header => {
                header.style.cursor = 'pointer';
                header.addEventListener('click', function() {
                    const column = this.dataset.column;
                    const order = this.dataset.order === 'asc' ? 'desc' : 'asc';
                    this.dataset.order = order;

                    // اینجا می‌توان مرتب‌سازی را پیاده‌سازی کرد
                    console.log('Sort by:', column, order);
                });
            });
        }
    };

    // ==========================================
    // 7.UI COMPONENTS
    // ==========================================

    const UIComponents = {
        /**
         * راه‌اندازی کامپوننت‌ها
         */
        init: function() {
            this.setupTooltips();
            this.setupPopovers();
            this.setupModals();
            this.setupDropdowns();
            this.setupAlertDismiss();
            this.setupLoadingButtons();
            this.setupScrollToTop();
        },

        /**
         * Tooltips
         */
        setupTooltips: function() {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.forEach(el => new bootstrap.Tooltip(el));
        },

        /**
         * Popovers
         */
        setupPopovers: function() {
            const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
            popoverTriggerList.forEach(el => new bootstrap.Popover(el));
        },

        /**
         * Modals
         */
        setupModals: function() {
            // تنظیم فوکوس روی اولین input در مودال
            document.querySelectorAll('.modal').forEach(modal => {
                modal.addEventListener('shown.bs.modal', function() {
                    const firstInput = this.querySelector('input:not([type="hidden"]), select, textarea');
                    if (firstInput) firstInput.focus();
                });
            });
        },

        /**
         * Dropdowns
         */
        setupDropdowns: function() {
            // هر تنظیم سفارشی برای dropdowns
        },

        /**
         * حذف خودکار Alerts
         */
        setupAlertDismiss: function() {
            document.querySelectorAll('.alert:not(.alert-permanent)').forEach(alert => {
                setTimeout(() => {
                    alert.classList.add('fade');
                    setTimeout(() => alert.remove(), CONFIG.animationDuration);
                }, CONFIG.toastDuration);
            });
        },

        /**
         * دکمه‌های با Loading
         */
        setupLoadingButtons: function() {
            document.querySelectorAll('form').forEach(form => {
                form.addEventListener('submit', function() {
                    const submitBtn = this.querySelector('button[type="submit"]');
                    if (submitBtn && !submitBtn.disabled) {
                        const originalText = submitBtn.innerHTML;
                        submitBtn.disabled = true;
                        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>در حال پردازش...';

                        // بازگردانی در صورت خطا (در نهایت توسط سرور یا ریدایرکت حل می‌شود، اما این یک safety net است)
                        setTimeout(() => {
                            submitBtn.disabled = false;
                            submitBtn.innerHTML = originalText;
                        }, 10000);
                    }
                });
            });
        },

        /**
         * دکمه برگشت به بالا
         */
        setupScrollToTop: function() {
            const scrollBtn = document.createElement('button');
            scrollBtn.className = 'btn btn-primary btn-scroll-top';
            scrollBtn.innerHTML = '<i class="bi bi-arrow-up"></i>';
            scrollBtn.style.cssText = `
                position: fixed;
                bottom: 20px;
                left: 20px;
                width: 45px;
                height: 45px;
                border-radius: 50%;
                display: none;
                z-index: 1000;
                box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            `;
            document.body.appendChild(scrollBtn);

            window.addEventListener('scroll', function() {
                scrollBtn.style.display = window.scrollY > 300 ? 'block' : 'none';
            });

            scrollBtn.addEventListener('click', function() {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            });
        }
    };

    // ==========================================
    // 8.KEYBOARD SHORTCUTS
    // ==========================================

    const KeyboardShortcuts = {
        init: function() {
            document.addEventListener('keydown', function(e) {
                // Ctrl/Cmd + K: جستجو
                if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                    e.preventDefault();
                    const searchInput = document.querySelector('input[name="keyword"]');
                    if (searchInput) searchInput.focus();
                }

                // Ctrl/Cmd + N: مشتری جدید
                if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
                    e.preventDefault();
                    window.location.href = '/customers/new';
                }

                // Escape: بستن مودال
                if (e.key === 'Escape') {
                    const openModal = document.querySelector('.modal.show');
                    if (openModal) {
                        bootstrap.Modal.getInstance(openModal)?.hide();
                    }
                }
            });
        }
    };

    // ==========================================
    // 9.NUMBER INPUT FORMATTING
    // ==========================================

    const NumberInput = {
        init: function() {
            // فرمت کردن input های عددی
            document.querySelectorAll('input[type="number"][data-format]').forEach(input => {
                input.addEventListener('blur', function() {
                    // منطق فرمت‌دهی (مثلاً اضافه کردن جداکننده هزارگان)
                    const value = parseInt(this.value) || 0;
                    // می‌توان اینجا فرمت سفارشی اعمال کرد
                });
            });

            // محدود کردن ورودی فقط به اعداد
            document.querySelectorAll('input[data-digits-only]').forEach(input => {
                input.addEventListener('input', function() {
                    this.value = this.value.replace(/\D/g, '');
                });
            });
        }
    };

    // ==========================================
    // 10.PRINT FUNCTIONALITY
    // ==========================================

    const PrintManager = {
        /**
         * چاپ بخش خاصی از صفحه
         */
        printSection: function(sectionId) {
            const section = document.getElementById(sectionId);
            if (!section) return;

            const printWindow = window.open('', '_blank');
            printWindow.document.write(`
                <!DOCTYPE html>
                <html dir="rtl" lang="fa">
                <head>
                    <meta charset="UTF-8">
                    <title>چاپ - پی‌مستر</title>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.rtl.min.css" rel="stylesheet">
                    <link href="https://cdn.jsdelivr.net/gh/rastikerdar/vazirmatn@v33.003/Vazirmatn-font-face.css" rel="stylesheet">
                    <style>
                        body { font-family: 'Vazirmatn', sans-serif; padding: 20px; }
                        @media print { .no-print { display: none; } }
                    </style>
                </head>
                <body>
                    ${section.innerHTML}
                    <script>window.onload = function() { window.print(); window.close(); }<\/script>
                </body>
                </html>
            `);
            printWindow.document.close();
        },

        /**
         * چاپ کل صفحه
         */
        printPage: function() {
            window.print();
        }
    };

    // ==========================================
    // 11.LOCAL STORAGE MANAGER
    // ==========================================

    const StorageManager = {
        /**
         * ذخیره داده
         */
        set: function(key, value) {
            try {
                localStorage.setItem('pm_' + key, JSON.stringify(value));
            } catch (e) {
                console.error('Storage set error:', e);
            }
        },

        /**
         * دریافت داده
         */
        get: function(key, defaultValue = null) {
            try {
                const item = localStorage.getItem('pm_' + key);
                return item ? JSON.parse(item) : defaultValue;
            } catch (e) {
                console.error('Storage get error:', e);
                return defaultValue;
            }
        },

        /**
         * حذف داده
         */
        remove: function(key) {
            localStorage.removeItem('pm_' + key);
        },

        /**
         * پاک کردن همه داده‌ها
         */
        clear: function() {
            Object.keys(localStorage)
                .filter(key => key.startsWith('pm_'))
                .forEach(key => localStorage.removeItem(key));
        }
    };

    // ==========================================
    // 12.INITIALIZATION
    // ==========================================

    const App = {
        /**
         * راه‌اندازی برنامه
         */
        init: function() {
            console.log('🚀 PayMaster App Initialized');

            // راه‌اندازی ماژول‌ها
            FormValidator.init();
            ContractCalculator.init();
            DataTable.init();
            UIComponents.init();
            KeyboardShortcuts.init();
            NumberInput.init();

            // Event های سفارشی
            this.setupCustomEvents();
        },

        /**
         * Event های سفارشی
         */
        setupCustomEvents: function() {
            // نمایش پیام‌های Flash
            const successMessage = document.querySelector('[data-success-message]');
            const errorMessage = document.querySelector('[data-error-message]');

            if (successMessage) {
                // استفاده از data-attribute
                Utils.showToast(successMessage.dataset.successMessage, 'success');
                // حذف attribute برای جلوگیری از نمایش مجدد در ریدایرکت‌های بعدی
                successMessage.removeAttribute('data-success-message');
            }
            if (errorMessage) {
                // استفاده از data-attribute
                Utils.showToast(errorMessage.dataset.errorMessage, 'danger');
                errorMessage.removeAttribute('data-error-message');
            }
        }
    };

    // ==========================================
    // 13.EXPOSE TO GLOBAL SCOPE
    // ==========================================

    window.PayMaster = {
        Utils: Utils,
        Api: ApiService,
        Calculator: ContractCalculator,
        Print: PrintManager,
        Storage: StorageManager,
        showToast: Utils.showToast.bind(Utils)
    };

    // ==========================================
    // 14.DOM READY
    // ==========================================

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => App.init());
    } else {
        App.init();
    }

})();