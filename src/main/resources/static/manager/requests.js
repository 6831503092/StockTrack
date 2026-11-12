(() => {
    'use strict';
    const user = window.api?.getCurrentUser?.();
    if (!user || user.role !== 'MANAGER') {
        window.location.replace('../index.html');
        return;
    }
    // Isolated browser data until a request API is implemented.
    const key = 'stocktrack_manager_requests_' + (user.userId || user.username);
    let requests = [];
    try {
        const saved = JSON.parse(localStorage.getItem(key) || '[]');
        if (!Array.isArray(saved) || saved.some(r => !r || !['Pending', 'Approved', 'Rejected'].includes(r.status))) throw Error();
        requests = saved;
    } catch {
        document.querySelector('.empty-subtitle').textContent = 'Saved requests could not be loaded.';
    }
    let status = 'Pending', page = 1, selected = null;
    const size = 10;
    const body = document.getElementById('requests-table-body');
    const empty = body.innerHTML;
    const dialog = document.getElementById('manager-request-dialog');
    const reason = document.getElementById('manager-decision-reason');
    const message = document.getElementById('manager-request-message');
    function node(tag, text, cls) {
        const el = document.createElement(tag);
        el.textContent = text ?? '-';
        if (cls) el.className = cls;
        return el;
    }
    function render() {
        const query = document.getElementById('requests-search-input').value.trim().toLowerCase();
        const filtered = requests.filter(r => (status === 'ALL' || r.status === status) &&
            [r.requestId, r.type, r.requestedByName].some(v => String(v ?? '').toLowerCase().includes(query)));
        page = Math.max(1, Math.min(page, Math.ceil(filtered.length / size)));
        body.replaceChildren();
        for (const r of filtered.slice((page - 1) * size, page * size)) {
            const row = document.createElement('tr');
            [r.requestId, r.type, r.requestedByName, r.date].forEach(v => row.append(node('td', v)));
            const badge = node('td', '');
            badge.append(node('span', r.status, 'badge-status badge-' + r.status.toLowerCase()));
            row.append(badge);
            const cell = node('td', '');
            const button = node('button', '[Detail]', 'btn-action-detail');
            button.type = 'button';
            button.addEventListener('click', () => open(r));
            cell.append(button); row.append(cell); body.append(row);
        }
        if (!filtered.length) body.innerHTML = empty;
        for (const [id, value] of Object.entries({total: requests.length, pending: requests.filter(r => r.status === 'Pending').length,
            approved: requests.filter(r => r.status === 'Approved').length, rejected: requests.filter(r => r.status === 'Rejected').length})) {
            document.getElementById('kpi-req-' + id).textContent = value;
        }
        document.getElementById('requests-pagination-info').textContent = filtered.length
            ? `Showing ${(page - 1) * size + 1} to ${Math.min(page * size, filtered.length)} of ${filtered.length} entries`
            : 'Showing 0 of 0 entries';
        document.getElementById('btn-page-num').textContent = page;
        document.getElementById('btn-requests-prev').disabled = page <= 1;
        document.getElementById('btn-requests-next').disabled = page * size >= filtered.length;
    }
    function open(r) {
        selected = r;
        const detail = document.getElementById('manager-request-detail');
        detail.replaceChildren();
        for (const [label, value] of Object.entries({'Request ID': r.requestId, Type: r.type, 'Requested by': r.requestedByName,
            Date: r.date, Status: r.status, Note: r.note || 'No additional note.', 'Decision reason': r.decisionReason || '-'})) {
            detail.append(node('strong', label), node('p', value));
        }
        reason.value = ''; reason.setCustomValidity(''); message.textContent = '';
        reason.disabled = r.status !== 'Pending';
        dialog.querySelectorAll('[type="submit"]').forEach(b => b.hidden = r.status !== 'Pending');
        dialog.showModal();
    }
    reason.addEventListener('input', () => reason.setCustomValidity(''));
    document.getElementById('manager-decision-form').addEventListener('submit', event => {
        event.preventDefault();
        if (!selected || selected.status !== 'Pending') return;
        const decision = event.submitter?.value;
        if (!['Approved', 'Rejected'].includes(decision)) return;
        if (decision === 'Rejected' && !reason.value.trim()) {
            reason.setCustomValidity('Enter a reason for rejection.'); reason.reportValidity(); return;
        }
        const updated = requests.map(r => r === selected ? {...r, status: decision, decisionReason: reason.value.trim(),
            decidedBy: user.userId || user.username, decidedAt: new Date().toISOString()} : r);
        try { localStorage.setItem(key, JSON.stringify(updated)); }
        catch { message.textContent = 'Unable to save decision. Please try again.'; return; }
        requests = updated; dialog.close(); render();
    });
    document.getElementById('manager-request-close').addEventListener('click', () => dialog.close());
    document.getElementById('requests-tabs').addEventListener('click', event => {
        const tab = event.target.closest('[data-status]');
        if (!tab) return;
        status = tab.dataset.status; page = 1;
        document.querySelectorAll('#requests-tabs button').forEach(b => {
            b.classList.toggle('active', b === tab); b.setAttribute('aria-pressed', String(b === tab));
        });
        render();
    });
    document.getElementById('requests-search-input').addEventListener('input', () => { page = 1; render(); });
    document.getElementById('btn-requests-prev').addEventListener('click', () => { page--; render(); });
    document.getElementById('btn-requests-next').addEventListener('click', () => { page++; render(); });
    render();
})();