/**
 * Transfer Product Workflow
 * Configures StaffWizard for stock movement verification across zones.
 */
(function () {
    'use strict';

    window.StaffWizard.init({
        prefix: 'transfer',
        fetchUrl: '/api/staff/transfers/pending',
        selectRequestError: 'Select an available approved transfer request to continue.',
        mapItem: (tr) => ({
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
        }),
        subtitles: [
            'Select an approved transfer request to begin verification',
            'Verify physical item relocation between zones',
            'Review and confirm zone transfer',
            'Transfer verification is complete'
        ],
        nextBtnText: [
            'Continue to Verify →',
            'Continue to Confirm →',
            'Confirm Transfer →',
            'Transfer Another →'
        ],
        step0: {
            heading: 'Available Approved Stock Movements (From Manager)',
            loadingText: 'Loading approved stock movements from manager...',
            headers: ['Transfer ID', 'Reference', 'Product', 'From Zone', 'To Zone', 'Quantity', 'Date', 'Status', 'Action', 'Select'],
            renderRowValues: (item) => [item.id, item.reference, item.productName, item.fromZone, item.toZone, item.quantity, item.date],
            statusBadge: { text: 'Pending Verification', className: 'badge-status badge-pending' },
            emptyText: 'No approved stock movements awaiting verification. Waiting for manager approval.'
        },
        renderPanelHeader: (panel, item, el) => {
            el('h3', item.id + ' • ' + item.reference, panel);
            el('p', 'Relocation: ' + item.fromZone + ' → ' + item.toZone + ' | Requested by: ' + item.requester + ' (' + item.date + ')', panel);
            el('p', 'Status: Approved by Manager (Awaiting Physical Verification)', panel);
        },
        step1: {
            headers: ['Product', 'From Zone → To Zone', 'Expected Quantity', 'Verified Quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
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
            },
            notesLabel: 'Transfer verification notes (optional)',
            notesPlaceholder: 'Physical relocation notes or bay/shelf details',
            validate: (item, quantities) => {
                if (!quantities.every((q, i) => Number.isInteger(q) && q >= 1 && q <= item.items[i][1])) {
                    return { isValid: false, errorMessage: 'Verified quantity must be between 1 and the transfer quantity.' };
                }
                return { isValid: true };
            }
        },
        step2: {
            headers: ['Product', 'From Zone → To Zone', 'Expected Quantity', 'Verified Quantity'],
            renderItemRow: (row, product, i, quantities, el) => {
                el('td', product[0], row);
                el('td', product[3], row);
                el('td', String(product[1]), row);
                el('td', String(quantities[i]), row);
            },
            confirmCheckboxLabel: 'I have physically verified and relocated the goods to the destination zone. Update warehouse location now.',
            confirmRequiredError: 'Please confirm that you have physically relocated the items.',
            submitUrl: (item) => '/api/staff/transfers/' + item.rawId + '/verify',
            submitPayload: (item, quantities, notes) => ({
                items: [{
                    countedQty: quantities[0]
                }],
                notes: notes
            }),
            submitBtnSavingText: 'Updating zone location...',
            submitErrorMessage: 'Failed to verify transfer: '
        },
        step3: {
            renderResult: (panel, item, quantities, el) => {
                el('h3', 'Stock movement verified and completed successfully!', panel);
                el('p', 'Transfer ' + item.id + ' (' + item.reference + ') has been completed.', panel);
                el('p', item.productName + ' location has been updated from ' + item.fromZone + ' to ' + item.toZone + '.', panel);
            }
        },
        reject: {
            submitUrl: (item) => '/api/staff/transfers/' + item.rawId + '/reject',
            successAlert: (item) => 'Stock Transfer #' + item.id + ' has been rejected.'
        }
    });
})();
