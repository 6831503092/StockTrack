/* Manager-only navigation extension; shared sidebar remains unchanged. */
(() => {
    const container = document.getElementById('sidebar-container');
    if (!container || container.dataset.role !== 'MANAGER') return;
    const mount = () => {
        const nav = container.querySelector('#sidebar-nav-list');
        const dashboard = nav?.querySelector('[data-nav-id="dashboard"]');
        if (!dashboard) return;
        dashboard.href = 'dashboard.html';
        if (nav.querySelector('[data-nav-id="requests"]')) return;
        const link = dashboard.cloneNode(true);
        link.href = 'requests.html';
        link.dataset.navId = 'requests';
        link.dataset.tooltip = 'View Requests';
        link.classList.toggle('active', container.dataset.activeMenu === 'requests');
        link.querySelectorAll('span').forEach(span => { span.textContent = 'View Requests'; });
        link.setAttribute('aria-label', 'View Requests');
        const icon = link.querySelector('.sidebar-icon-box');
        if (icon) icon.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><path d="M8 13h8M8 17h5"></path></svg>';
        nav.append(link);
    };
    new MutationObserver(mount).observe(container, { childList: true, subtree: true });
    mount();
})();