(function () {

    'use strict';

    // ==========================================
    // 1. ACCESS CONTROL
    // ==========================================

    const user = window.api?.getCurrentUser?.();

    if (!user || user.role !== 'MANAGER') {
        window.location.replace('../index.html');
        return;
    }


    // ==========================================
    // 2. PRODUCT DATA
    // ==========================================

    const key = 'stocktrack_manager_products_' +
        (user.userId || user.username);

    let products = [
        {
            id: 5001,
            name: 'Modern table',
            cost: 700,
            price: 1000,
            stock: 100,
            minimum: 20,
            zone: 1
        },
        {
            id: 5002,
            name: 'Wooden table',
            cost: 400,
            price: 700,
            stock: 100,
            minimum: 20,
            zone: 1
        },
        {
            id: 5003,
            name: 'Dining table',
            cost: 800,
            price: 1100,
            stock: 100,
            minimum: 20,
            zone: 2
        }
    ];


    // ==========================================
    // 3. LOAD SAVED DATA
    // ==========================================

    const feedback = document.getElementById('manager-feedback');

    try {

        const saved = JSON.parse(localStorage.getItem(key) || 'null');

        if (saved !== null) {

            if (
                !Array.isArray(saved) ||
                saved.some(p =>
                    !Number.isInteger(p.id) ||
                    typeof p.name !== 'string' ||
                    !['cost', 'price', 'stock', 'minimum', 'zone']
                        .every(field =>
                            Number.isFinite(p[field]) && p[field] >= 0
                        )
                )
            ) {
                throw new Error('Invalid product data');
            }

            products = saved;
        }

    } catch (error) {

        feedback.textContent =
            'Saved data could not be loaded. Showing sample products.';

    }


    // ==========================================
    // 4. ELEMENTS
    // ==========================================

    const search = document.getElementById('manager-search');

    const zone = document.getElementById('manager-zone');

    const dialog = document.getElementById('manager-edit-dialog');

    const form = document.getElementById('manager-edit-form');

    const tbody = document.getElementById('manager-product-rows');

    const bars = document.getElementById('manager-bars');

    let editingId = null;


    // ==========================================
    // 5. CREATE ELEMENT HELPER
    // ==========================================

    function element(tag, text, className) {

        const node = document.createElement(tag);

        if (text !== undefined) {
            node.textContent = text;
        }

        if (className) {
            node.className = className;
        }

        return node;

    }


    // ==========================================
    // 6. SAVE DATA
    // ==========================================

    function saveData(data) {

        try {

            localStorage.setItem(key, JSON.stringify(data));

            return true;

        } catch (error) {

            feedback.textContent =
                'Unable to save data. Browser storage may be unavailable or full.';

            return false;

        }

    }


    // ==========================================
    // 7. RENDER DASHBOARD
    // ==========================================

    function render() {

        // Total Stock
        const total = products.reduce(
            (sum, product) => sum + product.stock,
            0
        );

        document.getElementById('manager-total').textContent =
            total.toLocaleString();


        // Low Stock
        const lowStock = products.filter(
            product => product.stock < product.minimum
        ).length;

        document.getElementById('manager-low').textContent = lowStock;


        // Search and Zone Filter
        const query = search.value.trim().toLowerCase();

        const visible = products.filter(product => {

            const matchesZone =
                zone.value === 'all' ||
                String(product.zone) === zone.value;

            const matchesSearch =
                product.name.toLowerCase().includes(query) ||
                String(product.id).includes(query);

            return matchesZone && matchesSearch;

        });


        // Clear Table and Chart
        tbody.replaceChildren();

        bars.replaceChildren();


                // Chart Maximum
        const maximum = Math.max(
            100,
            ...visible.map(product => product.stock)
        );

        document.getElementById('manager-axis-top').textContent =
            maximum.toLocaleString();

        document.getElementById('manager-axis-mid').textContent =
            Math.floor(maximum / 2).toLocaleString();


        const money = value => value.toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });


        // Render Products
        visible.forEach(product => {

            const row = element('tr');

            [
                product.id,
                product.name,
                money(product.cost),
                money(product.price),
                product.stock,
                product.minimum
            ].forEach((value, index) => {
                const td = element('td', value);
                if (index === 4) td.style.fontWeight = '600'; // Stock column bold
                row.appendChild(td);
            });


                        // Edit Button
            const action = element('td', undefined, 'text-center');

            const button = element(
                'button',
                '',
                'manager-edit-button'
            );

            button.type = 'button';
            button.innerHTML = `
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                Edit
            `;

            button.setAttribute(
                'aria-label',
                'Edit ' + product.name
            );

            button.addEventListener('click', () => {

                editingId = product.id;

                form.elements.productName.value = product.name;

                ['cost', 'price', 'stock', 'minimum'].forEach(field => {

                    form.elements[field].value = product[field];

                });

                document.getElementById('manager-edit-error').textContent = '';

                dialog.showModal();

            });

            action.appendChild(button);

            row.appendChild(action);

            tbody.appendChild(row);


            // Chart Bars
            const column = element('div', undefined, 'manager-bar-column');

            const bar = element('div', undefined, 'manager-bar');

            bar.style.height =
                (product.stock / maximum * 175) + 'px';

            bar.appendChild(
                element('strong', product.stock)
            );

            column.appendChild(bar);

            column.appendChild(
                element('span', product.name, 'manager-bar-label')
            );

            bars.appendChild(column);

        });


        // Empty State
        if (visible.length === 0) {

            const row = element('tr');

            const cell = element(
                'td',
                'No products match your search or zone.'
            );

            cell.colSpan = 7;

            row.appendChild(cell);

            tbody.appendChild(row);

        }


        // Accessible Chart Description
        document.querySelector('.manager-chart').setAttribute(
            'aria-label',
            visible.length
                ? visible.map(p => p.name + ': ' + p.stock + ' units').join(', ')
                : 'No matching products'
        );

    }


    // ==========================================
    // 8. EDIT PRODUCT
    // ==========================================

    form.elements.productName.addEventListener('input', () => {

        form.elements.productName.setCustomValidity('');

    });


    form.addEventListener('submit', event => {

        event.preventDefault();

        const name = form.elements.productName.value.trim();

        if (!name) {

            form.elements.productName.setCustomValidity(
                'Enter a product name.'
            );

            form.reportValidity();

            return;

        }


        const updated = products.map(product => {

            if (product.id !== editingId) {
                return product;
            }

            return {

                ...product,

                name: name,

                cost: Number(form.elements.cost.value),

                price: Number(form.elements.price.value),

                stock: Number(form.elements.stock.value),

                minimum: Number(form.elements.minimum.value)

            };

        });


        // Save
        if (!saveData(updated)) {
            return;
        }

        products = updated;

        dialog.close();

        render();

        feedback.textContent =
            'Product updated successfully. Saved in this browser only.';

    });


    // ==========================================
    // 9. CANCEL EDIT
    // ==========================================

    document.getElementById('manager-edit-cancel')
        .addEventListener('click', () => {

            dialog.close();

        });


    // ==========================================
    // 10. SEARCH AND FILTER
    // ==========================================

    search.addEventListener('input', render);

    zone.addEventListener('change', render);


    // ==========================================
    // 11. INITIAL LOAD
    // ==========================================

    render();

    console.log('Manager Dashboard loaded successfully.');

})();