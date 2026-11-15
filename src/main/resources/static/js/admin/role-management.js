/**
 * StockTrack - Admin Role Management
 * Handles user listing, KPI summary calculation, filtering, new user creation, and role change requests.
 */
(function () {
    // 1. Guard access
    if (window.Auth) {
        window.Auth.requireRole('ADMIN');
    } else if (window.AdminApp) {
        window.AdminApp.checkAdminAccess();
    }

    const ROLE_META = {
        ADMIN: { roleClass: 'role-admin', roleLabel: 'Admin' },
        MANAGER: { roleClass: 'role-manager', roleLabel: 'Manager' },
        STAFF: { roleClass: 'role-staff', roleLabel: 'Staff' }
    };

    // DOM Elements Cache
    const searchInput = document.getElementById('role-search-input');
    const roleFilterSelect = document.getElementById('role-filter-select');
    const paginationInfo = document.getElementById('role-pagination-info');
    const tableBody = document.getElementById('role-table-body');
    const formCreateUser = document.getElementById('form-create-user');
    const formChangeRole = document.getElementById('form-change-role');

    const kpiTotal = document.getElementById('kpi-total-users');
    const kpiAdmin = document.getElementById('kpi-admin-users');
    const kpiStaff = document.getElementById('kpi-staff-users');
    const kpiManager = document.getElementById('kpi-manager-users');

    let currentUsers = [];

    // Helper shortcuts
    const escapeHtml = (window.Format && window.Format.escapeHtml) || (window.AdminApp && window.AdminApp.escapeHtml) || (s => String(s || ''));
    const openModal = (id) => (window.UI ? window.UI.openModal(id) : window.AdminApp.openModal(id));
    const closeModal = (id) => (window.UI ? window.UI.closeModal(id) : window.AdminApp.closeModal(id));
    const showCenterAlert = (opts) => (window.UI ? window.UI.showCenterAlert(opts) : window.AdminApp.showCenterAlert(opts));

    // Custom Date Picker
    if (window.AdminApp && window.AdminApp.initCustomDatePicker) {
        window.AdminApp.initCustomDatePicker({
            box: 'requests-date-box',
            onChange: filterRoles
        });
    }

    // Load & Render Users
    async function loadUsers() {
        try {
            const res = await window.api.get('/api/users');
            currentUsers = res?.data || [];
            renderKPIs(currentUsers);
            renderUserTable(currentUsers);
        } catch (err) {
            console.error('Failed to load users:', err);
            if (tableBody) tableBody.innerHTML = '';
            if (paginationInfo) paginationInfo.textContent = 'Showing 0 of 0 entries';
        }
    }

    function getVisibleUsers(users) {
        return (users || []).filter(u => (u.role || '').toUpperCase() !== 'OWNER');
    }

    function renderKPIs(users) {
        const visible = getVisibleUsers(users);
        const countByRole = (role) => visible.filter(u => (u.role || '').toUpperCase() === role).length;

        if (kpiTotal) kpiTotal.textContent = visible.length;
        if (kpiAdmin) kpiAdmin.textContent = countByRole('ADMIN');
        if (kpiStaff) kpiStaff.textContent = countByRole('STAFF');
        if (kpiManager) kpiManager.textContent = countByRole('MANAGER');
    }

    function renderUserTable(users) {
        if (!tableBody) return;

        const visibleUsers = getVisibleUsers(users);

        if (!visibleUsers.length) {
            tableBody.innerHTML = '';
            if (paginationInfo) paginationInfo.textContent = 'Showing 0 of 0 entries';
            return;
        }

        const currentUser = (window.Auth && window.Auth.getUser()) || (window.api && window.api.getCurrentUser()) || {};

        tableBody.innerHTML = visibleUsers.map(user => {
            const roleUpper = (user.role || '').toUpperCase();
            const { roleClass, roleLabel } = ROLE_META[roleUpper] || ROLE_META.STAFF;
            const isSelf = currentUser && currentUser.userId === user.userId;
            const safeName = escapeHtml(user.fullName || '');

            const actionHtml = isSelf
                ? '<span class="cell-subtext">Current User</span>'
                : `<button type="button" class="btn-change-role-action" onclick="openChangeRoleModal(${user.userId}, '${safeName}', '${roleLabel}')">Change Role</button>`;

            return `
                <tr class="role-row" data-user-id="${user.userId}" data-user-name="${safeName}" data-role="${roleLabel}">
                    <td class="cell-bold">${user.userId}</td>
                    <td><div>${escapeHtml(user.fullName || '-')}</div></td>
                    <td><span class="badge-role ${roleClass}">${roleLabel}</span></td>
                    <td class="cell-subtext">-</td>
                    <td>${actionHtml}</td>
                </tr>
            `;
        }).join('');

        filterRoles();
    }

    // Form state checks
    function checkCreateUserFormState() {
        const btn = document.querySelector('#form-create-user button[type="submit"]');
        if (!btn) return;
        const fullName = (document.getElementById('new-fullname')?.value || '').trim();
        const role = (document.getElementById('new-role')?.value || '').trim();
        const password = document.getElementById('new-password')?.value || '';
        btn.disabled = !fullName || !role || !password;
    }

    function checkChangeRoleFormState() {
        const btn = document.querySelector('#form-change-role button[type="submit"]');
        if (!btn) return;
        const currentRole = (document.getElementById('modal-current-role')?.value || '').trim().toUpperCase();
        const newRole = (document.getElementById('modal-new-role')?.value || '').trim().toUpperCase();
        btn.disabled = !newRole || newRole === currentRole;
    }

    ['new-fullname', 'new-role', 'new-password'].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', checkCreateUserFormState);
            el.addEventListener('change', checkCreateUserFormState);
        }
    });

    document.getElementById('modal-new-role')?.addEventListener('change', checkChangeRoleFormState);

    // Modal actions
    function openCreateUserModal() {
        if (formCreateUser) formCreateUser.reset();
        checkCreateUserFormState();
        openModal('modal-create-user');
    }

    function closeCreateUserModal() {
        closeModal('modal-create-user');
    }

    document.getElementById('btn-open-create-user-modal')?.addEventListener('click', openCreateUserModal);

    async function handleCreateUserSubmit(e) {
        if (e && e.preventDefault) e.preventDefault();
        const fullName = (document.getElementById('new-fullname')?.value || '').trim();
        const role = document.getElementById('new-role')?.value;
        const password = document.getElementById('new-password')?.value;
        const reason = (document.getElementById('new-reason')?.value || '').trim();

        if (!fullName || !role || !password) {
            return showCenterAlert({
                title: 'Validation Error',
                message: 'Please fill in Full Name, Role, and Password.',
                type: 'error'
            });
        }

        try {
            const res = await window.api.post('/api/requests/user-create', { fullName, role, password, reason });
            closeCreateUserModal();
            loadUsers();

            showCenterAlert({
                title: 'Request Submitted!',
                message: res.message || `User creation request for "${fullName}" submitted successfully for approval.`,
                type: 'success'
            });
        } catch (err) {
            showCenterAlert({
                title: 'Failed to Submit',
                message: err.message || 'Failed to submit user creation request',
                type: 'error'
            });
        }
    }

    function openChangeRoleModal(userId, userName, currentRole) {
        const titleEl = document.getElementById('modal-role-title');
        if (titleEl) titleEl.textContent = 'Change User Role';

        const uidEl = document.getElementById('modal-user-id');
        if (uidEl) uidEl.value = userId;

        const unameEl = document.getElementById('modal-user-name');
        if (unameEl) unameEl.value = userName;

        const curRoleEl = document.getElementById('modal-current-role');
        if (curRoleEl) curRoleEl.value = currentRole;

        const reasonEl = document.getElementById('modal-role-reason');
        if (reasonEl) reasonEl.value = '';

        const newRoleEl = document.getElementById('modal-new-role');
        if (newRoleEl) newRoleEl.value = currentRole === 'Staff' ? 'Manager' : 'Staff';

        checkChangeRoleFormState();
        openModal('modal-change-role');
    }

    function closeChangeRoleModal() {
        closeModal('modal-change-role');
    }

    async function handleChangeRoleSubmit(e) {
        if (e && e.preventDefault) e.preventDefault();
        const userId = parseInt(document.getElementById('modal-user-id')?.value, 10);
        const userName = document.getElementById('modal-user-name')?.value || 'User';
        const currentRole = (document.getElementById('modal-current-role')?.value || '').trim();
        const newRole = document.getElementById('modal-new-role')?.value;
        const reason = (document.getElementById('modal-role-reason')?.value || '').trim();

        if (!newRole || newRole.trim().toUpperCase() === currentRole.toUpperCase()) {
            return showCenterAlert({
                title: 'No Changes Detected',
                message: 'Please select a new role different from the current role before sending a request.',
                type: 'warning'
            });
        }

        try {
            const res = await window.api.post('/api/requests/role-change', { userId, newRole, reason });
            closeChangeRoleModal();

            showCenterAlert({
                title: 'Request Submitted!',
                message: res.message || `Role change request for ${userName} to ${newRole} submitted successfully for approval.`,
                type: 'success'
            });
        } catch (err) {
            showCenterAlert({
                title: 'Failed to Submit',
                message: err.message || 'Failed to submit role change request',
                type: 'error'
            });
        }
    }

    function filterRoles() {
        const rows = document.querySelectorAll('.role-row');
        if (!rows.length) {
            if (paginationInfo) paginationInfo.textContent = 'Showing 0 of 0 entries';
            return;
        }

        const query = (searchInput?.value || '').toLowerCase().trim();
        const selectedRole = roleFilterSelect?.value || 'ALL';
        let visibleCount = 0;

        rows.forEach(row => {
            const uid = (row.getAttribute('data-user-id') || '').toLowerCase();
            const uname = (row.getAttribute('data-user-name') || '').toLowerCase();
            const urole = (row.getAttribute('data-role') || '').toLowerCase();

            const isMatch = (!query || uid.includes(query) || uname.includes(query)) &&
                (selectedRole === 'ALL' || urole === selectedRole.toLowerCase());

            row.style.display = isMatch ? '' : 'none';
            if (isMatch) visibleCount++;
        });

        if (paginationInfo) {
            paginationInfo.textContent = `Showing ${visibleCount} of ${rows.length} entries`;
        }
    }

    if (searchInput) searchInput.addEventListener('input', filterRoles);
    if (roleFilterSelect) roleFilterSelect.addEventListener('change', filterRoles);

    // Global exposure for inline HTML attributes / onclick handlers
    window.openCreateUserModal = openCreateUserModal;
    window.closeCreateUserModal = closeCreateUserModal;
    window.handleCreateUserSubmit = handleCreateUserSubmit;
    window.openChangeRoleModal = openChangeRoleModal;
    window.closeChangeRoleModal = closeChangeRoleModal;
    window.handleChangeRoleSubmit = handleChangeRoleSubmit;
    window.loadUsers = loadUsers;
    window.filterRoles = filterRoles;

    // Initial load
    loadUsers();
})();
