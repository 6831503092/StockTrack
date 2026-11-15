/**
 * StockTrack - Owner Dashboard Script
 * Fetches and displays business KPIs and Product Performance data.
 */
(function () {
    // Role Guard
    if (window.Auth) {
        window.Auth.requireRole('OWNER');
    }

    const escapeHtml = (window.Format && window.Format.escapeHtml) || (s => String(s || ''));

    // DOM Elements Cache
    const elRevenue = document.getElementById('owner-total-revenue');
    const elInventory = document.getElementById('owner-inventory-value');
    const elSale = document.getElementById('owner-total-sale');
    const elExport = document.getElementById('owner-total-export');
    const elProfit = document.getElementById('owner-total-profit');
    const hotSellerContainer = document.getElementById('hot-seller-container');
    const topImportContainer = document.getElementById('top-import-container');

    async function loadOwnerDashboard() {
        try {
            const json = await window.api.get('/api/owner/dashboard');
            const data = json?.data;

            if (data) {
                renderKPIs(data);
                renderHotSellers(data.hotSellers);
                renderTopImports(data.topImports);
            }
        } catch (err) {
            console.error('Error loading owner dashboard:', err);
        }
    }

    function renderKPIs(data) {
        if (elRevenue) elRevenue.textContent = data.totalRevenue || '฿0';
        if (elInventory) elInventory.textContent = data.inventoryValue || '฿0';
        if (elSale) elSale.textContent = data.totalSale || '0';
        if (elExport) elExport.textContent = data.totalExport || '0';
        if (elProfit) elProfit.textContent = data.totalProfit || '฿0';
    }

    function renderHotSellers(items) {
        if (!hotSellerContainer) return;

        if (!items || items.length === 0) {
            hotSellerContainer.innerHTML = `
                <div class="performance-empty">
                    <div class="empty-icon">★</div>
                    <h4>No sales data available</h4>
                    <p>Best selling products will appear here.</p>
                </div>
            `;
            return;
        }

        hotSellerContainer.innerHTML = `
            <div class="performance-list">
                ${items.map(item => `
                    <div class="performance-item">
                        <div class="performance-left">
                            <span class="performance-rank rank-${item.rank}">${item.rank}</span>
                            <div class="performance-product-info">
                                <span class="performance-product-name">${escapeHtml(item.productName)}</span>
                                <span class="performance-product-zone">${escapeHtml(item.zoneName || 'Warehouse')}</span>
                            </div>
                        </div>
                        <div class="performance-right">
                            <span class="performance-value">${escapeHtml(item.amount)}</span>
                            <span class="performance-sub">${item.quantity} sold</span>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    function renderTopImports(items) {
        if (!topImportContainer) return;

        if (!items || items.length === 0) {
            topImportContainer.innerHTML = `
                <div class="performance-empty">
                    <div class="empty-icon">▦</div>
                    <h4>No import data available</h4>
                    <p>Most imported products will appear here.</p>
                </div>
            `;
            return;
        }

        topImportContainer.innerHTML = `
            <div class="performance-list">
                ${items.map(item => `
                    <div class="performance-item">
                        <div class="performance-left">
                            <span class="performance-rank rank-${item.rank}">${item.rank}</span>
                            <div class="performance-product-info">
                                <span class="performance-product-name">${escapeHtml(item.productName)}</span>
                                <span class="performance-product-zone">${escapeHtml(item.zoneName || 'Warehouse')}</span>
                            </div>
                        </div>
                        <div class="performance-right">
                            <span class="performance-value">${escapeHtml(item.amount)}</span>
                            <span class="performance-sub">${item.quantity} units</span>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    window.loadOwnerDashboard = loadOwnerDashboard;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadOwnerDashboard);
    } else {
        loadOwnerDashboard();
    }
})();
