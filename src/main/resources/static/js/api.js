/**
 * StockTrack - Centralized API Helper (fetch wrapper)
 * Automatically attaches JSON headers, session credentials, and X-User-Id header.
 */
(function () {
    function getStoredUser() {
        try {
            const raw = localStorage.getItem('stocktrack_user');
            return raw ? JSON.parse(raw) : null;
        } catch (err) {
            return null;
        }
    }

    async function request(endpoint, options = {}) {
        const user = getStoredUser();
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        if (user && user.userId !== undefined && user.userId !== null) {
            headers['X-User-Id'] = String(user.userId);
        } else if (user && user.role === 'MANAGER') {
            headers['X-User-Id'] = '503069';
        } else if (user && user.role === 'ADMIN') {
            headers['X-User-Id'] = '503092';
        }

        const config = {
            method: options.method || 'GET',
            credentials: 'include',
            ...options,
            headers
        };

        if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
            config.body = JSON.stringify(config.body);
        }

        const response = await fetch(endpoint, config);
        let data = null;

        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            data = await response.json();
        } else {
            data = await response.text();
        }

        if (!response.ok) {
            const message = (data && data.message) ? data.message : `HTTP Error ${response.status}`;
            const error = new Error(message);
            error.status = response.status;
            error.data = data;
            throw error;
        }

        return data;
    }

    window.api = {
        getCurrentUser: getStoredUser,
        request,
        get: (endpoint, options = {}) => request(endpoint, { ...options, method: 'GET' }),
        post: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'POST', body }),
        put: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'PUT', body }),
        delete: (endpoint, options = {}) => request(endpoint, { ...options, method: 'DELETE' })
    };
})();

