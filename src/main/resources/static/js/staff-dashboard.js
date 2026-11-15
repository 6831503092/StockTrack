(function () {
    'use strict';

    const user = window.api.getCurrentUser();
    if (!user || (user.role !== 'STAFF' && user.role !== 'MANAGER' && user.role !== 'ADMIN')) {
        window.location.replace('../index.html');
        return;
    }

    const searchInput = document.getElementById('activity-search');
    const operationFilter = document.getElementById('activity-operation');
    const activityRows = document.getElementById('staff-activity-rows');
    let activities = [];

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

    async function loadDashboard() {
        try {
            const res = await window.api.get('/api/staff/dashboard');
            const data = (res && res.data) ? res.data : res;
            if (data) {
                document.getElementById('staff-receipt-count').textContent = Number(data.receiptCount || 0).toLocaleString();
                document.getElementById('staff-issue-count').textContent = Number(data.stockOutCount || 0).toLocaleString();
                document.getElementById('staff-transfer-count').textContent = Number(data.transferCount || 0).toLocaleString();
                document.getElementById('staff-total-count').textContent = Number(data.totalCount || 0).toLocaleString();
                activities = Array.isArray(data.recentActivities) ? data.recentActivities : [];
            }
        } catch (err) {
            console.error('Failed to load dashboard data:', err);
            document.getElementById('staff-receipt-count').textContent = '0';
            document.getElementById('staff-issue-count').textContent = '0';
            document.getElementById('staff-transfer-count').textContent = '0';
            document.getElementById('staff-total-count').textContent = '0';
            activities = [];
        }
        renderActivities();
    }

    function renderActivities() {
        activityRows.replaceChildren();
        const searchTerm = (searchInput.value || '').trim().toLowerCase();
        const selectedOperation = operationFilter.value;
        const filteredActivities = activities.filter(activity => {
            const matchesSearch = !searchTerm || `${activity.operation} ${activity.product}`.toLowerCase().includes(searchTerm);
            return matchesSearch && (!selectedOperation || activity.operation === selectedOperation);
        });

        if (!activities.length || !filteredActivities.length) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 5;
            cell.className = 'activity-empty-state';
            cell.textContent = !activities.length
                ? 'No staff activity has been recorded yet.'
                : 'No recorded activity matches these filters.';
            row.appendChild(cell);
            activityRows.appendChild(row);
            return;
        }

        const fragment = document.createDocumentFragment();
        filteredActivities.forEach(activity => {
            const row = document.createElement('tr');
            [activity.date || '—', activity.operation, activity.product, activity.quantity || 1].forEach(value => {
                const cell = document.createElement('td');
                cell.textContent = value;
                row.appendChild(cell);
            });
            const statusCell = document.createElement('td');
            statusCell.style.textAlign = 'center';
            const badge = document.createElement('span');
            const isRejected = (activity.status || '').toLowerCase().includes('reject');
            const bClass = window.Format ? window.Format.getStatusBadgeClass(activity.status) : (isRejected ? 'badge-rejected' : 'badge-approved');
            const dStatus = window.Format ? window.Format.formatDisplayStatus(activity.status || 'Completed') : (activity.status || 'Completed');
            badge.className = `badge-status ${bClass}`;
            badge.textContent = dStatus;
            statusCell.appendChild(badge);
            row.appendChild(statusCell);
            fragment.appendChild(row);
        });
        activityRows.appendChild(fragment);
    }

    searchInput.addEventListener('input', renderActivities);
    operationFilter.addEventListener('change', renderActivities);

    loadDashboard();
})();
