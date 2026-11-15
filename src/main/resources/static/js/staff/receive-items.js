/**
 * Receive Items Workflow
 * Configures StaffWizard for inbound purchase order receiving.
 */
(function () {
    'use strict';

    window.StaffWizard.init({
        prefix: 'receive',
        fetchUrl: '/api/staff/receives/pending',
        selectRequestError: 'Select an available approved purchase order to continue.',
        mapItem: (po) => ({
            id: po.code || ('PO-' + po.id),
            rawId: po.id,
            reference: po.reference || ('PO-' + po.id),
            requester: po.requester || 'Admin',
            supplier: po.partyName || 'Supplier',
            date: po.date || '—',
            items: (po.items || []).map(it => [it.productName, it.expectedQty, it.itemId, it.zoneName])
        }),
        subtitles: [
            'Select an approved purchase order to begin receiving',
            'Check quantities against the physical goods delivered',
            'Review and confirm receiving goods into inventory',
            'Receiving is complete'
        ],
        nextBtnText: [
            'Continue to Verify →',
            'Continue to Confirm →',
            'Confirm Receipt →',
            'Receive Another →'
        ],
        step0: {
            heading: 'Available Approved Purchase Orders (From Manager)',
            loadingText: 'Loading approved purchase orders from manager...',
            headers: ['Order ID', 'Reference', 'Supplier', 'Created By', 'Date', 'Items', 'Status', 'Action', 'Select'],
            renderRowValues: (item) => [item.id, item.reference, item.supplier, item.requester, item.date, item.items.length],
            statusBadge: { text: 'Approved', className: 'badge-status badge-approved' },
            emptyText: 'No approved purchase orders awaiting receipt. Waiting for manager approval.'
        },
        renderPanelHeader: (panel, item, el) => {
            el('h3', item.id + ' • ' + item.reference, panel);
            el('p', 'Supplier: ' + item.supplier + ' | Created by: ' + item.requester + ' (' + item.date + ')', panel);
            el('p', 'Status: Approved by Manager', panel);
        },
        step1: {
            renderNotice: (content, el) => {
                const notice = el('div', undefined, content);
                notice.className = 'receive-exact-notice';
                notice.style.cssText = 'margin: 16px 0; padding: 12px 16px; border-radius: 8px; background: #eef2ff; border: 1px solid #c7d2fe; color: #3730a3; font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 8px;';
                notice.innerHTML = '<span style="font-size: 16px;">ℹ️</span> <span><strong>Receiving Requirement:</strong> Delivered quantities must exactly match the purchase order (no shortages or overages allowed) to receive items into warehouse stock. If quantities do not match, please click <strong>✕ Reject Order</strong> and specify a reason.</span>';
            },
            headers: ['Product', 'Destination Zone', 'Expected Quantity', 'Received Quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
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
            },
            notesLabel: 'Delivery notes (optional)',
            notesPlaceholder: 'Condition of delivery, packages, or notes',
            validate: (item, quantities) => {
                if (!quantities.every(q => Number.isInteger(q) && q >= 0)) {
                    return { isValid: false, errorMessage: 'Please enter valid received quantities.' };
                }
                const mismatchIndex = item.items.findIndex((prod, i) => Number(quantities[i]) !== Number(prod[1]));
                if (mismatchIndex !== -1) {
                    const mismatchedProduct = item.items[mismatchIndex];
                    return {
                        isValid: false,
                        errorMessage: `Received quantity must exactly match the purchase order: Product "${mismatchedProduct[0]}" has a specified quantity (${quantities[mismatchIndex]}) that does not match the ordered quantity (${mismatchedProduct[1]}). If items are incomplete or over, please click ✕ Reject Order and specify a reason.`
                    };
                }
                return { isValid: true };
            }
        },
        step2: {
            headers: ['Product', 'Destination Zone', 'Expected Quantity', 'Received Quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
                el('td', product[0], row);
                el('td', product[3] || 'Default Zone', row);
                el('td', String(product[1]), row);
                el('td', String(quantities[i]), row);
            },
            confirmCheckboxLabel: 'I have verified the physical goods. Add these items to warehouse stock now.',
            validate: (item, quantities) => {
                const mismatchIndex = item.items.findIndex((prod, i) => Number(quantities[i]) !== Number(prod[1]));
                if (mismatchIndex !== -1) {
                    const mismatchedProduct = item.items[mismatchIndex];
                    return {
                        isValid: false,
                        errorMessage: `Received quantity must exactly match the purchase order: Product "${mismatchedProduct[0]}" (${quantities[mismatchIndex]}/${mismatchedProduct[1]}). Please click Reject and specify a reason.`
                    };
                }
                return { isValid: true };
            },
            submitUrl: (item) => '/api/staff/receives/' + item.rawId + '/accept',
            submitPayload: (item, quantities, notes) => ({
                items: item.items.map((prod, i) => ({
                    itemId: prod[2],
                    countedQty: quantities[i],
                    isCorrectProduct: true
                })),
                notes: notes
            }),
            submitBtnSavingText: 'Saving to inventory...',
            submitErrorMessage: 'Failed to submit receiving: '
        },
        step3: {
            renderResult: (panel, item, quantities, el) => {
                el('h3', 'Receipt saved and inventory updated successfully!', panel);
                el('p', 'Purchase Order ' + item.id + ' (' + item.reference + ') has been marked as RECEIVED.', panel);
                el('p', quantities.reduce((sum, q) => sum + q, 0) + ' units have been added to warehouse stock.', panel);
            }
        },
        reject: {
            submitUrl: (item) => '/api/staff/receives/' + item.rawId + '/reject',
            successAlert: (item) => 'Purchase Order ' + item.id + ' has been rejected.'
        }
    });
})();
