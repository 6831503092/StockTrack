/**
 * StockTrack - Shared Admin Utilities & Modal Controller
 * Centralizes role access checks, modal lifecycle, formatting, and fallback helpers.
 */
(function () {
    const STATUS_BADGE_MAP = {
        Approved: 'badge-approved',
        Rejected: 'badge-rejected',
        Pending: 'badge-pending'
    };

    const FALLBACK_ICONS = {
        warehouse: '<div class="zp-placeholder-box"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg></div>',
        product: '<div class="zp-placeholder-box"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg></div>'
    };

    function checkAdminAccess() {
        const raw = localStorage.getItem('stocktrack_user');
        if (!raw) {
            window.location.replace('../index.html');
            return false;
        }
        try {
            const user = JSON.parse(raw);
            if (!user || user.role !== 'ADMIN') {
                window.location.replace('../index.html');
                return false;
            }
            return true;
        } catch (err) {
            localStorage.removeItem('stocktrack_user');
            window.location.replace('../index.html');
            return false;
        }
    }

    function getCurrentUser() {
        const raw = localStorage.getItem('stocktrack_user');
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (err) {
            return null;
        }
    }

    function getStatusBadgeClass(status) {
        if (!status) return 'badge-pending';
        const s = String(status).toUpperCase();
        if (s === 'APPROVED' || s === 'COMPLETED' || s === 'RECEIVED' || s === 'ISSUED') {
            return 'badge-approved';
        }
        if (s === 'REJECTED' || s === 'REJECTED_ON_RECEIPT' || s === 'REJECTED_ON_ISSUE') {
            return 'badge-rejected';
        }
        return STATUS_BADGE_MAP[status] || 'badge-pending';
    }

    function formatMoney(amount) {
        return Number(amount || 0).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function getTodayISO() {
        return new Date().toISOString().split('T')[0];
    }

    function openModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (modal) modal.classList.add('active');
    }

    function closeModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (!modal) return;
        modal.classList.remove('active');
        const form = modal.querySelector('form');
        if (form) form.reset();
    }

    function initImageFallbacks() {
        document.querySelectorAll('img[data-fallback-icon]').forEach((img) => {
            const type = img.getAttribute('data-fallback-icon');
            const applyFallback = () => {
                if (FALLBACK_ICONS[type] && img.parentElement) {
                    img.parentElement.innerHTML = FALLBACK_ICONS[type];
                }
            };
            if (img.complete && img.naturalWidth === 0) {
                applyFallback();
            } else {
                img.addEventListener('error', applyFallback, { once: true });
            }
        });
    }

    // Unified delegated listeners for all Admin modals (backdrop click, close/cancel buttons, Escape key)
    document.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal-overlay')) {
            closeModal(e.target);
            return;
        }
        const closeBtn = e.target.closest('.btn-close-modal, [data-close-modal]');
        if (closeBtn) {
            const modal = closeBtn.closest('.modal-overlay');
            if (modal) closeModal(modal);
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay.active').forEach(closeModal);
        }
    });

    function matchesDateFilter(itemDateStr, selectedIsoDate) {
        if (!selectedIsoDate) return true;
        if (!itemDateStr) return false;

        const str = String(itemDateStr).trim();
        if (str.startsWith(selectedIsoDate) || str.includes(selectedIsoDate)) return true;

        const parts = selectedIsoDate.split('-');
        if (parts.length === 3) {
            const [y, m, d] = parts;
            const ddmmyyyy = `${d}/${m}/${y}`;
            const dm = `${parseInt(d, 10)}/${parseInt(m, 10)}/${y}`;
            if (str.includes(ddmmyyyy) || str.includes(dm)) return true;
        }

        const parsed = new Date(str);
        if (!isNaN(parsed.getTime())) {
            const y = parsed.getFullYear();
            const m = String(parsed.getMonth() + 1).padStart(2, '0');
            const d = String(parsed.getDate()).padStart(2, '0');
            if (`${y}-${m}-${d}` === selectedIsoDate) return true;
        }

        return false;
    }

    const CAL_MONTHS = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    function initCustomDatePicker(options = {}) {
        const box = typeof options.box === 'string'
            ? document.getElementById(options.box)
            : (options.box || document.getElementById('requests-date-box'));
        if (!box) return null;

        const input = box.querySelector('input[type="date"]');
        const clearBtn = box.querySelector('.btn-clear-date');
        const popup = box.querySelector('.custom-calendar-popup');
        const monthTitle = box.querySelector('.cal-month-title');
        const daysGrid = box.querySelector('.cal-days-grid');
        const prevBtn = box.querySelector('#cal-prev-month, .cal-nav-btn:first-of-type');
        const nextBtn = box.querySelector('#cal-next-month, .cal-nav-btn:last-of-type');
        const todayBtn = box.querySelector('#cal-btn-today, .cal-quick-btn:not(.cal-quick-clear)');
        const calClearBtn = box.querySelector('#cal-btn-clear, .cal-quick-clear');

        let viewDate = new Date();

        function renderCalendar() {
            if (!monthTitle || !daysGrid) return;
            const year = viewDate.getFullYear();
            const month = viewDate.getMonth();

            monthTitle.textContent = `${CAL_MONTHS[month]} ${year}`;

            const firstDayIndex = new Date(year, month, 1).getDay();
            const lastDayDate = new Date(year, month + 1, 0).getDate();
            const prevMonthLastDay = new Date(year, month, 0).getDate();

            const today = new Date();
            const pad = (n) => String(n).padStart(2, '0');
            const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
            const selectedIso = input ? input.value : '';

            let cellsHtml = '';

            // Previous month trailing days
            for (let i = firstDayIndex - 1; i >= 0; i--) {
                const dayNum = prevMonthLastDay - i;
                const prevDate = new Date(year, month - 1, dayNum);
                const iso = `${prevDate.getFullYear()}-${pad(prevDate.getMonth() + 1)}-${pad(dayNum)}`;
                const isSelected = iso === selectedIso ? 'is-selected' : '';
                cellsHtml += `<button type="button" class="cal-day-cell other-month ${isSelected}" data-date="${iso}">${dayNum}</button>`;
            }

            // Current month days
            for (let d = 1; d <= lastDayDate; d++) {
                const iso = `${year}-${pad(month + 1)}-${pad(d)}`;
                const isToday = iso === todayIso ? 'is-today' : '';
                const isSelected = iso === selectedIso ? 'is-selected' : '';
                cellsHtml += `<button type="button" class="cal-day-cell ${isToday} ${isSelected}" data-date="${iso}">${d}</button>`;
            }

            // Next month leading days
            const totalCells = firstDayIndex + lastDayDate;
            const remaining = (totalCells % 7 === 0) ? 0 : 7 - (totalCells % 7);
            for (let d = 1; d <= remaining; d++) {
                const nextDate = new Date(year, month + 1, d);
                const iso = `${nextDate.getFullYear()}-${pad(nextDate.getMonth() + 1)}-${pad(d)}`;
                const isSelected = iso === selectedIso ? 'is-selected' : '';
                cellsHtml += `<button type="button" class="cal-day-cell other-month ${isSelected}" data-date="${iso}">${d}</button>`;
            }

            daysGrid.innerHTML = cellsHtml;
        }

        function openPopup() {
            if (!popup) return;
            if (input && input.value) {
                const parts = input.value.split('-').map(Number);
                if (parts.length === 3 && !isNaN(parts[0]) && !isNaN(parts[1])) {
                    viewDate = new Date(parts[0], parts[1] - 1, 1);
                }
            } else {
                viewDate = new Date();
            }
            renderCalendar();
            popup.style.display = 'block';
        }

        function closePopup() {
            if (popup) popup.style.display = 'none';
        }

        function togglePopup() {
            if (!popup) return;
            if (popup.style.display === 'none' || !popup.style.display) {
                openPopup();
            } else {
                closePopup();
            }
        }

        box.addEventListener('click', (e) => {
            if (e.target.closest('.btn-clear-date')) return;
            if (e.target.closest('.custom-calendar-popup')) return;
            togglePopup();
        });

        if (input) {
            input.addEventListener('change', () => {
                if (input.value) {
                    if (clearBtn) clearBtn.style.display = 'inline-flex';
                    box.classList.add('has-date');
                } else {
                    if (clearBtn) clearBtn.style.display = 'none';
                    box.classList.remove('has-date');
                }
                if (typeof options.onChange === 'function') {
                    options.onChange(input.value);
                }
                if (popup && popup.style.display !== 'none') {
                    renderCalendar();
                }
            });
        }

        if (daysGrid) {
            daysGrid.addEventListener('click', (e) => {
                const btn = e.target.closest('.cal-day-cell');
                if (!btn) return;
                const chosenDate = btn.getAttribute('data-date');
                if (chosenDate && input) {
                    input.value = chosenDate;
                    input.dispatchEvent(new Event('change'));
                }
                closePopup();
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                viewDate.setMonth(viewDate.getMonth() - 1);
                renderCalendar();
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                viewDate.setMonth(viewDate.getMonth() + 1);
                renderCalendar();
            });
        }

        if (todayBtn) {
            todayBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                const now = new Date();
                const pad = (n) => String(n).padStart(2, '0');
                const iso = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
                if (input) {
                    input.value = iso;
                    input.dispatchEvent(new Event('change'));
                }
                closePopup();
            });
        }

        if (calClearBtn) {
            calClearBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                if (input) {
                    input.value = '';
                    input.dispatchEvent(new Event('change'));
                }
                closePopup();
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                if (input) {
                    input.value = '';
                    input.dispatchEvent(new Event('change'));
                }
                closePopup();
            });
        }

        document.addEventListener('click', (e) => {
            if (!box.contains(e.target)) {
                closePopup();
            }
        });

        return {
            open: openPopup,
            close: closePopup,
            render: renderCalendar,
            getValue: () => input ? input.value : '',
            setValue: (val) => {
                if (input) {
                    input.value = val;
                    input.dispatchEvent(new Event('change'));
                }
            }
        };
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initImageFallbacks);
    } else {
        initImageFallbacks();
    }

    window.AdminApp = {
        checkAdminAccess,
        getCurrentUser,
        getStatusBadgeClass,
        formatMoney,
        getTodayISO,
        openModal,
        closeModal,
        matchesDateFilter,
        initCustomDatePicker
    };
})();
