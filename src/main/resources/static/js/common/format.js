/**
 * StockTrack - Common Formatting Utilities
 * Standardizes date, currency, status badge, and HTML escaping across all roles.
 */
(function () {
    const CURRENCY_FORMATTER = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    const CAL_MONTHS = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const SHORT_MONTHS = [
        'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
    ];

    const pad2 = (n) => String(n).padStart(2, '0');

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
        const num = Number(amount);
        return isNaN(num) ? '0.00' : CURRENCY_FORMATTER.format(num);
    }

    function formatCurrency(amount) {
        return '฿' + formatMoney(amount);
    }

    function parseDateParts(dateVal) {
        if (!dateVal) return null;
        if (Array.isArray(dateVal) && dateVal.length >= 3) {
            return {
                year: Number(dateVal[0]),
                month: Number(dateVal[1]),
                day: Number(dateVal[2]),
                hour: dateVal.length >= 4 ? Number(dateVal[3]) : 0,
                minute: dateVal.length >= 5 ? Number(dateVal[4]) : 0
            };
        }
        if (typeof dateVal === 'string') {
            const trimmed = dateVal.trim();
            const writtenMatch = trimmed.match(/^(\d{1,2})\s+([A-Za-z]{3})\s+(\d{4})(?:[,\s]+(\d{1,2}):(\d{2}))?/);
            if (writtenMatch) {
                const monthIdx = SHORT_MONTHS.findIndex(m => m.toLowerCase() === writtenMatch[2].toLowerCase());
                return {
                    year: Number(writtenMatch[3]),
                    month: monthIdx !== -1 ? monthIdx + 1 : 1,
                    day: Number(writtenMatch[1]),
                    hour: writtenMatch[4] ? Number(writtenMatch[4]) : 0,
                    minute: writtenMatch[5] ? Number(writtenMatch[5]) : 0
                };
            }
            if (trimmed.includes('T')) {
                const [dPart, tPart] = trimmed.split('T');
                const [y, m, d] = dPart.split('-').map(Number);
                const [hh, mm] = (tPart || '00:00').split(':').map(Number);
                if (y && m && d) return { year: y, month: m, day: d, hour: hh || 0, minute: mm || 0 };
            }
            if (trimmed.includes('-')) {
                const spaceParts = trimmed.split(' ');
                const parts = spaceParts[0].split('-').map(Number);
                const timeParts = spaceParts[1] ? spaceParts[1].split(':').map(Number) : [0, 0];
                if (parts.length >= 3) return { year: parts[0], month: parts[1], day: parts[2], hour: timeParts[0] || 0, minute: timeParts[1] || 0 };
            }
            if (trimmed.includes('/')) {
                const spaceParts = trimmed.split(' ');
                const parts = spaceParts[0].split('/').map(Number);
                const timeParts = spaceParts[1] ? spaceParts[1].split(':').map(Number) : [0, 0];
                if (parts.length >= 3) return { year: parts[2], month: parts[1], day: parts[0], hour: timeParts[0] || 0, minute: timeParts[1] || 0 };
            }
            const d = new Date(trimmed);
            if (!isNaN(d.getTime())) {
                return {
                    year: d.getFullYear(),
                    month: d.getMonth() + 1,
                    day: d.getDate(),
                    hour: d.getHours(),
                    minute: d.getMinutes()
                };
            }
        }
        if (dateVal instanceof Date && !isNaN(dateVal.getTime())) {
            return {
                year: dateVal.getFullYear(),
                month: dateVal.getMonth() + 1,
                day: dateVal.getDate(),
                hour: dateVal.getHours(),
                minute: dateVal.getMinutes()
            };
        }
        return null;
    }

    function formatDisplayDate(dateVal) {
        if (!dateVal) return '-';
        if (typeof dateVal === 'string' && /^\d{1,2}\s+[A-Za-z]{3}\s+\d{4},\s*\d{2}:\d{2}$/.test(dateVal.trim())) {
            return dateVal.trim();
        }
        const parts = parseDateParts(dateVal);
        if (!parts) return String(dateVal).trim() || '-';
        const monthName = SHORT_MONTHS[parts.month - 1] || pad2(parts.month);
        return `${pad2(parts.day)} ${monthName} ${parts.year}, ${pad2(parts.hour || 0)}:${pad2(parts.minute || 0)}`;
    }

    function formatDisplayDateOnly(dateVal) {
        const parts = parseDateParts(dateVal);
        if (!parts) return '-';
        return `${pad2(parts.day)}/${pad2(parts.month)}/${parts.year}`;
    }

    function formatReviewDateTime(dateVal) {
        return formatDisplayDate(dateVal);
    }

    function getTodayISO() {
        const now = new Date();
        return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`;
    }

    const APPROVED_SET = new Set(['APPROVED', 'COMPLETED', 'RECEIVED', 'ISSUED', 'STOCK_OUT', 'SUCCESS']);
    const REJECTED_SET = new Set(['REJECTED', 'REJECTED_ON_RECEIPT', 'REJECTED_ON_ISSUE', 'REJECTED_ON_STOCK_OUT', 'FAILED']);

    function getStatusBadgeClass(status) {
        if (!status) return 'badge-pending';
        const s = String(status).trim().toUpperCase();
        if (APPROVED_SET.has(s)) return 'badge-approved';
        if (REJECTED_SET.has(s)) return 'badge-rejected';
        return 'badge-pending';
    }

    function formatDisplayStatus(status) {
        if (!status) return 'Pending';
        const raw = String(status).trim();
        const upper = raw.toUpperCase();
        if (upper === 'REJECTED_ON_RECEIPT') return 'Rejected (Receipt)';
        if (upper === 'REJECTED_ON_STOCK_OUT' || upper === 'REJECTED_ON_ISSUE') return 'Rejected (Stock Out)';
        if (upper === 'STOCK_OUT') return 'Stock Out';
        if (upper === 'PENDING_VERIFICATION') return 'Pending Verification';
        return raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
    }

    function renderStatusBadge(status, customLabel) {
        const badgeClass = getStatusBadgeClass(status);
        const label = customLabel || formatDisplayStatus(status);
        return `<span class="badge-status ${badgeClass}">${escapeHtml(label)}</span>`;
    }

    const Format = {
        escapeHtml,
        formatMoney,
        formatCurrency,
        parseDateParts,
        formatDisplayDate,
        formatDisplayDateOnly,
        formatReviewDateTime,
        getTodayISO,
        getStatusBadgeClass,
        formatDisplayStatus,
        renderStatusBadge
    };

    window.StockTrackFormat = Format;
    window.Format = Format;
})();
