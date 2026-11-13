(function () {
    'use strict';

    const user = window.api.getCurrentUser();
    if (!user || user.role !== 'STAFF') {
        window.location.replace('../index.html');
        return;
    }

    const identity = user.userId || user.username;
    const searchInput = document.getElementById('activity-search');
    const operationFilter = document.getElementById('activity-operation');
    const sources = [
        { operation: 'Receive', key: 'stocktrack_receipts_' + user.userId, countId: 'staff-receipt-count', dateField: 'receivedDate' },
        { operation: 'Issue', key: 'stocktrack_issues_' + identity, countId: 'staff-issue-count', dateField: 'issuedDate' },
        { operation: 'Transfer', key: 'stocktrack_transfers_' + identity, countId: 'staff-transfer-count', dateField: 'transferDate' }
    ];
    const activityRows = document.getElementById('staff-activity-rows');
    const recordsByOperation = new Map();
    const activities = [];
    let storageUnavailable = false;

    sources.forEach(source => {
        let records = [];
        try {
            const parsed = JSON.parse(localStorage.getItem(source.key) || '[]');
            if (Array.isArray(parsed)) records = parsed.filter(record => record && Array.isArray(record.items));
        } catch (error) {
            storageUnavailable = true;
        }
        recordsByOperation.set(source.operation, records);
        document.getElementById(source.countId).textContent = records.length.toLocaleString();

        records.forEach(record => {
            const date = record.confirmedAt || record[source.dateField] || '';
            record.items.forEach(item => {
                if (!item || typeof item.product !== 'string' || !Number.isFinite(Number(item.quantity))) return;
                activities.push({ operation: source.operation, product: item.product, quantity: Number(item.quantity), date });
            });
        });
    });

    const totalRecords = [...recordsByOperation.values()].reduce((sum, records) => sum + records.length, 0);
    document.getElementById('staff-total-count').textContent = totalRecords.toLocaleString();

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

    activities.sort((left, right) => (Date.parse(right.date) || 0) - (Date.parse(left.date) || 0));
    const visibleActivities = activities.slice(0, 10);
    function renderActivities() {
        activityRows.replaceChildren();
        const searchTerm = searchInput.value.trim().toLocaleLowerCase();
        const selectedOperation = operationFilter.value;
        const filteredActivities = visibleActivities.filter(activity => {
            const matchesSearch = !searchTerm || `${activity.operation} ${activity.product}`.toLocaleLowerCase().includes(searchTerm);
            return matchesSearch && (!selectedOperation || activity.operation === selectedOperation);
        });

        if (!visibleActivities.length || !filteredActivities.length) {
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 5;
            cell.className = 'activity-empty-state';
            cell.textContent = !visibleActivities.length
                ? storageUnavailable
                    ? 'Local activity could not be read in this browser.'
                    : 'No staff activity has been saved in this browser yet.'
                : 'No saved activity matches these filters.';
            row.appendChild(cell);
            activityRows.appendChild(row);
            return;
        }

        const fragment = document.createDocumentFragment();
        filteredActivities.forEach(activity => {
            const row = document.createElement('tr');
            const parsedDate = new Date(activity.date);
            const dateText = !activity.date ? '—' : Number.isNaN(parsedDate.getTime()) ? activity.date : parsedDate.toLocaleString('en-GB', {
                day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
            });
            [dateText, activity.operation, activity.product, activity.quantity].forEach(value => {
                const cell = document.createElement('td');
                cell.textContent = value;
                row.appendChild(cell);
            });
            const statusCell = document.createElement('td');
            const badge = document.createElement('span');
            badge.className = 'activity-status';
            badge.textContent = 'Saved locally';
            statusCell.appendChild(badge);
            row.appendChild(statusCell);
            fragment.appendChild(row);
        });
        activityRows.appendChild(fragment);
    }

    searchInput.addEventListener('input', renderActivities);
    operationFilter.addEventListener('change', renderActivities);
    renderActivities();
})();
