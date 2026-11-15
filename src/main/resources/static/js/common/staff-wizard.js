/**
 * Shared Staff Workflow Wizard for StockTrack
 * Powers receive-items, stock-out, and transfer-items multi-step workflows.
 */
(function (global) {
    'use strict';

    function init(config) {
        const user = (global.Auth && global.Auth.getUser) ? global.Auth.getUser() : (global.api && global.api.getCurrentUser());
        if (!user || (user.role !== 'STAFF' && user.role !== 'MANAGER' && user.role !== 'ADMIN')) {
            window.location.replace('../index.html');
            return;
        }

        const prefix = config.prefix;
        let requests = [];
        let step = 0, selected = '', quantities = [], notes = '', confirmed = false;

        const content = document.getElementById(`${prefix}-stage-content`);
        const alert = document.getElementById(`${prefix}-alert`);
        const next = document.getElementById(`${prefix}-next`);
        const back = document.getElementById(`${prefix}-back`);
        const rejectBtn = document.getElementById(`${prefix}-reject`);
        const rejectModal = document.getElementById(`${prefix}-reject-modal`);
        const rejectForm = document.getElementById(`${prefix}-reject-form`);
        const rejectReason = document.getElementById(`${prefix}-reject-reason`);
        const rejectError = document.getElementById(`${prefix}-reject-error`);
        const rejectCancel = document.getElementById(`${prefix}-reject-cancel`);
        const rejectConfirm = document.getElementById(`${prefix}-reject-confirm`);
        const actions = document.getElementById('workflow-actions') || document.querySelector('.workflow-actions');
        const titles = config.titles || ['Requests', 'Verify', 'Confirm', 'Result'];

        function el(tag, text, parent) {
            const node = document.createElement(tag);
            if (text !== undefined) node.textContent = text;
            if (parent) parent.appendChild(node);
            return node;
        }

        function showError(text) {
            if (alert) {
                alert.textContent = text;
                alert.hidden = false;
            }
        }

        function getSelectedRequest() {
            return requests.find(item => item.id === selected);
        }

        function choose(id) {
            selected = id;
            const cur = getSelectedRequest();
            quantities = cur ? cur.items.map(item => item[1]) : [];
            notes = '';
            confirmed = false;
            if (alert) alert.hidden = true;
        }

        function createTable(headers) {
            const wrapper = el('div', undefined, content);
            wrapper.className = `${prefix}-table-scroll requests-table-wrapper`;
            const tbl = el('table', undefined, wrapper);
            tbl.className = 'requests-table';
            const row = el('tr', undefined, el('thead', undefined, tbl));
            headers.forEach(text => {
                const th = el('th', text, row);
                th.scope = 'col';
            });
            return el('tbody', undefined, tbl);
        }

        function focusTitle() {
            document.getElementById(`${prefix}-stage-title`)?.focus();
        }

        async function loadRequests() {
            if (!content) return;
            content.replaceChildren();
            el('p', config.step0.loadingText || 'Loading requests...', content);
            try {
                const res = await global.api.get(config.fetchUrl);
                const data = (res && res.data) ? res.data : (Array.isArray(res) ? res : []);
                requests = data.map(config.mapItem);
                if (requests.length > 0 && !selected) {
                    choose(requests[0].id);
                }
            } catch (err) {
                showError((config.loadErrorMessage || 'Failed to load pending requests from server: ') + (err.message || err));
                requests = [];
            }
            render();
        }

        function render() {
            if (!content) return;
            content.replaceChildren();
            actions?.querySelector(`.${prefix}-confirm-check`)?.remove();

            const stageTitle = document.getElementById(`${prefix}-stage-title`);
            const stageLabel = document.getElementById(`${prefix}-stage-label`);
            const subtitle = document.getElementById(`${prefix}-subtitle`);

            if (stageTitle) stageTitle.textContent = titles[step];
            if (stageLabel) stageLabel.textContent = titles[step].toUpperCase();
            if (subtitle && config.subtitles) {
                subtitle.textContent = config.subtitles[step];
            }

            document.querySelectorAll(`.${prefix}-steps li`).forEach((node, i) => {
                node.removeAttribute('aria-current');
                if (i === step) node.setAttribute('aria-current', 'step');
                node.classList.toggle('complete', i < step);
            });

            if (back) back.hidden = step === 0 || step === 3;
            if (rejectBtn) rejectBtn.hidden = (step === 0 || step === 3);
            if (next) {
                next.hidden = (step === 0 && !requests.length);
                if (config.nextBtnText) {
                    next.textContent = config.nextBtnText[step];
                }
            }

            // Step 0: Requests List
            if (step === 0) {
                el('h3', config.step0.heading, content);
                const body = createTable(config.step0.headers);
                requests.forEach(item => {
                    const row = el('tr', undefined, body);
                    const cellValues = config.step0.renderRowValues(item);
                    cellValues.forEach(value => el('td', value, row));

                    const statusTd = el('td', undefined, row);
                    statusTd.style.textAlign = 'center';
                    const badge = el('span', config.step0.statusBadge.text, statusTd);
                    badge.className = config.step0.statusBadge.className;

                    const actionTd = el('td', undefined, row);
                    actionTd.style.textAlign = 'center';
                    const actionGroup = el('div', undefined, actionTd);
                    actionGroup.className = 'action-buttons-group';
                    const open = el('button', 'Detail', actionGroup);
                    open.type = 'button';
                    open.className = `${prefix}-open btn-action-detail`;
                    open.setAttribute('aria-label', 'Open ' + item.id);
                    open.addEventListener('click', () => {
                        choose(item.id);
                        step = 1;
                        render();
                        focusTitle();
                    });

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
                    const cell = el('td', config.step0.emptyText, el('tr', undefined, body));
                    cell.colSpan = config.step0.headers.length;
                    cell.style.textAlign = 'center';
                    cell.style.padding = '32px';
                }
                return;
            }

            const item = getSelectedRequest();
            if (!item) {
                step = 0;
                render();
                return;
            }

            // Step 3: Result
            if (step === 3) {
                const panel = el('div', undefined, content);
                panel.className = `${prefix}-details`;
                config.step3.renderResult(panel, item, quantities, el);

                const nav = el('div', undefined, panel);
                nav.style.cssText = 'margin-top: 20px; display: flex; gap: 12px;';
                const dashLink = el('a', '← Back to Staff Dashboard', nav);
                dashLink.href = 'dashboard.html';
                dashLink.className = 'btn-secondary';
                dashLink.style.cssText = 'display: inline-flex; align-items: center; padding: 10px 18px; border-radius: 8px; text-decoration: none; font-size: 13px; font-weight: 600; border: 1px solid #cbd5e1; color: #475569; background: #fff;';
                return;
            }

            // Details Header Panel for Step 1 & 2
            const panel = el('div', undefined, content);
            panel.className = `${prefix}-details`;
            config.renderPanelHeader(panel, item, el);

            // Step 1: Verification Form
            if (step === 1) {
                if (config.step1.renderNotice) {
                    config.step1.renderNotice(content, el);
                }

                const body = createTable(config.step1.headers);
                item.items.forEach((product, i) => {
                    const row = el('tr', undefined, body);
                    config.step1.renderItemRow(row, product, i, quantities, el, item);
                });

                const label = el('label', config.step1.notesLabel, content);
                label.className = `${prefix}-notes-label`;
                const textarea = el('textarea', undefined, label);
                textarea.rows = 3;
                textarea.maxLength = 1000;
                textarea.value = notes;
                textarea.placeholder = config.step1.notesPlaceholder;
                textarea.addEventListener('input', () => { notes = textarea.value; });
            }

            // Step 2: Confirm Screen
            if (step === 2) {
                const body = createTable(config.step2.headers);
                item.items.forEach((product, i) => {
                    const row = el('tr', undefined, body);
                    config.step2.renderItemRow(row, product, i, quantities, el, item);
                });

                if (notes) el('p', 'Notes: ' + notes, panel);

                const label = el('label', undefined);
                label.className = `${prefix}-confirm-check`;
                const check = el('input', undefined, label);
                check.type = 'checkbox';
                check.checked = confirmed;
                check.addEventListener('change', () => { confirmed = check.checked; });
                el('span', config.step2.confirmCheckboxLabel, label);

                if (back && back.parentNode === actions) {
                    actions.insertBefore(label, back);
                } else if (actions) {
                    actions.appendChild(label);
                }
            }
        }

        // Navigation Handlers
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

                const item = getSelectedRequest();
                if (!item) {
                    showError(config.selectRequestError || 'Select an available approved request to continue.');
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

                    if (config.step1.validate) {
                        const validation = config.step1.validate(item, quantities, content);
                        if (!validation.isValid) {
                            showError(validation.errorMessage);
                            return;
                        }
                    }

                    step = 2;
                    render();
                    focusTitle();
                    return;
                }

                if (step === 2) {
                    if (!confirmed) {
                        showError(config.step2.confirmRequiredError || 'Please confirm that you have checked the goods and quantities.');
                        return;
                    }

                    if (config.step2.validate) {
                        const validation = config.step2.validate(item, quantities);
                        if (!validation.isValid) {
                            showError(validation.errorMessage);
                            return;
                        }
                    }

                    next.disabled = true;
                    next.textContent = config.step2.submitBtnSavingText || 'Saving...';
                    try {
                        const payload = config.step2.submitPayload(item, quantities, notes);
                        await global.api.post(config.step2.submitUrl(item), payload);
                        step = 3;
                        render();
                        focusTitle();
                    } catch (err) {
                        showError((config.step2.submitErrorMessage || 'Failed to submit: ') + (err.message || err));
                    } finally {
                        next.disabled = false;
                        if (config.nextBtnText) {
                            next.textContent = config.nextBtnText[step];
                        }
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

        // Rejection Modal Handlers
        if (rejectBtn && rejectModal) {
            rejectBtn.addEventListener('click', () => {
                if (rejectReason) rejectReason.value = '';
                if (rejectError) {
                    rejectError.textContent = '';
                    rejectError.hidden = true;
                }
                rejectModal.showModal();
                rejectReason?.focus();
            });

            if (rejectCancel) {
                rejectCancel.addEventListener('click', () => {
                    rejectModal.close();
                });
            }

            if (rejectForm) {
                rejectForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const reason = rejectReason ? rejectReason.value.trim() : '';
                    if (!reason) {
                        if (rejectError) {
                            rejectError.textContent = 'กรุณาระบุเหตุผลในการปฏิเสธ (Reject reason is required)';
                            rejectError.hidden = false;
                        }
                        rejectReason?.focus();
                        return;
                    }

                    const item = getSelectedRequest();
                    if (!item) return;

                    if (rejectConfirm) {
                        rejectConfirm.disabled = true;
                        rejectConfirm.textContent = 'Rejecting...';
                    }

                    try {
                        await global.api.post(config.reject.submitUrl(item), { reason: reason });
                        rejectModal.close();
                        if (alert) {
                            alert.textContent = config.reject.successAlert(item);
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
                        if (rejectError) {
                            rejectError.textContent = 'Failed to reject: ' + (err.message || err);
                            rejectError.hidden = false;
                        }
                    } finally {
                        if (rejectConfirm) {
                            rejectConfirm.disabled = false;
                            rejectConfirm.textContent = 'Confirm Reject';
                        }
                    }
                });
            }
        }

        // Connection status indicator
        function initConnection() {
            const status = document.getElementById(`${prefix}-connection`);
            if (!status) return;
            function update() {
                const dot = document.createElement('span');
                dot.setAttribute('aria-hidden', 'true');
                status.classList.toggle('offline', !navigator.onLine);
                status.replaceChildren(dot, document.createTextNode(navigator.onLine ? 'Online' : 'Offline'));
            }
            window.addEventListener('online', update);
            window.addEventListener('offline', update);
            update();
        }
        initConnection();

        // Initial load
        loadRequests();
    }

    global.StaffWizard = { init: init };
})(window);
