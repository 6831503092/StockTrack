/**
 * StockTrack - Sidebar Role Configuration & Menu Definitions
 * Separates navigation data and inline SVGs from sidebar runtime logic.
 */
(function () {
    const DASHBOARD_ICON = `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="2.5"></rect><rect x="13" y="3" width="8" height="8" rx="2.5"></rect><rect x="3" y="13" width="8" height="8" rx="2.5"></rect><rect x="13" y="13" width="8" height="8" rx="2.5"></rect></svg>`;

    const ROLE_CONFIGS = {
        ADMIN: {
            title: 'Admin',
            roleClass: 'role-admin',
            defaultUserId: '503092',
            defaultFullName: 'Admin User',
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&h=120&q=80',
            homeUrl: 'my-requests.html',
            menus: [
                {
                    id: 'my-requests',
                    title: 'My requests',
                    url: 'my-requests.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`
                },
                {
                    id: 'role-management',
                    title: 'Role Management',
                    url: 'role-management.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`
                },
                {
                    id: 'zone-product',
                    title: 'Zone & Product',
                    url: 'zone-product.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>`
                },
                {
                    id: 'purchase-order',
                    title: 'Purchase Order',
                    url: 'purchase-order.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24"><text x="12" y="17.5" text-anchor="middle" font-family="'Plus Jakarta Sans', system-ui, -apple-system, sans-serif" font-size="15.5" font-weight="800" fill="currentColor" letter-spacing="-0.5">PO</text></svg>`
                },
                {
                    id: 'sales-order',
                    title: 'Sales Order',
                    url: 'sales-order.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24"><text x="12" y="17.5" text-anchor="middle" font-family="'Plus Jakarta Sans', system-ui, -apple-system, sans-serif" font-size="15.5" font-weight="800" fill="currentColor" letter-spacing="-0.5">SO</text></svg>`
                }
            ]
        },
        MANAGER: {
            title: 'Manager',
            roleClass: 'role-manager',
            defaultUserId: '503069',
            defaultFullName: 'Manager User',
            avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=120&h=120&q=80',
            homeUrl: 'dashboard.html',
            menus: [
                {
                    id: 'dashboard',
                    title: 'Dashboard',
                    url: 'dashboard.html',
                    icon: DASHBOARD_ICON
                },
                {
                    id: 'requests',
                    title: 'View Requests',
                    url: 'requests.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`
                }
            ]
        },
        OWNER: {
            title: 'Owner',
            roleClass: 'role-owner',
            defaultUserId: '503107',
            defaultFullName: 'Owner User',
            avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=120&h=120&q=80',
            homeUrl: 'dashboard.html',
            menus: [
                {
                    id: 'dashboard',
                    title: 'Dashboard',
                    url: 'dashboard.html',
                    icon: DASHBOARD_ICON
                },
                {
                    id: 'activity-log',
                    title: 'Activity Log',
                    url: 'activity-log.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 3-6.7"></path><path d="M3 4v5h5"></path><path d="M12 7v5l3 2"></path></svg>`
                }
            ]
        },
        STAFF: {
            title: 'Staff',
            roleClass: 'role-staff',
            defaultUserId: '503080',
            defaultFullName: 'Staff User',
            avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=120&h=120&q=80',
            homeUrl: 'dashboard.html',
            menus: [
                {
                    id: 'dashboard',
                    title: 'Dashboard',
                    url: 'dashboard.html',
                    icon: DASHBOARD_ICON
                },
                {
                    id: 'receive',
                    title: 'Receive Items',
                    url: 'receive-items.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 16V4m0 0L7 9m5-5 5 5M5 14v5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-5"></path></svg>`
                },
                {
                    id: 'stock-out',
                    title: 'Stock Out',
                    url: 'stock-out.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v12m0 0 5-5m-5 5-5-5M5 18v1a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-1"></path></svg>`
                },
                {
                    id: 'transfer',
                    title: 'Transfer Product',
                    url: 'transfer-items.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8h14m0 0-4-4m4 4-4 4M20 16H6m0 0 4-4m-4 4 4 4"></path></svg>`
                }
            ]
        }
    };

    window.STOCKTRACK_ROLE_CONFIGS = ROLE_CONFIGS;
})();
