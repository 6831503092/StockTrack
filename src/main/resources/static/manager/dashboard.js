(function () {
    'use strict';
    const user = window.api.getCurrentUser();
    if (!user || user.role !== 'MANAGER') { window.location.replace('../index.html'); return; }
    const key = 'stocktrack_manager_products_' + (user.userId || user.username);
    let products = [
        { id: 5001, name: 'Modern table', cost: 700, price: 1000, stock: 100, minimum: 20, zone: 1 },
        { id: 5002, name: 'Wooden table', cost: 400, price: 700, stock: 100, minimum: 20, zone: 1 },
        { id: 5003, name: 'Dining table', cost: 800, price: 1100, stock: 100, minimum: 20, zone: 1 }
    ];
    const feedback = document.getElementById('manager-feedback');
    try {
        const saved = JSON.parse(localStorage.getItem(key) || 'null');
        if (saved !== null) {
            if (!Array.isArray(saved) || saved.some(p => !Number.isInteger(p.id) || typeof p.name !== 'string' || !['cost','price','stock','minimum','zone'].every(field => Number.isFinite(p[field]) && p[field] >= 0))) throw new Error('Invalid data');
            products = saved;
        }
    } catch (error) { feedback.textContent = 'Saved data could not be loaded. Showing sample products.'; }
    const search = document.getElementById('manager-search');
    const zone = document.getElementById('manager-zone');
    const dialog = document.getElementById('manager-edit-dialog');
    const form = document.getElementById('manager-edit-form');
    let editingId = null;
    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text !== undefined) node.textContent = text;
        if (className) node.className = className;
        return node;
    }
    function render() {
        document.getElementById('manager-total').textContent = products.reduce((sum,p) => sum + p.stock,0).toLocaleString();
        document.getElementById('manager-low').textContent = products.filter(p => p.stock < p.minimum).length;
        const query = search.value.trim().toLowerCase();
        const visible = products.filter(p => (zone.value === 'all' || String(p.zone) === zone.value) && (p.name.toLowerCase().includes(query) || String(p.id).includes(query)));
        const tbody = document.getElementById('manager-product-rows');
        const bars = document.getElementById('manager-bars');
        tbody.replaceChildren(); bars.replaceChildren();
        const maximum = Math.max(100,...visible.map(p => p.stock));
        document.getElementById('manager-axis-top').textContent = maximum.toLocaleString();
        document.getElementById('manager-axis-mid').textContent = (maximum/2).toLocaleString();
        const money = value => value.toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});
        visible.forEach(product => {
            const row = element('tr');
            [product.id,product.name,money(product.cost),money(product.price),product.stock,product.minimum].forEach(value => row.appendChild(element('td',value)));
            const action = element('td');
            const button = element('button','[Edit]','manager-edit-button');
            button.type = 'button'; button.setAttribute('aria-label','Edit ' + product.name);
            button.addEventListener('click', () => {
                editingId = product.id;
                form.elements.productName.value = product.name;
                ['cost','price','stock','minimum'].forEach(field => { form.elements[field].value = product[field]; });
                form.elements.productName.setCustomValidity('');
                document.getElementById('manager-edit-error').textContent = '';
                dialog.showModal();
            });
            action.appendChild(button); row.appendChild(action); tbody.appendChild(row);
            const column = element('div',undefined,'manager-bar-column');
            const bar = element('div',undefined,'manager-bar');
            bar.style.height = (product.stock/maximum*175) + 'px';
            bar.appendChild(element('strong',product.stock));
            column.appendChild(bar); column.appendChild(element('span',product.name,'manager-bar-label')); bars.appendChild(column);
        });
        document.querySelector('.manager-chart').setAttribute('aria-label', visible.length ? visible.map(p => p.name + ': ' + p.stock + ' units').join(', ') : 'No matching products');
        if (!visible.length) { const row = element('tr'); const cell = element('td','No products match your search or zone.'); cell.colSpan = 7; row.appendChild(cell); tbody.appendChild(row); }
    }
    form.elements.productName.addEventListener('input', () => form.elements.productName.setCustomValidity(''));
    form.addEventListener('submit', event => {
        event.preventDefault();
        form.elements.productName.setCustomValidity(form.elements.productName.value.trim() ? '' : 'Enter a product name.');
        if (!form.reportValidity()) return;
        const updated = products.map(p => p.id === editingId ? {...p,name:form.elements.productName.value.trim(),cost:Number(form.elements.cost.value),price:Number(form.elements.price.value),stock:Number(form.elements.stock.value),minimum:Number(form.elements.minimum.value)} : p);
        try { localStorage.setItem(key,JSON.stringify(updated)); }
        catch (error) { document.getElementById('manager-edit-error').textContent = 'Unable to save. Browser storage may be unavailable or full.'; return; }
        products = updated; dialog.close(); render(); feedback.textContent = 'Product updated in this browser. Server inventory has not changed.';
    });
    document.getElementById('manager-edit-cancel').addEventListener('click', () => dialog.close());
    search.addEventListener('input',render); zone.addEventListener('change',render);
    render();
})();
