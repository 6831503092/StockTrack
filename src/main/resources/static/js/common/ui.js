/**
 * StockTrack - Common UI Component Helpers
 * Centralizes Toast notifications, Center Alerts, Confirm Dialogs, Modal lifecycle, and Loading states.
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

    function openModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (modal) {
            modal.classList.add('active');
            document.body.classList.add('modal-open');
        }
    }

    function closeModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (!modal) return;
        modal.classList.remove('active');
        if (!document.querySelector('.modal-overlay.active, .center-alert-overlay.active')) {
            document.body.classList.remove('modal-open');
        }
        const form = modal.querySelector('form');
        if (form) form.reset();
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
            <div class="toast-icon">${icons[toastType]}</div>
            <div class="toast-content">
                <div class="toast-title">${escapeHtml(displayTitle)}</div>
                <div class="toast-message">${escapeHtml(message)}</div>
            </div>
            <button type="button" class="toast-close-btn" aria-label="Close">&times;</button>
        `;

        const removeToast = () => {
            if (item.classList.contains('toast-hiding')) return;
            item.classList.add('toast-hiding');
            setTimeout(() => item.remove(), 320);
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
                <div class="center-alert-icon ${alertType}">${icons[alertType]}</div>
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

        requestAnimationFrame(() => {
            overlay.classList.add('active');
            overlay.querySelector('#btn-center-alert-ok')?.focus();
        });
    }

    function showConfirmDialog({
        title = 'Confirm Action',
        message = 'Are you sure you want to proceed?',
        confirmText = 'Confirm',
        cancelText = 'Cancel',
        type = 'danger'
    } = {}) {
        return new Promise((resolve) => {
            let overlay = document.getElementById('stocktrack-center-confirm');
            if (!overlay) {
                overlay = document.createElement('div');
                overlay.id = 'stocktrack-center-confirm';
                overlay.className = 'center-alert-overlay';
                document.body.appendChild(overlay);
            }

            const icons = {
                danger: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`,
                warning: `<svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`
            };

            const iconHtml = icons[type] || icons.danger;

            overlay.innerHTML = `
                <div class="center-alert-box">
                    <div class="center-alert-icon danger">${iconHtml}</div>
                    <h3 class="center-alert-title">${escapeHtml(title)}</h3>
                    <p class="center-alert-message">${escapeHtml(message)}</p>
                    <div class="center-alert-actions">
                        <button type="button" class="btn-cancel" id="btn-confirm-dialog-cancel">${escapeHtml(cancelText)}</button>
                        <button type="button" class="btn-confirm-danger" id="btn-confirm-dialog-ok">${escapeHtml(confirmText)}</button>
                    </div>
                </div>
            `;

            let resolved = false;
            const close = (result) => {
                if (resolved) return;
                resolved = true;
                overlay.classList.remove('active');
                resolve(result);
            };

            overlay.querySelector('#btn-confirm-dialog-ok')?.addEventListener('click', () => close(true));
            overlay.querySelector('#btn-confirm-dialog-cancel')?.addEventListener('click', () => close(false));
            overlay.onclick = (e) => {
                if (e.target === overlay) close(false);
            };

            requestAnimationFrame(() => {
                overlay.classList.add('active');
                overlay.querySelector('#btn-confirm-dialog-cancel')?.focus();
            });
        });
    }

    function setLoading(btn, isLoading, loadingText = 'Processing...') {
        if (!btn) return;
        if (isLoading) {
            btn.dataset.originalText = btn.innerHTML;
            btn.disabled = true;
            btn.classList.add('btn-loading');
            btn.innerHTML = `<span class="spinner"></span> ${escapeHtml(loadingText)}`;
        } else {
            if (btn.dataset.originalText) {
                btn.innerHTML = btn.dataset.originalText;
                delete btn.dataset.originalText;
            }
            btn.disabled = false;
            btn.classList.remove('btn-loading');
        }
    }

    // Delegated modal close listeners (backdrop, close buttons, escape key)
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
            const activeModal = document.querySelector('.modal-overlay.active');
            if (activeModal) {
                closeModal(activeModal);
            }
            const activeAlert = document.querySelector('.center-alert-overlay.active');
            if (activeAlert) {
                activeAlert.classList.remove('active');
            }
        }
    });

    const UI = {
        openModal,
        closeModal,
        showToast,
        showCenterAlert,
        showConfirmDialog,
        setLoading
    };

    window.StockTrackUI = UI;
    window.UI = UI;
    window.showToast = showToast;
    window.showCenterAlert = showCenterAlert;
    window.showConfirmDialog = showConfirmDialog;
})();
