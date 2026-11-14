/**
 * StockTrack - Owner Dashboard Script
 * Fetches and displays business KPIs and Product Performance data.
 */
document.addEventListener('DOMContentLoaded', () => {
    loadOwnerDashboard();
});

async function loadOwnerDashboard() {
    const rawUser = localStorage.getItem('stocktrack_user');
    let userId = '';
    if (rawUser) {
        try {
            const user = JSON.parse(rawUser);
            userId = user.userId || user.username || '';
        } catch (e) { }
    }

    try {
        const res = await fetch('/api/owner/dashboard', {
            headers: {
                'Content-Type': 'application/json',
                ...(userId ? { 'X-User-Id': String(userId) } : {})
            }
        });

        if (!res.ok) {
            throw new Error(`Failed to fetch dashboard data (${res.status})`);
        }

        const json = await res.json();
        const data = json.data;

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
    const elRevenue = document.getElementById('owner-total-revenue');
    const elInventory = document.getElementById('owner-inventory-value');
    const elSale = document.getElementById('owner-total-sale');
    const elExport = document.getElementById('owner-total-export');
    const elProfit = document.getElementById('owner-total-profit');

    if (elRevenue) elRevenue.textContent = data.totalRevenue || '฿0';
    if (elInventory) elInventory.textContent = data.inventoryValue || '฿0';
    if (elSale) elSale.textContent = data.totalSale || '0';
    if (elExport) elExport.textContent = data.totalExport || '0';
    if (elProfit) elProfit.textContent = data.totalProfit || '฿0';
}

function renderHotSellers(items) {
    const container = document.getElementById('hot-seller-container');
    if (!container) return;

    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="performance-empty">
                <div class="empty-icon">★</div>
                <h4>No sales data available</h4>
                <p>Best selling products will appear here.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
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
    const container = document.getElementById('top-import-container');
    if (!container) return;

    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="performance-empty">
                <div class="empty-icon">▦</div>
                <h4>No import data available</h4>
                <p>Most imported products will appear here.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `
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

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
