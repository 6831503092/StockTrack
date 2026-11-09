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

    function getStatusBadgeClass(status) {
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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initImageFallbacks);
    } else {
        initImageFallbacks();
    }

    window.AdminApp = {
        checkAdminAccess,
        getStatusBadgeClass,
        formatMoney,
        getTodayISO,
        openModal,
        closeModal
    };
})();
