(function () {
    'use strict';
    const user = window.api.getCurrentUser();
    if (!user || (user.role !== 'STAFF' && user.role !== 'MANAGER' && user.role !== 'ADMIN')) {
        window.location.replace('../index.html');
        return;
    }

    let requests = [];
    let step = 0, selected = '', quantities = [], notes = '', confirmed = false;
    const content = document.getElementById('receive-stage-content');
    const alert = document.getElementById('receive-alert');
    const next = document.getElementById('receive-next');
    const back = document.getElementById('receive-back');
    const rejectBtn = document.getElementById('receive-reject');
    const rejectModal = document.getElementById('receive-reject-modal');
    const rejectForm = document.getElementById('receive-reject-form');
    const rejectReason = document.getElementById('receive-reject-reason');
    const rejectError = document.getElementById('receive-reject-error');
    const rejectCancel = document.getElementById('receive-reject-cancel');
    const rejectConfirm = document.getElementById('receive-reject-confirm');
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
        wrapper.className = 'receive-table-scroll requests-table-wrapper';
        const tbl = el('table', undefined, wrapper);
        tbl.className = 'requests-table';
        const row = el('tr', undefined, el('thead', undefined, tbl));
        headers.forEach(text => { const th = el('th', text, row); th.scope = 'col'; });
        return el('tbody', undefined, tbl);
    }

    async function loadRequests() {
        if (!content) return;
        content.replaceChildren();
        el('p', 'Loading approved purchase orders from manager...', content);
        try {
            const res = await window.api.get('/api/staff/receives/pending');
            const data = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
            requests = data.map(po => ({
                id: po.code || ('PO-' + po.id),
                rawId: po.id,
                reference: po.reference || ('PO-' + po.id),
                requester: po.requester || 'Admin',
                supplier: po.partyName || 'Supplier',
                date: po.date || '—',
                items: (po.items || []).map(it => [it.productName, it.expectedQty, it.itemId, it.zoneName])
            }));
            if (requests.length > 0 && !selected) {
                choose(requests[0].id);
            }
        } catch (err) {
            error('Failed to load pending requests from server: ' + (err.message || err));
            requests = [];
        }
        render();
    }

    function render() {
        if (!content) return;
        content.replaceChildren();
        actions?.querySelector('.receive-confirm-check')?.remove();
        const stageTitle = document.getElementById('receive-stage-title');
        const stageLabel = document.getElementById('receive-stage-label');
        const subtitle = document.getElementById('receive-subtitle');

        if (stageTitle) stageTitle.textContent = titles[step];
        if (stageLabel) stageLabel.textContent = titles[step].toUpperCase();
        if (subtitle) {
            subtitle.textContent = [
                'Select an approved purchase order to begin receiving',
                'Check quantities against the physical goods delivered',
                'Review and confirm receiving goods into inventory',
                'Receiving is complete'
            ][step];
        }

        document.querySelectorAll('.receive-steps li').forEach((node, i) => {
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
            else if (step === 2) next.textContent = 'Confirm Receipt →';
            else if (step === 3) next.textContent = 'Receive Another →';
        }

        if (step === 0) {
            el('h3', 'Available Approved Purchase Orders (From Manager)', content);
            const body = table(['Order ID', 'Reference', 'Supplier', 'Created By', 'Date', 'Items', 'Status', 'Action', 'Select']);
            requests.forEach(item => {
                const row = el('tr', undefined, body);
                [item.id, item.reference, item.supplier, item.requester, item.date, item.items.length].forEach(value => el('td', value, row));
                const statusTd = el('td', undefined, row);
                const badge = el('span', 'Approved', statusTd);
                badge.className = 'badge-status badge-approved';
                const actionTd = el('td', undefined, row);
                const open = el('button', 'Open →', actionTd);
                open.type = 'button';
                open.className = 'receive-open btn-action-detail';
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
                const cell = el('td', 'No approved purchase orders awaiting receipt. Waiting for manager approval.', el('tr', undefined, body));
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
            panel.className = 'receive-details';
            el('h3', 'Receipt saved and inventory updated successfully!', panel);
            el('p', 'Purchase Order ' + item.id + ' (' + item.reference + ') has been marked as RECEIVED.', panel);
            el('p', quantities.reduce((sum, q) => sum + q, 0) + ' units have been added to warehouse stock.', panel);

            const nav = el('div', undefined, panel);
            nav.style.cssText = 'margin-top: 20px; display: flex; gap: 12px;';
            const dashLink = el('a', '← Back to Staff Dashboard', nav);
            dashLink.href = 'dashboard.html';
            dashLink.className = 'btn-secondary';
            dashLink.style.cssText = 'display: inline-flex; align-items: center; padding: 10px 18px; border-radius: 8px; text-decoration: none; font-size: 13px; font-weight: 600; border: 1px solid #cbd5e1; color: #475569; background: #fff;';
            return;
        }

        const panel = el('div', undefined, content);
        panel.className = 'receive-details';
        el('h3', item.id + ' • ' + item.reference, panel);
        el('p', 'Supplier: ' + item.supplier + ' | Created by: ' + item.requester + ' (' + item.date + ')', panel);
        el('p', 'Status: Approved by Manager', panel);

        if (step === 1) {
            const notice = el('div', undefined, content);
            notice.className = 'receive-exact-notice';
            notice.style.cssText = 'margin: 16px 0; padding: 12px 16px; border-radius: 8px; background: #eef2ff; border: 1px solid #c7d2fe; color: #3730a3; font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 8px;';
            notice.innerHTML = '<span style="font-size: 16px;">ℹ️</span> <span><strong>ข้อกำหนดการรับสินค้า:</strong> สินค้าต้องมีจำนวนครบถ้วนตามใบสั่งซื้อพอดี (ห้ามขาดหรือเกิน) ถึงจะสามารถบันทึกรับเข้าคลังได้ หากสินค้าไม่ครบหรือเกิน กรุณากดปุ่ม <strong>✕ Reject Order</strong> พร้อมระบุเหตุผล</span>';

            const body = table(['Product', 'Destination Zone', 'Expected Quantity', 'Received Quantity']);
            item.items.forEach((product, i) => {
                const row = el('tr', undefined, body);
                el('td', product[0], row);
                el('td', product[3] || 'Default Zone', row);
                el('td', String(product[1]), row);
                const cell = el('td', undefined, row);
                const input = el('input', undefined, cell);
                input.type = 'number';
                input.min = '0';
                input.max = String(product[1] * 2);
                input.step = '1';
                input.required = true;
                const currentVal = quantities[i] !== undefined ? quantities[i] : product[1];
                input.value = currentVal;
                input.className = 'receive-quantity';
                input.setAttribute('aria-label', 'Received quantity for ' + product[0]);
                if (Number(currentVal) !== Number(product[1])) {
                    input.style.borderColor = '#ef4444';
                    input.style.backgroundColor = '#fef2f2';
                }
                input.addEventListener('input', () => { 
                    quantities[i] = Number(input.value); 
                    if (Number(input.value) !== Number(product[1])) {
                        input.style.borderColor = '#ef4444';
                        input.style.backgroundColor = '#fef2f2';
                    } else {
                        input.style.borderColor = '#cbd5e1';
                        input.style.backgroundColor = '';
                    }
                });
            });

            const label = el('label', 'Delivery notes (optional)', content);
            label.className = 'receive-notes-label';
            const textarea = el('textarea', undefined, label);
            textarea.rows = 3;
            textarea.maxLength = 1000;
            textarea.value = notes;
            textarea.placeholder = 'Condition of delivery, packages, or notes';
            textarea.addEventListener('input', () => { notes = textarea.value; });
        }

        if (step === 2) {
            const body = table(['Product', 'Destination Zone', 'Expected Quantity', 'Received Quantity']);
            item.items.forEach((product, i) => {
                const row = el('tr', undefined, body);
                el('td', product[0], row);
                el('td', product[3] || 'Default Zone', row);
                el('td', String(product[1]), row);
                el('td', String(quantities[i]), row);
            });

            if (notes) el('p', 'Notes: ' + notes, panel);
            const label = el('label', undefined, actions);
            label.className = 'receive-confirm-check';
            const check = el('input', undefined, label);
            check.type = 'checkbox';
            check.checked = confirmed;
            check.addEventListener('change', () => { confirmed = check.checked; });
            el('span', 'I have verified the physical goods. Add these items to warehouse stock now.', label);
        }
    }

    function focusTitle() {
        document.getElementById('receive-stage-title')?.focus();
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
                error('Select an available approved purchase order to continue.');
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
                if (!quantities.every((q, i) => Number.isInteger(q) && q >= 0)) {
                    error('Please enter valid received quantities.');
                    return;
                }
                const mismatchIndex = item.items.findIndex((prod, i) => Number(quantities[i]) !== Number(prod[1]));
                if (mismatchIndex !== -1) {
                    const mismatchedProduct = item.items[mismatchIndex];
                    error(`ถ้ารับของเข้า จำนวนต้องครบพอดี ห้ามขาดหรือเกินถึงจะกดรับของเข้าคลังได้: สินค้า "${mismatchedProduct[0]}" มีจำนวนที่ระบุ (${quantities[mismatchIndex]}) ไม่ตรงกับยอดสั่งซื้อ (${mismatchedProduct[1]}) หากสินค้าไม่ครบหรือเกิน กรุณากดปุ่ม ✕ Reject Order พร้อมระบุเหตุผล`);
                    return;
                }
                step = 2;
                render();
                focusTitle();
                return;
            }

            if (step === 2) {
                if (!confirmed) {
                    error('Please confirm that you have checked the goods and quantities.');
                    return;
                }

                const mismatchIndex = item.items.findIndex((prod, i) => Number(quantities[i]) !== Number(prod[1]));
                if (mismatchIndex !== -1) {
                    const mismatchedProduct = item.items[mismatchIndex];
                    error(`ถ้ารับของเข้า จำนวนต้องครบพอดี ห้ามขาดหรือเกินถึงจะกดรับของเข้าคลังได้: สินค้า "${mismatchedProduct[0]}" (${quantities[mismatchIndex]}/${mismatchedProduct[1]}) กรุณากด Reject พร้อมระบุเหตุผล`);
                    return;
                }

                next.disabled = true;
                next.textContent = 'Saving to inventory...';
                try {
                    const payload = {
                        items: item.items.map((prod, i) => ({
                            itemId: prod[2],
                            countedQty: quantities[i],
                            isCorrectProduct: true
                        })),
                        notes: notes
                    };
                    await window.api.post('/api/staff/receives/' + item.rawId + '/accept', payload);
                    step = 3;
                    render();
                    focusTitle();
                } catch (err) {
                    error('Failed to submit receiving: ' + (err.message || err));
                } finally {
                    next.disabled = false;
                    next.textContent = step === 3 ? 'Receive Another →' : 'Confirm Receipt →';
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
                await window.api.post('/api/staff/receives/' + item.rawId + '/reject', { reason: reason });
                rejectModal.close();
                if (alert) {
                    alert.textContent = 'Purchase Order ' + item.id + ' has been rejected.';
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
        const status = document.getElementById('receive-connection');
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
