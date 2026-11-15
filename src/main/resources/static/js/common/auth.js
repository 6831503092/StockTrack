/**
 * StockTrack - Common Authentication & Role Guard Module
 * Centralizes user state in localStorage and role-based route guarding.
 */
(function () {
    const STORAGE_KEY = 'stocktrack_user';

    function getUser() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
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
            localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
        } catch (e) {
            console.error('Failed to save user in storage:', e);
        }
    }

    function clearUser() {
        try {
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
