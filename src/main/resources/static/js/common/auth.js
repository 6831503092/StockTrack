/**
 * StockTrack - Common Authentication & Role Guard Module
 * Manages tab-isolated user state (sessionStorage with localStorage fallback)
 * and role-based route guarding.
 */
(function () {
    const STORAGE_KEY = 'stocktrack_user';

    function getUser() {
        try {
            // 1. Prefer sessionStorage for strict per-tab session isolation
            const sessionRaw = sessionStorage.getItem(STORAGE_KEY);
            if (sessionRaw) {
                return JSON.parse(sessionRaw);
            }

            // 2. Fallback to localStorage (e.g. freshly opened tab or cross-tab restore)
            const localRaw = localStorage.getItem(STORAGE_KEY);
            if (localRaw) {
                // Seed this tab's sessionStorage so changes in other tabs don't affect this tab
                sessionStorage.setItem(STORAGE_KEY, localRaw);
                return JSON.parse(localRaw);
            }

            return null;
        } catch (e) {
            console.error('Failed to parse stored user:', e);
            return null;
        }
    }

    function setUser(user) {
        if (!user) {
            clearUser();
            return;
        }
        try {
            const serialized = JSON.stringify(user);
            sessionStorage.setItem(STORAGE_KEY, serialized);
            localStorage.setItem(STORAGE_KEY, serialized);
        } catch (e) {
            console.error('Failed to save user in storage:', e);
        }
    }

    function clearUser() {
        try {
            sessionStorage.removeItem(STORAGE_KEY);
            localStorage.removeItem(STORAGE_KEY);
        } catch (e) {
            console.error('Failed to clear user storage:', e);
        }
    }

    function isLoggedIn() {
        const user = getUser();
        return Boolean(user && user.userId);
    }

    function getRole() {
        const user = getUser();
        return user && user.role ? String(user.role).toUpperCase() : null;
    }

    function requireRole(allowedRoles, redirectUrl = '../index.html') {
        const user = getUser();
        const role = getRole();

        const rolesArray = Array.isArray(allowedRoles)
            ? allowedRoles.map(r => String(r).toUpperCase())
            : [String(allowedRoles).toUpperCase()];

        if (!user || !role || !rolesArray.includes(role)) {
            if (!user) clearUser();
            window.location.replace(redirectUrl);
            return false;
        }
        return true;
    }

    const Auth = {
        getUser,
        getCurrentUser: getUser,
        setUser,
        clearUser,
        isLoggedIn,
        getRole,
        requireRole,
        STORAGE_KEY
    };

    window.StockTrackAuth = Auth;
    window.Auth = Auth;
})();
