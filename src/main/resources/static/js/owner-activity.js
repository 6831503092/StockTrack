/**
 * StockTrack - Owner Activity Log Script
 * Handles real flow activity log fetching, filtering, and table rendering.
 */
document.addEventListener('DOMContentLoaded', () => {
    initActivityLog();
});

let debounceTimer = null;

function initActivityLog() {
    const searchInput = document.getElementById('activity-search');
    const typeSelect = document.getElementById('activity-type');
    const dateInput = document.getElementById('activity-date');

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
    const searchInput = document.getElementById('activity-search');
    const typeSelect = document.getElementById('activity-type');
    const dateInput = document.getElementById('activity-date');
    const tbody = document.getElementById('activity-table-body');

    const searchVal = searchInput ? searchInput.value.trim() : '';
    const typeVal = typeSelect ? typeSelect.value : 'all';
    const dateVal = dateInput ? dateInput.value : '';

    const params = new URLSearchParams();
    if (typeVal && typeVal !== 'all') params.append('type', typeVal);
    if (searchVal) params.append('search', searchVal);
    if (dateVal) params.append('date', dateVal);

    const rawUser = localStorage.getItem('stocktrack_user');
    let userId = '';
    if (rawUser) {
        try {
            const user = JSON.parse(rawUser);
            userId = user.userId || user.username || '';
        } catch (e) { }
    }

    try {
        const url = '/api/owner/activities' + (params.toString() ? '?' + params.toString() : '');
        const res = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                ...(userId ? { 'X-User-Id': String(userId) } : {})
            }
        });

        if (!res.ok) {
            throw new Error(`Failed to load activities (${res.status})`);
        }

        const json = await res.json();
        const activities = json.data || [];
        renderActivityTable(activities);
    } catch (err) {
        console.error('Error fetching activities:', err);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="activity-no-data">
                        <div class="empty-icon">⚠</div>
                        <h3>Failed to load activities</h3>
                        <p>${escapeHtml(err.message)}</p>
                    </td>
                </tr>
            `;
        }
    }
}

function renderActivityTable(list) {
    const tbody = document.getElementById('activity-table-body');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="activity-no-data">
                    <div class="empty-icon">☰</div>
                    <h3>No activity records available</h3>
                    <p>No business activities match your current search or filters.</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = list.map(item => {
        const typeClass = `badge-${(item.type || 'other').toLowerCase()}`;
        const statusClass = `badge-status-${(item.status || 'completed').toLowerCase()}`;

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
                <td>
                    <span class="activity-badge ${statusClass}">${escapeHtml(item.status || 'Completed')}</span>
                </td>
            </tr>
        `;
    }).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
