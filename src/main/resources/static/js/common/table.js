/**
 * StockTrack - Common Table Utilities
 * Provides table searching, filtering, empty states, and pagination count helpers.
 */
(function () {
    const escapeHtml = (window.Format && window.Format.escapeHtml) || function (str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    };

    function renderEmptyTable(tbodyOrSelector, message = 'No records found', colspan = 7) {
        const tbody = typeof tbodyOrSelector === 'string'
            ? document.querySelector(tbodyOrSelector)
            : tbodyOrSelector;
        if (!tbody) return;

        tbody.innerHTML = `
            <tr>
                <td colspan="${colspan}" class="table-empty-cell" style="text-align: center; padding: 48px 16px; color: #94a3b8; font-size: 0.95rem;">
                    <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" style="color: #cbd5e1;">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <span>${escapeHtml(message)}</span>
                    </div>
                </td>
            </tr>
        `;
    }

    function updatePaginationInfo(containerOrId, visibleCount, totalCount, label = 'entries') {
        const el = typeof containerOrId === 'string'
            ? document.getElementById(containerOrId)
            : containerOrId;
        if (!el) return;
        el.textContent = `Showing ${visibleCount} of ${totalCount} ${label}`;
    }

    function filterRows({
        rowSelector,
        searchQuery = '',
        searchAttrs = [],
        filterValue = 'ALL',
        filterAttr = null,
        paginationInfoId = null
    } = {}) {
        const rows = document.querySelectorAll(rowSelector);
        if (!rows.length) {
            if (paginationInfoId) updatePaginationInfo(paginationInfoId, 0, 0);
            return 0;
        }

        const q = searchQuery ? searchQuery.toLowerCase().trim() : '';
        const fVal = filterValue ? filterValue.trim().toUpperCase() : 'ALL';
        let visibleCount = 0;

        rows.forEach(row => {
            let matchesSearch = true;
            if (q) {
                if (searchAttrs.length > 0) {
                    matchesSearch = searchAttrs.some(attr => {
                        const val = (row.getAttribute(attr) || '').toLowerCase();
                        return val.includes(q);
                    });
                } else {
                    matchesSearch = (row.textContent || '').toLowerCase().includes(q);
                }
            }

            let matchesFilter = true;
            if (fVal && fVal !== 'ALL' && filterAttr) {
                const rowFilterVal = (row.getAttribute(filterAttr) || '').trim().toUpperCase();
                matchesFilter = (rowFilterVal === fVal);
            }

            const visible = matchesSearch && matchesFilter;
            row.style.display = visible ? '' : 'none';
            if (visible) visibleCount++;
        });

        if (paginationInfoId) {
            updatePaginationInfo(paginationInfoId, visibleCount, rows.length);
        }

        return visibleCount;
    }

    function debounce(fn, delay = 250) {
        let timer = null;
        return function (...args) {
            if (timer) clearTimeout(timer);
            timer = setTimeout(() => {
                fn.apply(this, args);
            }, delay);
        };
    }

    const Table = {
        renderEmptyTable,
        updatePaginationInfo,
        filterRows,
        debounce
    };

    window.StockTrackTable = Table;
    window.Table = Table;
})();
