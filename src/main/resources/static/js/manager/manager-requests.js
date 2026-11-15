/**
 * StockTrack - Manager Approval Requests Controller
 * Handles /manager/requests.html:
 * - KPI summary cards (/api/requests/kpi)
 * - Status tab filtering (PENDING, APPROVED, REJECTED, ALL)
 * - Live search and pagination (/api/requests)
 * - Detailed modal inspection with formatted payloads and decision actions (/api/requests/{id}/approve, /api/requests/{id}/reject)
 */
(function () {
    'use strict';

    // 1. Access Control
    const user = (window.Auth && window.Auth.getUser) ? window.Auth.getUser() : (window.api && window.api.getCurrentUser());
    if (!user || (user.role !== 'MANAGER' && user.role !== 'OWNER' && user.role !== 'ADMIN')) {
        window.location.replace('../index.html');
        return;
    }

    // 2. Helper Functions
    function createElement(tag, text, className) {
        const node = document.createElement(tag);
        if (text !== undefined && text !== null) {
            node.textContent = text;
        }
        if (className) {
            node.className = className;
        }
        return node;
    }

    function formatMoney(value) {
        return (Number(value) || 0).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function formatDateTime(isoStr) {
        if (!isoStr) return '-';
        if (window.Format && window.Format.formatDisplayDate) {
            return window.Format.formatDisplayDate(isoStr);
        }
        try {
            const d = new Date(isoStr);
            if (isNaN(d.getTime())) return isoStr;
            const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
            const day = String(d.getDate()).padStart(2, '0');
            const month = months[d.getMonth()] || String(d.getMonth() + 1).padStart(2, '0');
            const year = d.getFullYear();
            const hours = String(d.getHours()).padStart(2, '0');
            const mins = String(d.getMinutes()).padStart(2, '0');
            return `${day} ${month} ${year}, ${hours}:${mins}`;
        } catch (_) {
            return isoStr;
        }
    }

    function formatRequestType(type) {
        const map = {
            'PO_CREATE': 'Purchase Order (PO)',
            'SO_CREATE': 'Sales Order (SO)',
            'ROLE_CHANGE': 'Role Change',
            'USER_CREATE': 'User Creation',
            'PRODUCT_MOVE': 'Zone Transfer',
            'PRODUCT_UPDATE': 'Product Edit',
            'PRODUCT_CREATE': 'Product Create',
            'PRODUCT_DELETE': 'Product Deactivate',
            'ZONE_CREATE': 'Zone Create',
            'ZONE_UPDATE': 'Zone Edit',
            'SUPPLIER_CREATE': 'Supplier Create',
            'SUPPLIER_UPDATE': 'Supplier Edit',
            'SUPPLIER_DEACTIVATE': 'Supplier Deactivate',
            'CUSTOMER_CREATE': 'Customer Create',
            'CUSTOMER_UPDATE': 'Customer Edit',
            'CUSTOMER_DEACTIVATE': 'Customer Deactivate'
        };
        return map[type] || type || '-';
    }

    function formatPayloadDetails(r) {
        if (!r.payload) return null;
        try {
            const p = typeof r.payload === 'object' ? r.payload : JSON.parse(r.payload);
            const lines = [];

            if (r.requestType === 'USER_CREATE') {
                if (p.fullName) lines.push(`Full Name: ${p.fullName}`);
                if (p.userId) lines.push(`User ID: ${p.userId}`);
                if (p.role) lines.push(`Assigned Role: ${p.role}`);
                if (p.reason) lines.push(`Reason: ${p.reason}`);
            } else if (r.requestType === 'ROLE_CHANGE') {
                if (p.userId) lines.push(`User ID: ${p.userId}`);
                if (p.oldRole && p.newRole) lines.push(`Role Change: ${p.oldRole} → ${p.newRole}`);
                if (p.reason) lines.push(`Reason: ${p.reason}`);
            } else if (r.requestType === 'PRODUCT_UPDATE') {
                if (p.productId) lines.push(`Product ID: #${p.productId}`);
                if (p.productName) lines.push(`Product Name: ${p.productName}`);
                if (p.sellingPrice !== undefined) lines.push(`Selling Price: ฿${formatMoney(p.sellingPrice)}`);
                if (p.defaultCost !== undefined) lines.push(`Default Cost: ฿${formatMoney(p.defaultCost)}`);
                if (p.currentStock !== undefined) lines.push(`Stock: ${p.currentStock}`);
                if (p.minStockAlert !== undefined) lines.push(`Min Alert: ${p.minStockAlert}`);
            } else if (r.requestType === 'PRODUCT_MOVE') {
                if (p.operation) lines.push(`Operation: ${p.operation}`);
                if (p.sourceZoneCode && p.targetZoneCode) lines.push(`Transfer: Zone ${p.sourceZoneCode} → Zone ${p.targetZoneCode}`);
                if (p.reason) lines.push(`Reason: ${p.reason}`);
            } else if (r.requestType === 'PO_CREATE' || r.requestType === 'SO_CREATE') {
                if (p.supplierName) lines.push(`Supplier: ${p.supplierName}`);
                if (p.customerName) lines.push(`Customer: ${p.customerName}`);
                if (p.totalAmount) lines.push(`Total Amount: ฿${formatMoney(p.totalAmount)}`);
                if (Array.isArray(p.items) && p.items.length) {
                    lines.push(`Items (${p.items.length}): ` + p.items.map(it => `${it.productName || it.productId} (x${it.quantity})`).join(', '));
                }
            } else if (r.requestType === 'ZONE_CREATE' || r.requestType === 'ZONE_UPDATE') {
                if (p.zoneCode) lines.push(`Zone Code: ${p.zoneCode}`);
                if (p.zoneName) lines.push(`Zone Name: ${p.zoneName}`);
                if (p.capacity !== undefined) {
                    const capText = r.requestType === 'ZONE_UPDATE'
                        ? `New Capacity: ${Number(p.capacity).toLocaleString()} Units` + (p.oldCapacity ? ` (was ${Number(p.oldCapacity).toLocaleString()} Units)` : '')
                        : `Capacity: ${Number(p.capacity).toLocaleString()} Units`;
                    lines.push(capText);
                }
                if (p.reason) lines.push(`Reason: ${p.reason}`);
            } else if (r.requestType?.startsWith('SUPPLIER_')) {
                if (p.companyName) lines.push(`Supplier: ${p.companyName}`);
                if (p.category) lines.push(`Category: ${p.category}`);
                if (p.zoneCode) lines.push(`Zone: ${p.zoneCode}`);
            } else if (r.requestType?.startsWith('CUSTOMER_')) {
                if (p.customerName) lines.push(`Customer: ${p.customerName}`);
            }

            return lines.length ? lines.join('\n') : null;
        } catch (_) {
            return null;
        }
    }

    // Modal Helpers
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';

        const focusable = modal.querySelector('input:not([disabled]), button:not([disabled]), select:not([disabled]), textarea:not([disabled])');
        if (focusable) {
            setTimeout(() => focusable.focus(), 50);
        }
    }

    function closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    // Bind modal backdrop click and close buttons
    document.addEventListener('click', event => {
        const target = event.target;
        if (target.classList.contains('modal-backdrop')) {
            target.classList.remove('active');
            document.body.style.overflow = '';
        }
        if (target.classList.contains('modal-close-btn') || target.id === 'manager-request-close') {
            closeModal('modal-request-detail');
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            closeModal('modal-request-detail');
        }
    });

    // 3. Dynamic Sidebar Navigation Extension (Fallback)
    function initManagerNavigation() {
        const container = document.getElementById('sidebar-container');
        if (!container || container.dataset.role !== 'MANAGER') return;

        const mount = () => {
            const nav = container.querySelector('#sidebar-nav-list');
            const dashboard = nav?.querySelector('[data-nav-id="dashboard"]');
            if (!dashboard) return;

            dashboard.href = 'dashboard.html';
            if (nav.querySelector('[data-nav-id="requests"]')) return;

            const link = dashboard.cloneNode(true);
            link.href = 'requests.html';
            link.dataset.navId = 'requests';
            link.dataset.tooltip = 'View Requests';
            link.classList.toggle('active', container.dataset.activeMenu === 'requests');
            link.querySelectorAll('span').forEach(span => { span.textContent = 'View Requests'; });
            link.setAttribute('aria-label', 'View Requests');

            const icon = link.querySelector('.sidebar-icon-box');
            if (icon) {
                icon.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><path d="M8 13h8M8 17h5"></path></svg>';
            }
            nav.append(link);
        };

        new MutationObserver(mount).observe(container, { childList: true, subtree: true });
        mount();
    }

    // 4. Requests Controller
    function initRequests() {
        const body = document.getElementById('requests-table-body');
        if (!body) return;

        const empty = body.innerHTML;
        const reason = document.getElementById('manager-decision-reason');
        const message = document.getElementById('manager-request-message');
        const searchInput = document.getElementById('requests-search-input');
        const tabs = document.getElementById('requests-tabs');
        const prevBtn = document.getElementById('btn-requests-prev');
        const nextBtn = document.getElementById('btn-requests-next');
        const pageNumBtn = document.getElementById('btn-page-num');
        const pageInfo = document.getElementById('requests-pagination-info');
        const decisionForm = document.getElementById('manager-decision-form');

        let requests = [];
        let currentStatus = 'PENDING';
        let page = 1;
        let selected = null;
        const size = 10;

        async function loadKpis() {
            try {
                const res = await window.api.get('/api/requests/kpi');
                if (res && res.data) {
                    const k = res.data;
                    const map = {
                        total: k.total || 0,
                        pending: k.pending || 0,
                        approved: k.approved || 0,
                        rejected: k.rejected || 0
                    };
                    for (const [id, val] of Object.entries(map)) {
                        const el = document.getElementById('kpi-req-' + id);
                        if (el) el.textContent = val.toLocaleString();
                    }
                }
            } catch (err) {
                console.error('Failed to load request KPIs:', err);
            }
        }

        async function loadRequests() {
            const query = (searchInput?.value || '').trim();
            let url = '/api/requests?status=' + encodeURIComponent(currentStatus);
            if (query) {
                url += '&search=' + encodeURIComponent(query);
            }

            try {
                const res = await window.api.get(url);
                requests = (res && Array.isArray(res.data)) ? res.data : [];
                renderRequests();
            } catch (err) {
                console.error('Failed to load requests:', err);
                body.replaceChildren();
                const errRow = createElement('tr');
                const errCell = createElement('td', 'Failed to load requests from server: ' + (err.message || ''));
                errCell.colSpan = 6;
                errCell.className = 'text-center py-4 text-red-600';
                errRow.appendChild(errCell);
                body.appendChild(errRow);
            }
        }

        function renderRequests() {
            const filtered = requests;
            const totalPages = Math.max(1, Math.ceil(filtered.length / size));
            page = Math.max(1, Math.min(page, totalPages));

            body.replaceChildren();

            const pageItems = filtered.slice((page - 1) * size, page * size);
            for (const r of pageItems) {
                const row = document.createElement('tr');

                // 1. Request ID (#ID)
                const reqIdStr = 'REQ-' + String(r.requestId).padStart(4, '0') + ` (#${r.requestId})`;
                const reqIdTd = createElement('td', reqIdStr, 'cell-id');
                row.appendChild(reqIdTd);

                // 2. Request Type
                const typeStr = formatRequestType(r.requestType);
                const typeTd = createElement('td', null, 'cell-medium');
                const typeMain = createElement('div', typeStr);
                typeTd.appendChild(typeMain);
                row.appendChild(typeTd);

                // 3. Requested By
                const requesterStr = r.requestedByName || ('User #' + (r.requestedByUserId || '-'));
                const reqByTd = createElement('td', requesterStr);
                row.appendChild(reqByTd);

                // 4. Date & Time
                const dateStr = formatDateTime(r.requestedDatetime);
                const dateTd = createElement('td', dateStr);
                row.appendChild(dateTd);

                // 5. Status Badge (Unified with Admin)
                const badgeTd = createElement('td', null, 'text-center');
                const displayStatus = window.Format ? window.Format.formatDisplayStatus(r.status) : (r.status ? (r.status.charAt(0).toUpperCase() + r.status.slice(1).toLowerCase()) : 'Pending');
                const badgeClass = window.Format ? window.Format.getStatusBadgeClass(r.status) : `badge-${(r.status || 'pending').toLowerCase()}`;
                const badge = createElement(
                    'span',
                    displayStatus,
                    `badge-status ${badgeClass}`
                );
                badgeTd.appendChild(badge);
                row.appendChild(badgeTd);

                // 6. Action Button (Unified with Admin)
                const actionTd = createElement('td', null, 'text-center');
                const actionGroup = createElement('div', null, 'action-buttons-group');
                const btn = createElement('button', 'Detail', 'btn-action-detail');
                btn.type = 'button';
                btn.addEventListener('click', () => openDetail(r));
                actionGroup.appendChild(btn);

                const isRejected = String(r.status || '').toUpperCase() === 'REJECTED';
                if (isRejected && r.decisionReason) {
                    const reasonBtn = createElement('button', null, 'btn-action-reason');
                    reasonBtn.type = 'button';
                    reasonBtn.title = 'View reason for rejection';
                    reasonBtn.innerHTML = `
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        Reason
                    `;
                    reasonBtn.addEventListener('click', () => openDetail(r));
                    actionGroup.appendChild(reasonBtn);
                }

                actionTd.appendChild(actionGroup);
                row.appendChild(actionTd);

                body.appendChild(row);
            }

            if (!filtered.length) {
                body.innerHTML = empty;
            }

            // Pagination info & controls
            if (pageInfo) {
                pageInfo.textContent = filtered.length
                    ? `Showing ${(page - 1) * size + 1} to ${Math.min(page * size, filtered.length)} of ${filtered.length} entries`
                    : 'Showing 0 of 0 entries';
            }
            if (pageNumBtn) pageNumBtn.textContent = page;
            if (prevBtn) prevBtn.disabled = page <= 1;
            if (nextBtn) nextBtn.disabled = page * size >= filtered.length;
        }

        function openDetail(r) {
            selected = r;
            const detail = document.getElementById('manager-request-detail');
            if (detail) {
                detail.replaceChildren();

                const appendItem = (label, val, isFullWidth = false, isPayload = false) => {
                    const item = createElement('div', null, 'req-detail-item' + (isFullWidth ? ' full-width' : ''));
                    const lbl = createElement('span', label, 'req-detail-label');
                    item.appendChild(lbl);
                    if (isPayload) {
                        const box = createElement('div', val, 'req-detail-payload-box');
                        item.appendChild(box);
                    } else if (label === 'Current Status') {
                        const valueEl = createElement('div', null, 'req-detail-val');
                        const bClass = window.Format ? window.Format.getStatusBadgeClass(r.status) : `badge-${(r.status || 'pending').toLowerCase()}`;
                        const dStatus = window.Format ? window.Format.formatDisplayStatus(r.status) : (r.status || 'Pending');
                        const statusBadge = createElement('span', dStatus, `badge-status ${bClass}`);
                        valueEl.appendChild(statusBadge);
                        item.appendChild(valueEl);
                    } else {
                        const valueEl = createElement('div', val, 'req-detail-val');
                        item.appendChild(valueEl);
                    }
                    detail.appendChild(item);
                };

                const reqIdStr = 'REQ-' + String(r.requestId).padStart(4, '0') + ` (#${r.requestId})`;
                const typeStr = formatRequestType(r.requestType);
                const requesterStr = (r.requestedByName || ('User #' + r.requestedByUserId)) + (r.role ? ` (${r.role})` : '');
                const dateStr = formatDateTime(r.requestedDatetime);

                appendItem('Request ID', reqIdStr);
                appendItem('Request Type', typeStr);
                appendItem('Requested By', requesterStr);
                appendItem('Date & Time', dateStr);
                appendItem('Current Status', r.status || 'PENDING');
                appendItem('Summary Detail', r.detail || '-', true);

                const payloadSummary = formatPayloadDetails(r);
                if (payloadSummary) {
                    appendItem('Payload Details', payloadSummary, true, true);
                }

                if (r.status !== 'PENDING') {
                    appendItem('Decided By', r.decidedByName || ('User #' + (r.decidedByUserId || '-')));
                    appendItem('Decided At', formatDateTime(r.decidedDatetime));
                    if (r.decisionReason) {
                        appendItem('Decision Reason', r.decisionReason, true);
                    }
                }
            }

            if (reason) {
                reason.value = r.status === 'PENDING' ? '' : (r.decisionReason || '');
                reason.setCustomValidity('');
                reason.disabled = (r.status !== 'PENDING');
            }
            if (message) message.textContent = '';

            const rejectBtn = document.getElementById('btn-req-reject');
            const approveBtn = document.getElementById('btn-req-approve');
            const reasonGroup = document.getElementById('decision-reason-group');

            const isPending = (r.status === 'PENDING');
            if (rejectBtn) rejectBtn.hidden = !isPending;
            if (approveBtn) approveBtn.hidden = !isPending;
            if (reasonGroup) {
                reasonGroup.style.display = isPending ? 'flex' : 'none';
            }

            openModal('modal-request-detail');
        }

        if (reason) {
            reason.addEventListener('input', () => reason.setCustomValidity(''));
        }

        let lastClickedDecision = null;
        const rejectBtn = document.getElementById('btn-req-reject');
        const approveBtn = document.getElementById('btn-req-approve');
        if (rejectBtn) {
            rejectBtn.addEventListener('click', () => {
                lastClickedDecision = 'Rejected';
            });
        }
        if (approveBtn) {
            approveBtn.addEventListener('click', () => {
                lastClickedDecision = 'Approved';
                if (reason) reason.setCustomValidity('');
            });
        }

        if (decisionForm) {
            decisionForm.addEventListener('submit', async event => {
                event.preventDefault();
                if (!selected || selected.status !== 'PENDING') return;

                const decision = event.submitter?.value || lastClickedDecision;
                if (!['Approved', 'Rejected'].includes(decision)) return;

                const reasonText = reason ? reason.value.trim() : '';

                if (decision === 'Rejected' && !reasonText) {
                    if (reason) {
                        reason.setCustomValidity('Enter a reason for rejection.');
                        reason.reportValidity();
                    }
                    return;
                }

                const buttons = decisionForm.querySelectorAll('button');
                buttons.forEach(b => { b.disabled = true; });
                if (message) {
                    message.style.color = '#3b82f6';
                    message.textContent = 'Processing decision...';
                }

                try {
                    const endpoint = decision === 'Approved'
                        ? `/api/requests/${selected.requestId}/approve`
                        : `/api/requests/${selected.requestId}/reject`;

                    await window.api.post(endpoint, { reason: reasonText });

                    closeModal('modal-request-detail');
                    await Promise.all([loadKpis(), loadRequests()]);

                    if (window.UI && typeof window.UI.showToast === 'function') {
                        const actionWord = (decision === 'Approved') ? 'approved' : 'rejected';
                        window.UI.showToast(`Request #${selected.requestId} has been ${actionWord} successfully.`, 'success');
                    }
                } catch (err) {
                    console.error('Decision failed:', err);
                    if (message) {
                        message.style.color = '#dc2626';
                        message.textContent = err.message || 'Unable to save decision. Please try again.';
                    }
                    if (window.UI && typeof window.UI.showToast === 'function') {
                        window.UI.showToast(err.message || 'Unable to save decision. Please try again.', 'error');
                    }
                } finally {
                    buttons.forEach(b => { b.disabled = false; });
                }
            });
        }

        tabs?.addEventListener('click', event => {
            const tab = event.target.closest('.tab-pill');
            if (!tab) return;
            currentStatus = tab.dataset.status.toUpperCase();
            page = 1;
            tabs.querySelectorAll('.tab-pill').forEach(b => {
                const isActive = (b === tab);
                b.classList.toggle('active', isActive);
                b.setAttribute('aria-pressed', String(isActive));
            });
            loadRequests();
        });

        searchInput?.addEventListener('input', () => {
            page = 1;
            loadRequests();
        });

        prevBtn?.addEventListener('click', () => {
            if (page > 1) {
                page--;
                renderRequests();
            }
        });

        nextBtn?.addEventListener('click', () => {
            page++;
            renderRequests();
        });

        // Initialize Real Requests Data
        loadKpis();
        loadRequests();
    }

    function init() {
        initManagerNavigation();
        initRequests();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
