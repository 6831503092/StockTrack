/**
 * StockTrack - Manager Facade
 * Backward compatibility loader that routes to manager-dashboard.js or manager-requests.js
 */
(function () {
    'use strict';
    const isDashboard = !!document.getElementById('manager-bars');
    const isRequests = !!document.getElementById('requests-table-body');

    function loadScript(src) {
        const s = document.createElement('script');
        s.src = src;
        document.body.appendChild(s);
    }

    if (isDashboard && !window._managerDashboardLoaded) {
        window._managerDashboardLoaded = true;
        loadScript('../js/manager-dashboard.js');
    } else if (isRequests && !window._managerRequestsLoaded) {
        window._managerRequestsLoaded = true;
        loadScript('../js/manager-requests.js');
    }
})();
