/**
 * Stock Out Workflow
 * Configures StaffWizard for outbound sales order picking.
 */
(function () {
    'use strict';

    window.StaffWizard.init({
        prefix: 'issue',
        fetchUrl: '/api/staff/stock-out/pending',
        selectRequestError: 'Select an available approved sales order to continue.',
        mapItem: (so) => ({
            id: so.code || ('SO-' + so.id),
            rawId: so.id,
            reference: so.reference || ('SO-' + so.id),
            requester: so.requester || 'Admin',
            customer: so.partyName || 'Customer',
            date: so.date || '—',
            items: (so.items || []).map(it => [it.productName, it.expectedQty, it.itemId, it.zoneName])
        }),
        subtitles: [
            'Select an approved sales order to begin stock out',
            'Check quantities against the products picked',
            'Review and confirm picking items out of inventory',
            'Stock out is complete'
        ],
        nextBtnText: [
            'Continue to Verify →',
            'Continue to Confirm →',
            'Confirm Stock Out →',
            'Stock Out Another →'
        ],
        step0: {
            heading: 'Available Approved Sales Orders (From Manager)',
            loadingText: 'Loading approved sales orders from manager...',
            headers: ['Order ID', 'Reference', 'Customer', 'Created By', 'Date', 'Items', 'Status', 'Action', 'Select'],
            renderRowValues: (item) => [item.id, item.reference, item.customer, item.requester, item.date, item.items.length],
            statusBadge: { text: 'Approved', className: 'badge-status badge-approved' },
            emptyText: 'No approved sales orders awaiting stock out. Waiting for manager approval.'
        },
        renderPanelHeader: (panel, item, el) => {
            el('h3', item.id + ' • ' + item.reference, panel);
            el('p', 'Customer: ' + item.customer + ' | Created by: ' + item.requester + ' (' + item.date + ')', panel);
            el('p', 'Status: Approved by Manager', panel);
        },
        step1: {
            headers: ['Product', 'Zone', 'Approved Quantity', 'Stock out quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
                el('td', product[0], row);
                el('td', product[3] || 'Default Zone', row);
                el('td', String(product[1]), row);
                const cell = el('td', undefined, row);
                const input = el('input', undefined, cell);
                input.type = 'number';
                input.min = '1';
                input.max = String(product[1]);
                input.step = '1';
                input.required = true;
                input.value = quantities[i] !== undefined ? quantities[i] : product[1];
                input.className = 'issue-quantity';
                input.setAttribute('aria-label', 'Stock out quantity for ' + product[0]);
                input.addEventListener('input', () => { quantities[i] = Number(input.value); });
            },
            notesLabel: 'Stock out notes (optional)',
            notesPlaceholder: 'Purpose of stock out or picking instructions',
            validate: (item, quantities) => {
                if (!quantities.every((q, i) => Number.isInteger(q) && q >= 1 && q <= item.items[i][1])) {
                    return { isValid: false, errorMessage: 'Stock out quantity must be between 1 and the approved quantity.' };
                }
                return { isValid: true };
            }
        },
        step2: {
            headers: ['Product', 'Zone', 'Approved Quantity', 'Stock out quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
                el('td', product[0], row);
                el('td', product[3] || 'Default Zone', row);
                el('td', String(product[1]), row);
                el('td', String(quantities[i]), row);
            },
            confirmCheckboxLabel: 'I have picked and verified the goods. Deduct these items from warehouse stock now.',
            submitUrl: (item) => '/api/staff/stock-out/' + item.rawId + '/confirm',
            submitPayload: (item, quantities, notes) => ({
                items: item.items.map((prod, i) => ({
                    itemId: prod[2],
                    countedQty: quantities[i],
                    isCorrectProduct: true
                })),
                notes: notes
            }),
            submitBtnSavingText: 'Updating inventory...',
            submitErrorMessage: 'Failed to confirm stock out: '
        },
        step3: {
            renderResult: (panel, item, quantities, el) => {
                el('h3', 'Stock out confirmed and inventory deducted successfully!', panel);
                el('p', 'Sales Order ' + item.id + ' (' + item.reference + ') has been marked as STOCK OUT.', panel);
                el('p', quantities.reduce((sum, q) => sum + q, 0) + ' units have been deducted from warehouse stock.', panel);
            }
        },
        reject: {
            submitUrl: (item) => '/api/staff/stock-out/' + item.rawId + '/reject',
            successAlert: (item) => 'Sales Order ' + item.id + ' has been rejected.'
        }
    });
})();
