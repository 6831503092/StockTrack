/**
 * StockTrack - Owner Activity Log Script
 * Handles real flow activity log fetching, filtering, and table rendering.
 */
(function () {
    // Role Guard
    if (window.Auth) {
        window.Auth.requireRole('OWNER');
    }

    const escapeHtml = (window.Format && window.Format.escapeHtml) || (s => String(s || ''));

    let debounceTimer = null;

    // DOM Elements Cache
    const searchInput = document.getElementById('activity-search');
    const typeSelect = document.getElementById('activity-type');
    const dateInput = document.getElementById('activity-date');
    const tbody = document.getElementById('activity-table-body');

    function initActivityLog() {
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    fetchActivities();
                }, 300);
            });
        }

        if (typeSelect) {
            typeSelect.addEventListener('change', () => {
                fetchActivities();
            });
        }

        if (dateInput) {
            dateInput.addEventListener('change', () => {
                fetchActivities();
            });
        }

        fetchActivities();
    }

    async function fetchActivities() {
        const searchVal = searchInput ? searchInput.value.trim() : '';
        const typeVal = typeSelect ? typeSelect.value : 'all';
        const dateVal = dateInput ? dateInput.value : '';

        const params = new URLSearchParams();
        if (typeVal && typeVal !== 'all') params.append('type', typeVal);
        if (searchVal) params.append('search', searchVal);
        if (dateVal) params.append('date', dateVal);

        try {
            const queryStr = params.toString() ? '?' + params.toString() : '';
            const res = await window.api.get('/api/owner/activities' + queryStr);
            const activities = res?.data || [];
            currentActivities = activities;
            renderActivityTable(activities);
        } catch (err) {
            console.error('Error fetching activities:', err);
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="6" class="activity-no-data">
                            <div class="empty-icon">⚠</div>
                            <h3>Failed to load activities</h3>
                            <p>${escapeHtml(err.message || 'Unknown error')}</p>
                        </td>
                    </tr>
                `;
            }
        }
    }

    let currentActivities = [];

    function renderActivityTable(list) {
        if (!tbody) return;
        currentActivities = list || [];

        if (!list || list.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" class="activity-no-data">
                        <div class="empty-icon">☰</div>
                        <h3>No activity records available</h3>
                        <p>No business activities match your current search or filters.</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = list.map((item, index) => {
            const typeClass = `badge-${(item.type || 'other').toLowerCase()}`;
            const bClass = window.Format ? window.Format.getStatusBadgeClass(item.status) : `badge-${(item.status || 'completed').toLowerCase()}`;
            const displayStatus = window.Format ? window.Format.formatDisplayStatus(item.status || 'Completed') : (item.status || 'Completed');

            return `
                <tr>
                    <td>
                        <span class="activity-badge ${typeClass}">${escapeHtml(item.activityName)}</span>
                    </td>
                    <td>
                        <div style="font-weight:600; color:var(--owner-text); margin-bottom:3px;">
                            ${escapeHtml(item.description)}
                        </div>
                        <div style="font-size:0.73rem; color:var(--owner-muted);">
                            By ${escapeHtml(item.actorName || 'System')} ${item.actorRole ? `(${escapeHtml(item.actorRole)})` : ''}
                        </div>
                    </td>
                    <td>
                        <span class="activity-ref-code">${escapeHtml(item.reference || '—')}</span>
                    </td>
                    <td style="color:var(--owner-muted); white-space:nowrap;">
                        ${escapeHtml(item.dateTime || '—')}
                    </td>
                    <td style="text-align: center;">
                        <span class="badge-status ${bClass}">${escapeHtml(displayStatus)}</span>
                    </td>
                    <td style="text-align: center;">
                        <div class="action-buttons-group">
                            <button type="button" class="btn-action-detail" onclick="openActivityDetail(${index})">Detail</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    window.openActivityDetail = function (index) {
        const item = currentActivities[index];
        if (!item) return;
        const container = document.getElementById('owner-activity-detail');
        if (!container) return;
        container.replaceChildren();

        const appendItem = (label, val, isBadge = false, badgeClass = '') => {
            const row = document.createElement('div');
            row.className = 'req-detail-item';
            const lbl = document.createElement('span');
            lbl.className = 'req-detail-label';
            lbl.textContent = label;
            row.appendChild(lbl);

            const valueEl = document.createElement('div');
            valueEl.className = 'req-detail-val';
            if (isBadge) {
                const badge = document.createElement('span');
                badge.className = `badge-status ${badgeClass}`;
                badge.textContent = val;
                valueEl.appendChild(badge);
            } else {
                valueEl.textContent = val;
            }
            row.appendChild(valueEl);
            container.appendChild(row);
        };

        appendItem('Activity', item.activityName);
        appendItem('Description', item.description || '—');
        appendItem('Reference', item.reference || '—');
        appendItem('Actor', `${item.actorName || 'System'}${item.actorRole ? ' (' + item.actorRole + ')' : ''}`);
        appendItem('Date & Time', item.dateTime || '—');
        const bClass = window.Format ? window.Format.getStatusBadgeClass(item.status) : 'badge-completed';
        const displayStatus = window.Format ? window.Format.formatDisplayStatus(item.status || 'Completed') : (item.status || 'Completed');
        appendItem('Status', displayStatus, true, bClass);

        const modal = document.getElementById('modal-activity-detail');
        if (modal) modal.classList.add('active');
    };

    const closeBtn = document.getElementById('btn-close-activity-modal');
    const cancelBtn = document.getElementById('btn-activity-close');
    const actModal = document.getElementById('modal-activity-detail');
    if (closeBtn) closeBtn.addEventListener('click', () => actModal?.classList.remove('active'));
    if (cancelBtn) cancelBtn.addEventListener('click', () => actModal?.classList.remove('active'));
    if (actModal) {
        actModal.addEventListener('click', (e) => {
            if (e.target === actModal) actModal.classList.remove('active');
        });
    }

    window.fetchActivities = fetchActivities;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initActivityLog);
    } else {
        initActivityLog();
    }
})();
