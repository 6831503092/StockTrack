/**
 * StockTrack - Shared Sidebar Navigation Component Script
 * Handles dynamic role rendering, zero-shift hover expansion, active states, and unified logout.
 */
(function () {
    // Role Menu Configurations & Defaults
    const ROLE_CONFIGS = {
        ADMIN: {
            title: 'Admin',
            roleClass: 'role-admin',
            defaultUserId: '503092',
            defaultFullName: 'Admin User',
            avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&h=120&q=80',
            homeUrl: 'dashboard.html',
            menus: [
                {
                    id: 'dashboard',
                    title: 'Dashboard',
                    url: '#dashboard',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="2.5"></rect><rect x="13" y="3" width="8" height="8" rx="2.5"></rect><rect x="3" y="13" width="8" height="8" rx="2.5"></rect><rect x="13" y="13" width="8" height="8" rx="2.5"></rect></svg>`
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
                    url: '#dashboard',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="2.5"></rect><rect x="13" y="3" width="8" height="8" rx="2.5"></rect><rect x="3" y="13" width="8" height="8" rx="2.5"></rect><rect x="13" y="13" width="8" height="8" rx="2.5"></rect></svg>`
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
                    url: '#dashboard',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="2.5"></rect><rect x="13" y="3" width="8" height="8" rx="2.5"></rect><rect x="3" y="13" width="8" height="8" rx="2.5"></rect><rect x="13" y="13" width="8" height="8" rx="2.5"></rect></svg>`
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
                    id: 'receive',
                    title: 'Receive Items',
                    url: 'receive-items.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"></path></svg>`
                },
                {
                    id: 'issue',
                    title: 'Issue Items',
                    url: 'issue-items.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12h14m-5-5 5 5-5 5m9-10v10"></path></svg>`
                },
                {
                    id: 'transfer',
                    title: 'Transfer Product',
                    url: 'transfer-items.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8h16m-4-4 4 4-4 4M20 16H4m4-4-4 4 4 4"></path></svg>`
                },
                {
                    id: 'dashboard',
                    title: 'Dashboard',
                    url: 'dashboard.html',
                    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="2.5"></rect><rect x="13" y="3" width="8" height="8" rx="2.5"></rect><rect x="3" y="13" width="8" height="8" rx="2.5"></rect><rect x="13" y="13" width="8" height="8" rx="2.5"></rect></svg>`
                }
            ]
        }
    };

    // Unified Logout Handler
    window.handleSidebarLogout = function (e) {
        if (e) e.preventDefault();
        fetch('/api/logout', {
            method: 'POST',
            credentials: 'include'
        }).catch(() => {}).finally(() => {
            try {
                localStorage.removeItem('stocktrack_user');
            } catch (err) {}
            window.location.href = '../index.html';
        });
    };

    // Load Sidebar into Container
    async function loadSidebar(container) {
        if (!container) return;

        // Determine current role & active menu
        let userRole = container.getAttribute('data-role');
        let activeMenu = container.getAttribute('data-active-menu') || 'dashboard';

        let storedUser = null;
        try {
            const raw = localStorage.getItem('stocktrack_user');
            if (raw) {
                storedUser = JSON.parse(raw);
                if (storedUser && storedUser.role && !userRole) {
                    userRole = storedUser.role.toUpperCase();
                }
            }
        } catch (err) {}

        // Infer role from current file path if not found
        if (!userRole) {
            const path = window.location.pathname.toLowerCase();
            if (path.includes('admin')) userRole = 'ADMIN';
            else if (path.includes('manager')) userRole = 'MANAGER';
            else if (path.includes('owner')) userRole = 'OWNER';
            else if (path.includes('staff')) userRole = 'STAFF';
            else userRole = 'ADMIN';
        }

        const roleConfig = ROLE_CONFIGS[userRole] || ROLE_CONFIGS.ADMIN;

        // Attempt to fetch sidebar.html
        let sidebarHtml = '';
        const fetchUrls = ['/sidebar.html', '../sidebar.html', 'sidebar.html'];

        for (const url of fetchUrls) {
            try {
                const res = await fetch(url);
                if (res.ok) {
                    sidebarHtml = await res.text();
                    break;
                }
            } catch (e) {
                // Ignore and try next path
            }
        }

        // Fallback HTML template if fetch fails or running directly via file://
        if (!sidebarHtml) {
            sidebarHtml = `
            <aside class="sidebar" id="app-sidebar">
                <a href="#" class="sidebar-logo" id="sidebar-logo" title="StockTrack">
                    <div class="sidebar-icon-box">
                        <svg class="logo-icon" width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                            <line x1="12" y1="22.08" x2="12" y2="12"></line>
                        </svg>
                    </div>
                    <span class="logo-text">StockTrack</span>
                </a>
                <div class="sidebar-divider"></div>
                <div class="sidebar-user" id="sidebar-user-container">
                    <div class="role-title" id="sidebar-role-title">Role</div>
                    <div class="sidebar-user-row">
                        <div class="sidebar-icon-box">
                            <img id="sidebar-avatar" src="" alt="User Profile" class="avatar">
                        </div>
                        <div class="sidebar-user-details">
                            <div class="user-detail-row">
                                <span class="detail-label">User ID:</span>
                                <span class="detail-val" id="sidebar-user-id">--</span>
                            </div>
                            <div class="user-detail-row">
                                <span class="detail-label">Full Name:</span>
                                <span class="detail-val" id="sidebar-user-fullname">--</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="sidebar-divider"></div>
                <div class="sidebar-menu-label">Menu</div>
                <nav class="sidebar-nav" id="sidebar-nav-list"></nav>
                <div class="sidebar-bottom">
                    <a href="../index.html" class="nav-logout" id="sidebar-logout-btn" title="Logout" onclick="handleSidebarLogout(event)">
                        <div class="sidebar-icon-box">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                                <polyline points="16 17 21 12 16 7"></polyline>
                                <line x1="21" y1="12" x2="9" y2="12"></line>
                            </svg>
                        </div>
                        <span class="nav-title">Logout</span>
                    </a>
                </div>
            </aside>`;
        }

        container.innerHTML = sidebarHtml;

        // Populate elements with role configuration
        const roleTitleEl = container.querySelector('#sidebar-role-title');
        if (roleTitleEl) {
            roleTitleEl.textContent = roleConfig.title;
            if (roleConfig.roleClass) {
                roleTitleEl.className = 'role-title ' + roleConfig.roleClass;
            }
        }

        const avatarEl = container.querySelector('#sidebar-avatar');
        if (avatarEl) {
            avatarEl.src = roleConfig.avatar;
            avatarEl.alt = `${roleConfig.title} Profile`;
        }

        const logoEl = container.querySelector('#sidebar-logo');
        if (logoEl) {
            logoEl.href = roleConfig.homeUrl;
        }

        // Populate user details (User ID & Full Name)
        const currentUserId = (storedUser && (storedUser.userId || storedUser.username)) 
            ? String(storedUser.userId || storedUser.username) 
            : roleConfig.defaultUserId;

        const currentFullName = (storedUser && storedUser.fullName) 
            ? storedUser.fullName 
            : roleConfig.defaultFullName;

        const userIdEl = container.querySelector('#sidebar-user-id');
        if (userIdEl) {
            userIdEl.textContent = currentUserId;
        }

        const userFullNameEl = container.querySelector('#sidebar-user-fullname');
        if (userFullNameEl) {
            userFullNameEl.textContent = currentFullName;
        }

        // Render Nav Items with icon-box & title
        const navListEl = container.querySelector('#sidebar-nav-list');
        if (navListEl && roleConfig.menus) {
            const menuItems = userRole === 'STAFF'
                ? [...roleConfig.menus.filter(item => item.id === 'dashboard'), ...roleConfig.menus.filter(item => item.id !== 'dashboard')]
                : roleConfig.menus;
            navListEl.innerHTML = menuItems.map(item => {
                const isActive = item.id === activeMenu ? 'active' : '';
                return `
                    <a href="${item.url}" class="nav-item ${isActive}" data-nav-id="${item.id}" data-tooltip="${item.title}">
                        <div class="sidebar-icon-box">
                            ${item.icon}
                        </div>
                        <span class="nav-title">${item.title}</span>
                    </a>
                `;
            }).join('');

            // Handle in-page tab switching / active indicator
            navListEl.addEventListener('click', function (e) {
                const navLink = e.target.closest('.nav-item');
                if (!navLink) return;
                const href = navLink.getAttribute('href');
                if (href && href.startsWith('#')) {
                    navListEl.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
                    navLink.classList.add('active');

                    // If tab sections exist, toggle visibility
                    const tabId = href.substring(1);
                    const tabs = document.querySelectorAll('.role-tab, [data-tab]');
                    if (tabs.length > 0) {
                        tabs.forEach(tab => {
                            const isMatch = tab.id === `tab-${tabId}` || tab.id === tabId || tab.getAttribute('data-tab') === tabId;
                            tab.style.display = isMatch ? 'block' : 'none';
                        });
                    }
                }
            });
        }
    }

    // Auto-init on DOMContentLoaded
    document.addEventListener('DOMContentLoaded', function () {
        const containers = document.querySelectorAll('#sidebar-container, [data-component="sidebar"]');
        containers.forEach(loadSidebar);
    });

    // Expose programmatic init
    window.initSidebar = function (options = {}) {
        const container = options.container || document.querySelector('#sidebar-container');
        if (container) {
            if (options.role) container.setAttribute('data-role', options.role);
            if (options.activeMenu) container.setAttribute('data-active-menu', options.activeMenu);
            loadSidebar(container);
        }
    };
})();

