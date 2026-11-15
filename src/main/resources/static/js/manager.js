/**
 * StockTrack - Manager Controller & Backend Integration
 * Consolidated script for Manager module:
 * - Access Control & User Session
 * - Dynamic Sidebar Navigation Extension (View Requests)
 * - Dashboard Controller (Connected to Backend REST APIs: /api/zones/summary, /api/zones, /api/zones/products, /api/products/{id})
 * - Requests Controller (Connected to Backend REST APIs: /api/requests, /api/requests/kpi, /api/requests/{id}/approve, /api/requests/{id}/reject)
 * - UI & Design System Harmonized with Admin Module
 */
(function () {
    'use strict';

    // ==========================================
    // 1. ACCESS CONTROL & USER SESSION
    // ==========================================
    function getCurrentUser() {
        if (typeof window.api?.getCurrentUser === 'function') {
            return window.api.getCurrentUser();
        }
        try {
            const raw = localStorage.getItem('stocktrack_user');
            return raw ? JSON.parse(raw) : null;
        } catch (_) {
            return null;
        }
    }

    const user = getCurrentUser();
    if (!user || (user.role !== 'MANAGER' && user.role !== 'OWNER' && user.role !== 'ADMIN')) {
        window.location.replace('../index.html');
        return;
    }

    // ==========================================
    // 2. HELPER FUNCTIONS & MODAL SYSTEM
    // ==========================================
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
        try {
            const d = new Date(isoStr);
            if (isNaN(d.getTime())) return isoStr;
            return d.toLocaleDateString('en-GB') + ' ' + d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
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
            } else if (r.requestType === 'SUPPLIER_CREATE' || r.requestType === 'SUPPLIER_UPDATE' || r.requestType === 'SUPPLIER_DEACTIVATE') {
                const opLabel = r.requestType === 'SUPPLIER_CREATE' ? 'Request to Add New Supplier'
                    : r.requestType === 'SUPPLIER_UPDATE' ? 'Request to Edit Supplier' : 'Request to Delete Supplier';
                lines.push(`Action: ${opLabel}`);
                if (p.supplierId) lines.push(`Supplier ID: #${p.supplierId}`);
                if (p.companyName) {
                    const oldName = p.oldCompanyName && p.oldCompanyName !== p.companyName ? ` (was: ${p.oldCompanyName})` : '';
                    lines.push(`Company Name: ${p.companyName}${oldName}`);
                }
                if (p.category !== undefined) {
                    const catVal = p.category || '-';
                    const oldCat = p.oldCategory && p.oldCategory !== p.category ? ` (was: ${p.oldCategory})` : '';
                    lines.push(`Products: ${catVal}${oldCat}`);
                }
                if (p.zoneCode !== undefined) {
                    const zoneVal = p.zoneCode ? `Zone ${p.zoneCode}` : 'None';
                    const oldZone = p.oldZoneCode && p.oldZoneCode !== p.zoneCode ? ` (was: Zone ${p.oldZoneCode})` : '';
                    lines.push(`Assigned Zone: ${zoneVal}${oldZone}`);
                }
                if (p.reason) lines.push(`Detail: ${p.reason}`);
            } else if (r.requestType === 'CUSTOMER_CREATE' || r.requestType === 'CUSTOMER_UPDATE' || r.requestType === 'CUSTOMER_DEACTIVATE') {
                const opLabel = r.requestType === 'CUSTOMER_CREATE' ? 'Request to Add New Customer'
                    : r.requestType === 'CUSTOMER_UPDATE' ? 'Request to Edit Customer' : 'Request to Delete Customer';
                lines.push(`Action: ${opLabel}`);
                if (p.customerId) lines.push(`Customer ID: #${p.customerId}`);
                if (p.customerName) {
                    const oldName = p.oldCustomerName && p.oldCustomerName !== p.customerName ? ` (was: ${p.oldCustomerName})` : '';
                    lines.push(`Customer Name: ${p.customerName}${oldName}`);
                }
                if (p.reason) lines.push(`Detail: ${p.reason}`);
            } else if (r.requestType === 'ZONE_CREATE' || r.requestType === 'ZONE_UPDATE') {
                const isCreate = r.requestType === 'ZONE_CREATE';
                const opLabel = isCreate ? 'Request to Create New Zone' : 'Request to Edit Zone Capacity';
                lines.push(`Action: ${opLabel}`);
                if (p.zoneCode) lines.push(`Zone Code: ${p.zoneCode}`);
                if (p.zoneName) {
                    const oldName = p.oldZoneName && p.oldZoneName !== p.zoneName ? ` (was: ${p.oldZoneName})` : '';
                    lines.push(`Zone Name: ${p.zoneName}${oldName}`);
                }
                if (p.capacity !== undefined) {
                    const oldCap = p.oldCapacity !== undefined && p.oldCapacity !== p.capacity
                        ? ` (was: ${Number(p.oldCapacity).toLocaleString()} Units)`
                        : '';
                    lines.push(`Requested Capacity: ${Number(p.capacity).toLocaleString()} Units${oldCap}`);
                }
                if (!isCreate && p.storedUnits !== undefined) {
                    lines.push(`Currently Stored: ${Number(p.storedUnits).toLocaleString()} Units`);
                }
                if (p.reason) lines.push(`Detail: ${p.reason}`);
            } else {
                for (const [k, v] of Object.entries(p)) {
                    if (k === 'passwordHash' || k === 'password') continue;
                    if (typeof v !== 'object' && v !== null && v !== '') {
                        lines.push(`${k}: ${v}`);
                    }
                }
            }
            return lines.length ? lines.join('\n') : null;
        } catch (_) {
            return null;
        }
    }

    // Modal Lifecycle Helpers (Aligned with Admin App)
    function openModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (modal) {
            modal.classList.add('active');
        }
    }

    function closeModal(modalOrId) {
        const modal = typeof modalOrId === 'string'
            ? document.getElementById(modalOrId)
            : modalOrId;
        if (!modal) return;
        modal.classList.remove('active');
    }

    // Delegated Global Listeners for Modal Closing
    document.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal-overlay')) {
            closeModal(e.target);
            return;
        }
        const closeBtn = e.target.closest('.btn-close-modal, [data-close-modal], .btn-edit-cancel');
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

    // ==========================================
    // 3. NAVIGATION EXTENSION
    // ==========================================
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

    // ==========================================
    // 4. DASHBOARD CONTROLLER (dashboard.html)
    // ==========================================
    function initDashboard() {
        const tbody = document.getElementById('manager-product-rows');
        const bars = document.getElementById('manager-bars');
        if (!tbody || !bars) return;

        const search = document.getElementById('manager-search');
        const zone = document.getElementById('manager-zone');
        const chartZone = document.getElementById('manager-chart-zone');
        const chartCountPill = document.getElementById('chart-product-count');
        const form = document.getElementById('manager-edit-form');
        const feedback = document.getElementById('manager-feedback');
        const totalEl = document.getElementById('manager-total');
        const lowEl = document.getElementById('manager-low');
        const pendingEl = document.getElementById('manager-pending');
        const warehouseEl = document.getElementById('manager-warehouse');
        const axisTop = document.getElementById('manager-axis-top');
        const axis75 = document.getElementById('manager-axis-75');
        const axisMid = document.getElementById('manager-axis-mid');
        const axis25 = document.getElementById('manager-axis-25');
        const axis0 = document.getElementById('manager-axis-0');
        const chartWrapper = document.querySelector('.manager-chart');
        const chartSubtitle = document.getElementById('chart-subtitle');

        let products = [];
        let editingId = null;

        function openEditProductModal(product) {
            editingId = product.id;
            const idInput = document.getElementById('manager-edit-id');
            if (idInput) idInput.value = '#' + product.id;

            if (form) {
                if (form.elements.productName) form.elements.productName.value = product.name || '';
                if (form.elements.cost) form.elements.cost.value = product.defaultCost ?? 0;
                if (form.elements.price) form.elements.price.value = product.sellingPrice ?? 0;
                if (form.elements.stock) form.elements.stock.value = product.stock ?? 0;
                if (form.elements.minimum) form.elements.minimum.value = product.minStockAlert ?? 20;

                const err = document.getElementById('manager-edit-error');
                if (err) err.textContent = '';
            }
            openModal('modal-edit-product');
        }

        async function loadZones() {
            try {
                const res = await window.api.get('/api/zones');
                if (res && res.data) {
                    const currentVal = zone?.value || chartZone?.value || 'all';

                    const populate = (el) => {
                        if (!el) return;
                        el.replaceChildren();

                        const optAll = document.createElement('option');
                        optAll.value = 'all';
                        optAll.textContent = 'All zones';
                        el.appendChild(optAll);

                        for (const z of res.data) {
                            const opt = document.createElement('option');
                            opt.value = z.zoneCode;
                            opt.textContent = z.zoneName || ('Zone ' + z.zoneCode);
                            el.appendChild(opt);
                        }
                        el.value = currentVal;
                    };

                    populate(zone);
                    populate(chartZone);
                }
            } catch (err) {
                console.error('Failed to load zones:', err);
            }
        }

        async function loadSummary() {
            try {
                const [summaryRes, kpiRes] = await Promise.all([
                    window.api.get('/api/zones/summary'),
                    window.api.get('/api/requests/kpi')
                ]);

                if (summaryRes && summaryRes.data) {
                    const s = summaryRes.data;
                    if (totalEl) totalEl.textContent = (s.storedUnits || 0).toLocaleString();
                    if (lowEl) lowEl.textContent = (s.lowStockCount || 0).toLocaleString();
                    if (warehouseEl) warehouseEl.textContent = (s.activeZonesCount || 0).toLocaleString();
                }

                if (kpiRes && kpiRes.data) {
                    if (pendingEl) pendingEl.textContent = (kpiRes.data.pending || 0).toLocaleString();
                }
            } catch (err) {
                console.error('Failed to load dashboard summary:', err);
            }
        }

        async function loadProducts() {
            const query = (search ? search.value.trim() : '');
            const zoneVal = (zone ? zone.value : (chartZone ? chartZone.value : 'all'));

            let url = '/api/zones/products';
            const params = [];
            if (zoneVal && zoneVal !== 'all') {
                params.push('zone=' + encodeURIComponent(zoneVal));
            }
            if (query) {
                params.push('search=' + encodeURIComponent(query));
            }
            if (params.length) {
                url += '?' + params.join('&');
            }

            try {
                const res = await window.api.get(url);
                products = (res && Array.isArray(res.data)) ? res.data : [];
                renderProductTableAndChart();
            } catch (err) {
                console.error('Failed to load products:', err);
                if (feedback) {
                    feedback.className = 'show-error';
                    feedback.textContent = 'Error loading products from server.';
                }
            }
        }

        function renderProductTableAndChart() {
            tbody.replaceChildren();
            bars.replaceChildren();

            if (chartCountPill) {
                chartCountPill.textContent = products.length + (products.length === 1 ? ' item' : ' items');
            }

            const maximum = Math.max(100, ...products.map(p => p.stock || 0));
            if (axisTop) axisTop.textContent = maximum.toLocaleString();
            if (axis75) axis75.textContent = Math.round(maximum * 0.75).toLocaleString();
            if (axisMid) axisMid.textContent = Math.round(maximum * 0.5).toLocaleString();
            if (axis25) axis25.textContent = Math.round(maximum * 0.25).toLocaleString();
            if (axis0) axis0.textContent = '0';

            if (!products.length) {
                const emptyRow = createElement('tr');
                const emptyCell = createElement('td');
                emptyCell.colSpan = 7;
                emptyCell.className = 'table-empty-state';
                emptyCell.innerHTML = `
                    <div class="empty-state-wrap">
                        <div class="empty-state-icon-circle">
                            <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </div>
                        <p>No products found</p>
                        <span class="empty-subtitle">No products match your search or zone filter.</span>
                    </div>
                `;
                emptyRow.appendChild(emptyCell);
                tbody.appendChild(emptyRow);

                const chartEmpty = createElement('div', null, 'chart-empty-state');
                chartEmpty.innerHTML = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom: 6px;">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="8" y1="12" x2="16" y2="12"></line>
                    </svg>
                    <span style="font-size: 0.85rem; font-weight: 600; color: #64748b;">No products to display in chart</span>
                `;
                bars.appendChild(chartEmpty);

                if (chartWrapper) {
                    chartWrapper.setAttribute('aria-label', 'No matching products');
                }
                return;
            }

            for (let idx = 0; idx < products.length; idx++) {
                const product = products[idx];
                const isLow = (Number(product.stock) || 0) <= (Number(product.minStockAlert) || 20);
                const zoneLabel = product.zoneName || (product.location ? 'Zone ' + product.location : '-');

                // --- 1. TABLE ROW ---
                const row = createElement('tr');

                // 1.1 Product ID (#ID)
                const idTd = createElement('td', '#' + product.id, 'cell-id');
                row.appendChild(idTd);

                // 1.2 Product Name & Zone Subtitle
                const nameTd = createElement('td', null, 'cell-medium');
                const titleDiv = createElement('div', product.name);
                titleDiv.style.fontWeight = '600';
                titleDiv.style.color = '#0f172a';
                nameTd.appendChild(titleDiv);

                const subtextDiv = createElement('div', zoneLabel, 'cell-subtext');
                nameTd.appendChild(subtextDiv);
                row.appendChild(nameTd);

                // 1.3 Default Cost
                const costTd = createElement('td', '฿' + formatMoney(product.defaultCost));
                row.appendChild(costTd);

                // 1.4 Selling Price
                const priceTd = createElement('td', '฿' + formatMoney(product.sellingPrice));
                row.appendChild(priceTd);

                // 1.5 Current Stock (Admin Badge Style)
                const stockTd = createElement('td');
                const badgeClass = isLow ? 'badge-low-stock' : 'badge-in-stock';
                const stockBadge = createElement(
                    'span',
                    `${(Number(product.stock) || 0).toLocaleString()} Units`,
                    `badge-status ${badgeClass}`
                );
                stockTd.appendChild(stockBadge);
                row.appendChild(stockTd);

                // 1.6 Min Alert
                const minTd = createElement('td', `${(Number(product.minStockAlert) || 20).toLocaleString()} Units`);
                row.appendChild(minTd);

                // 1.7 Edit Action Button
                const actionTd = createElement('td', null, 'text-center');
                const editBtn = createElement('button', null, 'btn-action-edit');
                editBtn.type = 'button';
                editBtn.innerHTML = `
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                    <span>Edit</span>
                `;
                editBtn.setAttribute('aria-label', 'Edit ' + product.name);
                editBtn.addEventListener('click', () => {
                    openEditProductModal(product);
                });

                actionTd.appendChild(editBtn);
                row.appendChild(actionTd);
                tbody.appendChild(row);

                // --- 2. CHART BAR ---
                const column = createElement('div', undefined, 'manager-bar-column');
                column.setAttribute('title', `Click to edit ${product.name}`);

                const bar = createElement('div', undefined, 'manager-bar');
                const heightPx = Math.max(6, Math.round((product.stock / maximum) * 220));
                bar.style.height = heightPx + 'px';
                if (isLow) {
                    bar.classList.add('low-stock');
                }
                bar.appendChild(createElement('strong', product.stock));
                column.appendChild(bar);

                // Multi-line non-truncated product label
                const nameParts = (product.name || '').trim().split(/\s+/);
                let labelHtml = '';
                if (nameParts.length > 1) {
                    labelHtml = `<span class="label-line-1">${nameParts[0]}</span><span class="label-line-2">${nameParts.slice(1).join(' ')}</span>`;
                } else {
                    labelHtml = `<span class="label-line-1">${product.name}</span>`;
                }
                const labelSpan = createElement('div', undefined, 'manager-bar-label');
                labelSpan.innerHTML = labelHtml;
                labelSpan.title = product.name;
                column.appendChild(labelSpan);

                // Tooltip Placement: Always inside chart area (pos-inside) to never overlap .card-header-flex
                const posClass = 'pos-inside';
                let alignClass = 'align-center';
                if (idx < 2) {
                    alignClass = 'align-left';
                } else if (idx >= products.length - 2) {
                    alignClass = 'align-right';
                }

                const tooltip = createElement('div', undefined, `manager-bar-tooltip ${posClass} ${alignClass}`);
                tooltip.innerHTML = `
                    <div class="bar-tooltip-title">${product.name}</div>
                    <div class="bar-tooltip-meta">Zone ${product.location || '-'} • Product #${product.id}</div>
                    <div class="bar-tooltip-divider"></div>
                    <div class="bar-tooltip-row">
                        <span>Available Stock:</span>
                        <strong class="${isLow ? 'text-red' : 'text-blue'}">${product.stock} Units</strong>
                    </div>
                    <div class="bar-tooltip-row">
                        <span>Status:</span>
                        <span class="${isLow ? 'text-red' : 'text-mint'} font-bold">${isLow ? 'Low Stock Alert' : 'In Stock'}</span>
                    </div>
                    <div class="bar-tooltip-row">
                        <span>Min Alert Threshold:</span>
                        <span>${product.minStockAlert} Units</span>
                    </div>
                    <div class="bar-tooltip-row">
                        <span>Selling Price:</span>
                        <span>฿${formatMoney(product.sellingPrice)}</span>
                    </div>
                `;
                column.appendChild(tooltip);

                // Live Inspector update in card header on hover
                column.addEventListener('mouseenter', () => {
                    if (chartSubtitle) {
                        chartSubtitle.innerHTML = `<span style="color: #0f172a; font-weight: 700;">${product.name}</span> (#${product.id}) &nbsp;•&nbsp; Stock: <strong style="color: ${isLow ? '#ef4444' : '#2563eb'}">${product.stock} Units</strong> (${isLow ? 'Low Stock' : 'In Stock'}) &nbsp;•&nbsp; Min Alert: ${product.minStockAlert} &nbsp;•&nbsp; Price: ฿${formatMoney(product.sellingPrice)} &nbsp;•&nbsp; ${zoneLabel}`;
                    }
                });
                column.addEventListener('mouseleave', () => {
                    if (chartSubtitle) {
                        chartSubtitle.textContent = 'Current available stock · Units (Hover over bars to inspect details)';
                    }
                });

                // Quick click on chart bar also opens edit modal
                column.addEventListener('click', () => {
                    openEditProductModal(product);
                });

                bars.appendChild(column);
            }

            if (chartWrapper) {
                chartWrapper.setAttribute(
                    'aria-label',
                    products.map(p => p.name + ': ' + p.stock + ' units').join(', ')
                );
            }
        }

        // Form Submission (Edit Product in backend database)
        if (form) {
            form.elements.productName?.addEventListener('input', () => {
                form.elements.productName.setCustomValidity('');
            });

            form.addEventListener('submit', async event => {
                event.preventDefault();
                const name = form.elements.productName.value.trim();
                if (!name) {
                    form.elements.productName.setCustomValidity('Enter a product name.');
                    form.reportValidity();
                    return;
                }

                const submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) submitBtn.disabled = true;

                try {
                    const updatePayload = {
                        productId: editingId,
                        productName: name,
                        defaultCost: Number(form.elements.cost.value),
                        sellingPrice: Number(form.elements.price.value),
                        currentStock: Number(form.elements.stock.value),
                        minStockAlert: Number(form.elements.minimum.value)
                    };

                    await window.api.put('/api/products/' + editingId, updatePayload);

                    closeModal('modal-edit-product');
                    if (feedback) {
                        feedback.className = 'show-success';
                        feedback.textContent = 'Product #' + editingId + ' updated successfully.';
                        setTimeout(() => {
                            if (feedback) {
                                feedback.className = '';
                                feedback.textContent = '';
                            }
                        }, 4000);
                    }

                    await Promise.all([loadSummary(), loadProducts()]);
                } catch (err) {
                    const errEl = document.getElementById('manager-edit-error');
                    if (errEl) {
                        errEl.textContent = err.message || 'Failed to save product changes.';
                    }
                } finally {
                    if (submitBtn) submitBtn.disabled = false;
                }
            });
        }

        search?.addEventListener('input', () => {
            loadProducts();
        });

        zone?.addEventListener('change', () => {
            if (chartZone) chartZone.value = zone.value;
            loadProducts();
        });

        chartZone?.addEventListener('change', () => {
            if (zone) zone.value = chartZone.value;
            loadProducts();
        });

        // Initialize Real Dashboard Data
        loadZones();
        loadSummary();
        loadProducts();
    }

    // ==========================================
    // 5. REQUESTS CONTROLLER (requests.html)
    // ==========================================
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

                // 1. Request ID (Admin styled centered cell-id)
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

                // 5. Status Badge (Admin styled)
                const badgeTd = createElement('td', null, 'text-center');
                const statusLower = (r.status || 'pending').toLowerCase();
                const badge = createElement(
                    'span',
                    r.status || 'PENDING',
                    `badge-status badge-${statusLower}`
                );
                badgeTd.appendChild(badge);
                row.appendChild(badgeTd);

                // 6. Action Button
                const actionTd = createElement('td', null, 'text-center');
                const btn = createElement('button', 'Detail', 'btn-action-detail');
                btn.type = 'button';
                btn.addEventListener('click', () => openDetail(r));
                actionTd.appendChild(btn);
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

        if (decisionForm) {
            decisionForm.addEventListener('submit', async event => {
                event.preventDefault();
                if (!selected || selected.status !== 'PENDING') return;

                const decision = event.submitter?.value;
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
                } catch (err) {
                    console.error('Decision failed:', err);
                    if (message) {
                        message.style.color = '#dc2626';
                        message.textContent = err.message || 'Unable to save decision. Please try again.';
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

    // ==========================================
    // 6. INITIALIZATION
    // ==========================================
    function init() {
        initManagerNavigation();
        initDashboard();
        initRequests();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
