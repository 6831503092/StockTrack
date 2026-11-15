/**
 * StockTrack - Shared Sidebar Navigation Component Script
 * Handles dynamic role rendering, zero-shift hover expansion, active states, and unified logout.
 */
(function () {
    // Fetch or resolve role configurations from separated sidebar-config.js
    async function getRoleConfigs() {
        if (window.STOCKTRACK_ROLE_CONFIGS) {
            return window.STOCKTRACK_ROLE_CONFIGS;
        }
        await new Promise((resolve) => {
            const script = document.createElement('script');
            const inSubdir = window.location.pathname.includes('/admin/') ||
                             window.location.pathname.includes('/manager/') ||
                             window.location.pathname.includes('/owner/') ||
                             window.location.pathname.includes('/staff/');
            script.src = inSubdir ? '../js/common/sidebar-config.js' : 'js/common/sidebar-config.js';
            script.onload = () => resolve(window.STOCKTRACK_ROLE_CONFIGS);
            script.onerror = () => resolve(null);
            document.head.appendChild(script);
        });
        return window.STOCKTRACK_ROLE_CONFIGS || {};
    }

    // State persistence key for seamless navigation expansion
    const SIDEBAR_STORAGE_KEY = 'stocktrack_sidebar_expanded';

    // Unified Logout Handler
    window.handleSidebarLogout = function (e) {
        if (e) e.preventDefault();
        try {
            sessionStorage.removeItem(SIDEBAR_STORAGE_KEY);
        } catch (err) { }
        fetch('/api/logout', {
            method: 'POST',
            credentials: 'include'
        }).catch(() => { }).finally(() => {
            try {
                localStorage.removeItem('stocktrack_user');
            } catch (err) { }
            window.location.href = '../index.html';
        });
    };

    // Load Sidebar into Container
    async function loadSidebar(container) {
        if (!container) return;

        // Check if sidebar should be pre-expanded from previous page navigation
        let isPreExpanded = false;
        try {
            isPreExpanded = sessionStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
        } catch (e) { }

        // Determine current role & active menu
        let userRole = container.getAttribute('data-role');
        let activeMenu = container.getAttribute('data-active-menu');

        let storedUser = null;
        try {
            const raw = localStorage.getItem('stocktrack_user');
            if (raw) {
                storedUser = JSON.parse(raw);
                if (storedUser && storedUser.role && !userRole) {
                    userRole = storedUser.role.toUpperCase();
                }
            }
        } catch (err) { }

        // Infer role from current file path if not found
        if (!userRole) {
            const path = window.location.pathname.toLowerCase();
            if (path.includes('admin')) userRole = 'ADMIN';
            else if (path.includes('manager')) userRole = 'MANAGER';
            else if (path.includes('owner')) userRole = 'OWNER';
            else if (path.includes('staff')) userRole = 'STAFF';
            else userRole = 'ADMIN';
        }

        const roleConfigs = await getRoleConfigs();
        const roleConfig = roleConfigs[userRole] || roleConfigs.ADMIN || {};
        if (!activeMenu) {
            activeMenu = roleConfig.menus?.[0]?.id || 'dashboard';
        }

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

        // Apply pre-expanded state to fetched HTML before mounting
        if (sidebarHtml && isPreExpanded) {
            sidebarHtml = sidebarHtml.replace(/class=["']sidebar["']/, 'class="sidebar is-expanded no-transition"');
        }

        // Fallback HTML template if fetch fails or running directly via file://
        if (!sidebarHtml) {
            const initialSidebarClass = isPreExpanded ? 'sidebar is-expanded no-transition' : 'sidebar';
            sidebarHtml = `
            <aside class="${initialSidebarClass}" id="app-sidebar">
                <a href="#" class="sidebar-logo" id="sidebar-logo" title="StockTrack">
                    <div class="sidebar-icon-box">
                        <svg class="logo-icon" width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#27c690" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

        const sidebarEl = container.querySelector('.sidebar');

        // Manage pre-expanded state transition release and mouse tracking
        if (sidebarEl) {
            if (isPreExpanded) {
                // Remove no-transition on next frame so subsequent user actions animate smoothly
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        sidebarEl.classList.remove('no-transition');
                    });
                });

                // Safety: if mouse is not over sidebar on load (e.g. reload), collapse cleanly
                const checkInitialMouse = (e) => {
                    const rect = sidebarEl.getBoundingClientRect();
                    const isInside = (
                        e.clientX >= rect.left &&
                        e.clientX <= rect.right &&
                        e.clientY >= rect.top &&
                        e.clientY <= rect.bottom
                    );
                    if (!isInside) {
                        sidebarEl.classList.remove('is-expanded');
                        try {
                            sessionStorage.removeItem(SIDEBAR_STORAGE_KEY);
                        } catch (err) { }
                    }
                    window.removeEventListener('mousemove', checkInitialMouse);
                };
                window.addEventListener('mousemove', checkInitialMouse, { passive: true });
            }

            let isNavigating = false;

            // When hovering sidebar, record state
            sidebarEl.addEventListener('mouseenter', function () {
                try {
                    sessionStorage.setItem(SIDEBAR_STORAGE_KEY, 'true');
                } catch (err) { }
            });

            // When leaving sidebar, collapse smoothly unless navigating to another page
            sidebarEl.addEventListener('mouseleave', function () {
                if (isNavigating) return;
                sidebarEl.classList.remove('is-expanded');
                try {
                    sessionStorage.removeItem(SIDEBAR_STORAGE_KEY);
                } catch (err) { }
            });

            // Intercept clicks on links inside sidebar
            sidebarEl.addEventListener('click', function (e) {
                const link = e.target.closest('a');
                if (!link) return;

                // Logout button cleans up state
                if (link.id === 'sidebar-logout-btn' || link.classList.contains('nav-logout')) {
                    try {
                        sessionStorage.removeItem(SIDEBAR_STORAGE_KEY);
                    } catch (err) { }
                    return;
                }

                const href = link.getAttribute('href');
                if (href && !href.startsWith('#') && !href.startsWith('javascript:') && link.target !== '_blank') {
                    isNavigating = true;
                    try {
                        sessionStorage.setItem(SIDEBAR_STORAGE_KEY, 'true');
                    } catch (err) { }
                }
            });
        }

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
        const currentUserId = (storedUser && storedUser.userId)
            ? String(storedUser.userId)
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
            navListEl.innerHTML = roleConfig.menus.map(item => {
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
