/**
 * StockTrack - Manager Dashboard Controller
 * Handles /manager/dashboard.html:
 * - KPI summary cards (/api/zones/summary, /api/requests/kpi)
 * - Interactive stock chart by zone (/api/zones, /api/zones/products)
 * - Product inventory table with search, zone filtering, and inline product edit modal (/api/products/{id})
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

    // Modal helpers with Escape key support
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

    // Bind modal close buttons and background click
    document.addEventListener('click', event => {
        const target = event.target;
        if (target.classList.contains('modal-backdrop')) {
            target.classList.remove('active');
            document.body.style.overflow = '';
        }
        if (target.classList.contains('modal-close-btn') || target.id === 'manager-edit-cancel') {
            closeModal('modal-edit-product');
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            closeModal('modal-edit-product');
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

    // 4. Dashboard Controller
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

                // --- 1. Table Row ---
                const row = createElement('tr');

                const idTd = createElement('td', '#' + product.id, 'cell-id');
                row.appendChild(idTd);

                const nameTd = createElement('td', null, 'cell-medium');
                const titleDiv = createElement('div', product.name);
                titleDiv.style.fontWeight = '600';
                titleDiv.style.color = '#0f172a';
                nameTd.appendChild(titleDiv);

                const subtextDiv = createElement('div', zoneLabel, 'cell-subtext');
                nameTd.appendChild(subtextDiv);
                row.appendChild(nameTd);

                const costTd = createElement('td', '฿' + formatMoney(product.defaultCost));
                row.appendChild(costTd);

                const priceTd = createElement('td', '฿' + formatMoney(product.sellingPrice));
                row.appendChild(priceTd);

                const stockTd = createElement('td');
                const badgeClass = isLow ? 'badge-low-stock' : 'badge-in-stock';
                const stockBadge = createElement(
                    'span',
                    `${(Number(product.stock) || 0).toLocaleString()} Units`,
                    `badge-status ${badgeClass}`
                );
                stockTd.appendChild(stockBadge);
                row.appendChild(stockTd);

                const minTd = createElement('td', `${(Number(product.minStockAlert) || 20).toLocaleString()} Units`);
                row.appendChild(minTd);

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

                // --- 2. Chart Bar ---
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

        // Form Submission
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

    function init() {
        initManagerNavigation();
        initDashboard();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
