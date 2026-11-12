/**
 * StockTrack - Shared Admin Utilities & Modal Controller
 * Centralizes role access checks, modal lifecycle, formatting, sanitization, and datepicker helpers.
 */
(function () {
    const STATUS_BADGE_MAP = {
        Approved: 'badge-approved',
        Rejected: 'badge-rejected',
        Pending: 'badge-pending'
    };

    const APPROVED_STATUSES = new Set(['APPROVED', 'COMPLETED', 'RECEIVED', 'ISSUED']);
    const REJECTED_STATUSES = new Set(['REJECTED', 'REJECTED_ON_RECEIPT', 'REJECTED_ON_ISSUE']);

    const CURRENCY_FORMATTER = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

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

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatMoney(amount) {
        return CURRENCY_FORMATTER.format(Number(amount) || 0);
    }

    function formatDisplayDate(dateVal) {
        if (!dateVal) return '-';
        try {
            let year, month, day;
            const pad = (n) => String(n).padStart(2, '0');

            if (Array.isArray(dateVal)) {
                year = dateVal[0];
                month = dateVal[1];
                day = dateVal[2];
            } else if (typeof dateVal === 'string') {
                const str = dateVal.trim();
                // Check if already in dd/mm/yyyy format (day/month/year)
                const dmyMatch = str.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
                if (dmyMatch) {
                    day = parseInt(dmyMatch[1], 10);
                    month = parseInt(dmyMatch[2], 10);
                    year = parseInt(dmyMatch[3], 10);
                } else {
                    // Check ISO format: yyyy-mm-dd or yyyy-mm-ddTHH:mm:ss
                    const isoMatch = str.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
                    if (isoMatch) {
                        year = parseInt(isoMatch[1], 10);
                        month = parseInt(isoMatch[2], 10);
                        day = parseInt(isoMatch[3], 10);
                    } else {
                        const d = new Date(str);
                        if (isNaN(d.getTime())) return str;
                        year = d.getFullYear();
                        month = d.getMonth() + 1;
                        day = d.getDate();
                    }
                }
            } else if (dateVal instanceof Date) {
                if (isNaN(dateVal.getTime())) return '-';
                year = dateVal.getFullYear();
                month = dateVal.getMonth() + 1;
                day = dateVal.getDate();
            } else {
                return String(dateVal);
            }

            return `${pad(day)}/${pad(month)}/${year}`;
        } catch {
            return String(dateVal);
        }
    }

    function formatDisplayStatus(statusVal) {
        if (!statusVal) return 'Pending';
        const s = String(statusVal).trim();
        return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
    }

    function getStatusBadgeClass(status) {
        if (!status) return 'badge-pending';
        const s = String(status).toUpperCase();
        if (APPROVED_STATUSES.has(s)) return 'badge-approved';
        if (REJECTED_STATUSES.has(s)) return 'badge-rejected';
        return STATUS_BADGE_MAP[status] || 'badge-pending';
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

        const pad = (n) => String(n).padStart(2, '0');
        let itemIso = '';

        if (Array.isArray(itemDateStr)) {
            itemIso = `${itemDateStr[0]}-${pad(itemDateStr[1])}-${pad(itemDateStr[2])}`;
        } else if (typeof itemDateStr === 'string') {
            const str = itemDateStr.trim();
            const dmyMatch = str.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
            if (dmyMatch) {
                itemIso = `${dmyMatch[3]}-${pad(dmyMatch[2])}-${pad(dmyMatch[1])}`;
            } else {
                const isoMatch = str.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
                if (isoMatch) {
                    itemIso = `${isoMatch[1]}-${pad(isoMatch[2])}-${pad(isoMatch[3])}`;
                }
            }
        } else if (itemDateStr instanceof Date && !isNaN(itemDateStr.getTime())) {
            itemIso = `${itemDateStr.getFullYear()}-${pad(itemDateStr.getMonth() + 1)}-${pad(itemDateStr.getDate())}`;
        }

        if (itemIso) {
            return itemIso === selectedIsoDate;
        }

        return String(itemDateStr).includes(selectedIsoDate);
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

        // Elegant dd/mm/yyyy display label instead of native browser locale-dependent text
        let label = box.querySelector('.date-display-label');
        if (!label && input) {
            label = document.createElement('span');
            label.className = 'date-display-label';
            label.textContent = input.value ? formatDisplayDate(input.value) : 'dd/mm/yyyy';
            input.parentNode.insertBefore(label, input);
            input.style.display = 'none';
        }

        function updateDisplayLabel() {
            if (label) {
                if (input && input.value) {
                    label.textContent = formatDisplayDate(input.value);
                } else {
                    label.textContent = 'dd/mm/yyyy';
                }
            }
        }

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
                updateDisplayLabel();
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
            updateDisplayLabel();
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

        const handleClear = (e) => {
            e.stopPropagation();
            if (input) {
                input.value = '';
                input.dispatchEvent(new Event('change'));
            }
            closePopup();
        };

        if (calClearBtn) calClearBtn.addEventListener('click', handleClear);
        if (clearBtn) clearBtn.addEventListener('click', handleClear);

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

    function showToast(message, type = 'success', title = null, duration = 3800) {
        let container = document.getElementById('stocktrack-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'stocktrack-toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        const icons = {
            success: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`,
            error: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`,
            info: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`
        };

        const defaultTitles = {
            success: 'Success',
            error: 'Error',
            info: 'Notice'
        };

        const toastType = ['success', 'error', 'info'].includes(type) ? type : 'success';
        const displayTitle = title || defaultTitles[toastType];

        const item = document.createElement('div');
        item.className = `toast-item toast-${toastType}`;
        item.innerHTML = `
            <div class="toast-icon">
                ${icons[toastType]}
            </div>
            <div class="toast-content">
                <div class="toast-title">${escapeHtml(displayTitle)}</div>
                <div class="toast-message">${escapeHtml(message)}</div>
            </div>
            <button type="button" class="toast-close-btn" aria-label="Close">&times;</button>
        `;

        const removeToast = () => {
            if (item.classList.contains('toast-hiding')) return;
            item.classList.add('toast-hiding');
            setTimeout(() => {
                if (item.parentNode) {
                    item.parentNode.removeChild(item);
                }
            }, 320);
        };

        item.querySelector('.toast-close-btn')?.addEventListener('click', removeToast);

        container.appendChild(item);

        if (duration > 0) {
            setTimeout(removeToast, duration);
        }
    }

    function showCenterAlert({ title = 'Success!', message = '', type = 'success', confirmText = 'OK', onConfirm = null } = {}) {
        let overlay = document.getElementById('stocktrack-center-alert');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'stocktrack-center-alert';
            overlay.className = 'center-alert-overlay';
            document.body.appendChild(overlay);
        }

        const icons = {
            success: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`,
            error: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`
        };

        const alertType = ['success', 'error'].includes(type) ? type : 'success';
        const displayTitle = title || (alertType === 'success' ? 'Success!' : 'Error');

        overlay.innerHTML = `
            <div class="center-alert-box">
                <div class="center-alert-icon ${alertType}">
                    ${icons[alertType]}
                </div>
                <h3 class="center-alert-title">${escapeHtml(displayTitle)}</h3>
                <p class="center-alert-message">${escapeHtml(message)}</p>
                <button type="button" class="center-alert-btn" id="btn-center-alert-ok">${escapeHtml(confirmText)}</button>
            </div>
        `;

        const closeAlert = () => {
            overlay.classList.remove('active');
            if (typeof onConfirm === 'function') {
                onConfirm();
            }
        };

        overlay.querySelector('#btn-center-alert-ok')?.addEventListener('click', closeAlert);
        overlay.onclick = (e) => {
            if (e.target === overlay) closeAlert();
        };

        // Trigger opening animation smoothly
        requestAnimationFrame(() => {
            overlay.classList.add('active');
            overlay.querySelector('#btn-center-alert-ok')?.focus();
        });
    }

    const PRODUCT_IMAGE_MAP = {
        'officedesk': '../picture/zone1/OfficeDesk.webp',
        'diningtable': '../picture/zone1/DiningTable.webp',
        'coffeetable': '../picture/zone1/cofeetable.webp',
        'computerdesk': '../picture/zone1/computer-desk.webp',
        'meetingtable': '../picture/zone1/meetingtable.webp',
        'officechair': '../picture/zone2/OfficeChair.webp',
        'diningchair': '../picture/zone2/dinningchair.webp',
        'gamingchair': '../picture/zone2/GamingChair.webp',
        'foldingchair': '../picture/zone2/FoldingChair.webp',
        'sofachair': '../picture/zone2/SofaChair.webp',
        'singlebed': '../picture/zone3/SingleBed.webp',
        'queenbed': '../picture/zone3/queenbed.webp',
        'kingbed': '../picture/zone3/KingBed.webp',
        'bunkbed': '../picture/zone3/BunkBed.webp',
        'foldingbed': '../picture/zone3/FoldingBed.webp',
        'wardrobe': '../picture/zone4/Wardrobe.webp',
        'shoecabinet': '../picture/zone4/ShoeCabinet.webp',
        'bookshelf': '../picture/zone4/bookshelf.webp',
        'tvcabinet': '../picture/zone4/TVCabinet.webp',
        'drawercabinet': '../picture/zone4/DrawerCabinet.webp'
    };

    function getProductImageUrl(productName, zoneCode) {
        if (!productName) return '../picture/favicon/favicon.png';
        const clean = String(productName).toLowerCase().replace(/[^a-z0-9]/g, '');

        if (PRODUCT_IMAGE_MAP[clean]) {
            return PRODUCT_IMAGE_MAP[clean];
        }

        for (const k in PRODUCT_IMAGE_MAP) {
            if (clean.includes(k) || k.includes(clean)) {
                return PRODUCT_IMAGE_MAP[k];
            }
        }

        const z = String(zoneCode || '').toUpperCase();
        if (z === 'Z1') return '../picture/zone1/OfficeDesk.webp';
        if (z === 'Z2') return '../picture/zone2/OfficeChair.webp';
        if (z === 'Z3') return '../picture/zone3/SingleBed.webp';
        if (z === 'Z4') return '../picture/zone4/bookshelf.webp';

        return '../picture/favicon/favicon.png';
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
        formatDisplayDate,
        formatDisplayStatus,
        escapeHtml,
        getTodayISO,
        openModal,
        closeModal,
        matchesDateFilter,
        initCustomDatePicker,
        showToast,
        showCenterAlert,
        getProductImageUrl
    };
    window.showToast = showToast;
    window.showCenterAlert = showCenterAlert;
})();
