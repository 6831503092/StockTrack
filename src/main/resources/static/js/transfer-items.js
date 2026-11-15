(function () {
    'use strict';
    const user = window.api.getCurrentUser();
    if (!user || (user.role !== 'STAFF' && user.role !== 'MANAGER' && user.role !== 'ADMIN')) {
        window.location.replace('../index.html');
        return;
    }

    let requests = [];
    let step = 0, selected = '', quantities = [], notes = '', confirmed = false;
    const content = document.getElementById('transfer-stage-content');
    const alert = document.getElementById('transfer-alert');
    const next = document.getElementById('transfer-next');
    const back = document.getElementById('transfer-back');
    const rejectBtn = document.getElementById('transfer-reject');
    const rejectModal = document.getElementById('transfer-reject-modal');
    const rejectForm = document.getElementById('transfer-reject-form');
    const rejectReason = document.getElementById('transfer-reject-reason');
    const rejectError = document.getElementById('transfer-reject-error');
    const rejectCancel = document.getElementById('transfer-reject-cancel');
    const rejectConfirm = document.getElementById('transfer-reject-confirm');
    const actions = document.getElementById('workflow-actions') || document.querySelector('.workflow-actions');
    const titles = ['Requests', 'Verify', 'Confirm', 'Result'];

    function el(tag, text, parent) {
        const node = document.createElement(tag);
        if (text !== undefined) node.textContent = text;
        if (parent) parent.appendChild(node);
        return node;
    }

    function error(text) {
        if (alert) { alert.textContent = text; alert.hidden = false; }
    }

    function request() {
        return requests.find(item => item.id === selected);
    }

    function choose(id) {
        selected = id;
        const cur = request();
        quantities = cur ? cur.items.map(item => item[1]) : [];
        notes = '';
        confirmed = false;
        if (alert) alert.hidden = true;
    }

    function table(headers) {
        const wrapper = el('div', undefined, content);
        wrapper.className = 'transfer-table-scroll requests-table-wrapper';
        const tbl = el('table', undefined, wrapper);
        tbl.className = 'requests-table';
        const row = el('tr', undefined, el('thead', undefined, tbl));
        headers.forEach(text => { const th = el('th', text, row); th.scope = 'col'; });
        return el('tbody', undefined, tbl);
    }

    async function loadRequests() {
        if (!content) return;
        content.replaceChildren();
        el('p', 'Loading approved stock movements from manager...', content);
        try {
            const res = await window.api.get('/api/staff/transfers/pending');
            const data = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
            requests = data.map(tr => ({
                id: tr.code || ('TR-' + tr.movementId),
                rawId: tr.movementId,
                reference: tr.reference || ('TR-' + tr.movementId),
                requester: tr.requestedBy || 'Admin',
                date: tr.date || '—',
                productName: tr.productName || 'Product',
                fromZone: tr.fromZoneName || 'Source Zone',
                toZone: tr.toZoneName || 'Target Zone',
                quantity: tr.quantity || 1,
                items: [[tr.productName, tr.quantity, tr.productId, tr.fromZoneName + ' → ' + tr.toZoneName]]
            }));
            if (requests.length > 0 && !selected) {
                choose(requests[0].id);
            }
        } catch (err) {
            error('Failed to load pending transfers: ' + (err.message || err));
            requests = [];
        }
        render();
    }

    function render() {
        if (!content) return;
        content.replaceChildren();
        actions?.querySelector('.transfer-confirm-check')?.remove();
        const stageTitle = document.getElementById('transfer-stage-title');
        const stageLabel = document.getElementById('transfer-stage-label');
        const subtitle = document.getElementById('transfer-subtitle');

        if (stageTitle) stageTitle.textContent = titles[step];
        if (stageLabel) stageLabel.textContent = titles[step].toUpperCase();
        if (subtitle) {
            subtitle.textContent = [
                'Select an approved transfer request to begin verification',
                'Verify physical item relocation between zones',
                'Review and confirm zone transfer',
                'Transfer verification is complete'
            ][step];
        }

        document.querySelectorAll('.transfer-steps li').forEach((node, i) => {
            node.removeAttribute('aria-current');
            if (i === step) node.setAttribute('aria-current', 'step');
            node.classList.toggle('complete', i < step);
        });

        if (back) back.hidden = step === 0 || step === 3;
        if (rejectBtn) rejectBtn.hidden = (step === 0 || step === 3);
        if (next) {
            next.hidden = (step === 0 && !requests.length);
            if (step === 0) next.textContent = 'Continue to Verify →';
            else if (step === 1) next.textContent = 'Continue to Confirm →';
            else if (step === 2) next.textContent = 'Confirm Transfer →';
            else if (step === 3) next.textContent = 'Transfer Another →';
        }

        if (step === 0) {
            el('h3', 'Available Approved Stock Movements (From Manager)', content);
            const body = table(['Transfer ID', 'Reference', 'Product', 'From Zone', 'To Zone', 'Quantity', 'Status', 'Action', 'Select']);
            requests.forEach(item => {
                const row = el('tr', undefined, body);
                [item.id, item.reference, item.productName, item.fromZone, item.toZone, item.quantity].forEach(value => el('td', value, row));
                const statusTd = el('td', undefined, row);
                const badge = el('span', 'Pending Verification', statusTd);
                badge.className = 'badge-status badge-pending';
                const actionTd = el('td', undefined, row);
                const open = el('button', 'Open →', actionTd);
                open.type = 'button';
                open.className = 'transfer-open btn-action-detail';
                open.setAttribute('aria-label', 'Open ' + item.id);
                open.addEventListener('click', () => { choose(item.id); step = 1; render(); focusTitle(); });
                const selectTd = el('td', undefined, row);
                const radio = el('input', undefined, selectTd);
                radio.type = 'radio';
                radio.name = 'request';
                radio.value = item.id;
                radio.checked = item.id === selected;
                radio.setAttribute('aria-label', 'Select ' + item.id);
                radio.addEventListener('change', () => choose(item.id));
            });
            if (!requests.length) {
                const cell = el('td', 'No approved stock movements awaiting verification. Waiting for manager approval.', el('tr', undefined, body));
                cell.colSpan = 9;
                cell.style.textAlign = 'center';
                cell.style.padding = '32px';
            }
            return;
        }

        const item = request();
        if (!item) {
            step = 0;
            render();
            return;
        }

        if (step === 3) {
            const panel = el('div', undefined, content);
            panel.className = 'transfer-details';
            el('h3', 'Stock movement verified and completed successfully!', panel);
            el('p', 'Transfer ' + item.id + ' (' + item.reference + ') has been completed.', panel);
            el('p', item.productName + ' location has been updated from ' + item.fromZone + ' to ' + item.toZone + '.', panel);

            const nav = el('div', undefined, panel);
            nav.style.cssText = 'margin-top: 20px; display: flex; gap: 12px;';
            const dashLink = el('a', '← Back to Staff Dashboard', nav);
            dashLink.href = 'dashboard.html';
            dashLink.className = 'btn-secondary';
            dashLink.style.cssText = 'display: inline-flex; align-items: center; padding: 10px 18px; border-radius: 8px; text-decoration: none; font-size: 13px; font-weight: 600; border: 1px solid #cbd5e1; color: #475569; background: #fff;';
            return;
        }

        const panel = el('div', undefined, content);
        panel.className = 'transfer-details';
        el('h3', item.id + ' • ' + item.reference, panel);
        el('p', 'Relocation: ' + item.fromZone + ' → ' + item.toZone + ' | Requested by: ' + item.requester + ' (' + item.date + ')', panel);
        el('p', 'Status: Approved by Manager (Awaiting Physical Verification)', panel);

        if (step === 1) {
            const body = table(['Product', 'From Zone → To Zone', 'Expected Quantity', 'Verified Quantity']);
            item.items.forEach((product, i) => {
                const row = el('tr', undefined, body);
                el('td', product[0], row);
                el('td', product[3], row);
                el('td', String(product[1]), row);
                const cell = el('td', undefined, row);
                const input = el('input', undefined, cell);
                input.type = 'number';
                input.min = '1';
                input.max = String(product[1]);
                input.step = '1';
                input.required = true;
                input.value = quantities[i] !== undefined ? quantities[i] : product[1];
                input.className = 'transfer-quantity';
                input.setAttribute('aria-label', 'Verified quantity for ' + product[0]);
                input.addEventListener('input', () => { quantities[i] = Number(input.value); });
            });

            const label = el('label', 'Transfer verification notes (optional)', content);
            label.className = 'transfer-notes-label';
            const textarea = el('textarea', undefined, label);
            textarea.rows = 3;
            textarea.maxLength = 1000;
            textarea.value = notes;
            textarea.placeholder = 'Physical relocation notes or bay/shelf details';
            textarea.addEventListener('input', () => { notes = textarea.value; });
        }

        if (step === 2) {
            const body = table(['Product', 'From Zone → To Zone', 'Expected Quantity', 'Verified Quantity']);
            item.items.forEach((product, i) => {
                const row = el('tr', undefined, body);
                el('td', product[0], row);
                el('td', product[3], row);
                el('td', String(product[1]), row);
                el('td', String(quantities[i]), row);
            });

            if (notes) el('p', 'Notes: ' + notes, panel);
            const label = el('label', undefined, actions);
            label.className = 'transfer-confirm-check';
            const check = el('input', undefined, label);
            check.type = 'checkbox';
            check.checked = confirmed;
            check.addEventListener('change', () => { confirmed = check.checked; });
            el('span', 'I have physically verified and relocated the goods to the destination zone. Update warehouse location now.', label);
        }
    }

    function focusTitle() {
        document.getElementById('transfer-stage-title')?.focus();
    }

    if (next) {
        next.addEventListener('click', async () => {
            if (alert) alert.hidden = true;
            if (step === 3) {
                step = 0;
                selected = '';
                quantities = [];
                notes = '';
                confirmed = false;
                await loadRequests();
                focusTitle();
                return;
            }

            const item = request();
            if (!item) {
                error('Select an available approved transfer request to continue.');
                return;
            }

            if (!quantities.length) {
                quantities = item.items.map(p => p[1]);
            }

            if (step === 0) {
                step = 1;
                render();
                focusTitle();
                return;
            }

            if (step === 1) {
                const inputs = [...content.querySelectorAll('input[type=number]')];
                for (const input of inputs) {
                    if (!input.reportValidity()) return;
                }
                if (!quantities.every((q, i) => Number.isInteger(q) && q >= 1 && q <= item.items[i][1])) {
                    error('Verified quantity must be between 1 and the transfer quantity.');
                    return;
                }
                step = 2;
                render();
                focusTitle();
                return;
            }

            if (step === 2) {
                if (!confirmed) {
                    error('Please confirm that you have physically relocated the items.');
                    return;
                }

                next.disabled = true;
                next.textContent = 'Updating zone location...';
                try {
                    const payload = {
                        items: [{
                            countedQty: quantities[0]
                        }],
                        notes: notes
                    };
                    await window.api.post('/api/staff/transfers/' + item.rawId + '/verify', payload);
                    step = 3;
                    render();
                    focusTitle();
                } catch (err) {
                    error('Failed to verify transfer: ' + (err.message || err));
                } finally {
                    next.disabled = false;
                    next.textContent = step === 3 ? 'Transfer another →' : 'Confirm transfer →';
                }
                return;
            }
        });
    }

    if (back) {
        back.addEventListener('click', () => {
            if (step > 0 && step < 3) {
                step--;
                confirmed = false;
                if (alert) alert.hidden = true;
                render();
                focusTitle();
            }
        });
    }

    if (rejectBtn && rejectModal) {
        rejectBtn.addEventListener('click', () => {
            rejectReason.value = '';
            rejectError.textContent = '';
            rejectError.hidden = true;
            rejectModal.showModal();
            rejectReason.focus();
        });

        rejectCancel.addEventListener('click', () => {
            rejectModal.close();
        });

        rejectForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const reason = rejectReason.value.trim();
            if (!reason) {
                rejectError.textContent = 'กรุณาระบุเหตุผลในการปฏิเสธ (Reject reason is required)';
                rejectError.hidden = false;
                rejectReason.focus();
                return;
            }

            const item = request();
            if (!item) return;

            rejectConfirm.disabled = true;
            rejectConfirm.textContent = 'Rejecting...';
            try {
                await window.api.post('/api/staff/transfers/' + item.rawId + '/reject', { reason: reason });
                rejectModal.close();
                if (alert) {
                    alert.textContent = 'Stock Transfer #' + item.id + ' has been rejected.';
                    alert.hidden = false;
                }
                step = 0;
                selected = '';
                quantities = [];
                notes = '';
                confirmed = false;
                await loadRequests();
                focusTitle();
            } catch (err) {
                rejectError.textContent = 'Failed to reject: ' + (err.message || err);
                rejectError.hidden = false;
            } finally {
                rejectConfirm.disabled = false;
                rejectConfirm.textContent = 'Confirm Reject';
            }
        });
    }

    function connection() {
        const status = document.getElementById('transfer-connection');
        if (!status) return;
        const dot = document.createElement('span');
        dot.setAttribute('aria-hidden', 'true');
        status.classList.toggle('offline', !navigator.onLine);
        status.replaceChildren(dot, document.createTextNode(navigator.onLine ? 'Online' : 'Offline'));
    }
    window.addEventListener('online', connection);
    window.addEventListener('offline', connection);
    connection();

    loadRequests();
})();
