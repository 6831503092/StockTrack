/**
 * StockTrack - Centralized API Helper (fetch wrapper)
 * Automatically attaches JSON headers, session credentials, and X-User-Id header.
 */
(function () {
    function getStoredUser() {
        if (window.Auth && typeof window.Auth.getUser === 'function') {
            return window.Auth.getUser();
        }
        try {
            const sessionRaw = sessionStorage.getItem('stocktrack_user');
            if (sessionRaw) {
                return JSON.parse(sessionRaw);
            }
            const localRaw = localStorage.getItem('stocktrack_user');
            if (localRaw) {
                sessionStorage.setItem('stocktrack_user', localRaw);
                return JSON.parse(localRaw);
            }
            return null;
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
            // Only redirect on 401 Unauthorized (unauthenticated/expired session)
            // 403 Forbidden means insufficient permission for that specific endpoint, not session loss
            if (response.status === 401) {
                const pathname = window.location.pathname;
                const isLoginPage = pathname.endsWith('index.html') || pathname === '/' || pathname === '';
                const isLoginEndpoint = typeof endpoint === 'string' && endpoint.includes('/api/login');
                if (!isLoginPage && !isLoginEndpoint) {
                    try {
                        if (window.Auth && typeof window.Auth.clearUser === 'function') {
                            window.Auth.clearUser();
                        } else {
                            sessionStorage.removeItem('stocktrack_user');
                            localStorage.removeItem('stocktrack_user');
                        }
                    } catch (e) {}
                    const redirectTarget = pathname.includes('/admin/') || pathname.includes('/manager/') || pathname.includes('/staff/') || pathname.includes('/owner/')
                        ? '../index.html'
                        : 'index.html';
                    window.location.replace(redirectTarget);
                }
            }

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
