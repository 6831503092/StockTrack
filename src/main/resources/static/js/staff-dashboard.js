(function () {
    'use strict';
    const user = window.api.getCurrentUser();
    if (!user || user.role !== 'STAFF') {
        window.location.replace('../index.html');
        return;
    }
    const status = document.getElementById('connection-status');
    status.setAttribute('role', 'status');
    function updateConnection() {
        const dot = document.createElement('span');
        dot.setAttribute('aria-hidden', 'true');
        status.classList.toggle('offline', !navigator.onLine);
        status.replaceChildren(dot, document.createTextNode(navigator.onLine ? 'Online' : 'Offline'));
    }
    window.addEventListener('online', updateConnection);
    window.addEventListener('offline', updateConnection);
    updateConnection();
    const activityRows = document.getElementById('staff-activity-rows');
    const identity = user.userId || user.username;
    const sources = [
        ['Receive', 'stocktrack_receipts_' + user.userId],
        ['Issue', 'stocktrack_issues_' + identity],
        ['Transfer', 'stocktrack_transfers_' + identity]
    ];
    const activities = [];
    sources.forEach(([operation, key]) => {
        try {
            const records = JSON.parse(localStorage.getItem(key) || '[]');
            if (!Array.isArray(records)) return;
            records.forEach(record => {
                if (!record || !Array.isArray(record.items)) return;
                record.items.forEach(item => {
                    if (!item || typeof item.product !== 'string' || !Number.isFinite(item.quantity)) return;
                    activities.push({ operation, product: item.product, quantity: item.quantity,
                        date: record.confirmedAt || record.receivedDate || record.issuedDate || record.transferDate || '' });
                });
            });
        } catch (error) { /* Keep sample activity available when local storage cannot be read. */ }
    });
    activities.sort((a, b) => (Date.parse(b.date) || 0) - (Date.parse(a.date) || 0));
    const fragment = document.createDocumentFragment();
    activities.slice(0, 10).forEach(activity => {
        const row = document.createElement('tr');
        const parsedDate = new Date(activity.date);
        const date = Number.isNaN(parsedDate.getTime()) ? activity.date : parsedDate.toLocaleString('en-GB', {
            day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });
        [date, activity.operation, activity.product, activity.quantity, 'Saved locally'].forEach(value => {
            const cell = document.createElement('td');
            cell.textContent = value;
            row.appendChild(cell);
        });
        fragment.appendChild(row);
    });
    activityRows.prepend(fragment);
})();
