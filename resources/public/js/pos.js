/**
 * POS - Point of Sale JavaScript
 * Manages cart, search, payment calculation, sale registration, and receipt printing.
 */
var POS = (function () {
    'use strict';

    // Cart: array of { producto_id, nombre, precio, cantidad, stock }
    var cart = [];
    var lastSale = null;

    // Parse product data embedded in DOM
    function getProductData() {
        var el = document.getElementById('pos-app');
        if (!el) return [];
        try {
            return JSON.parse(el.getAttribute('data-productos') || '[]');
        } catch (e) {
            return [];
        }
    }

    var allProducts = [];

    function init() {
        allProducts = getProductData();
        bindSearch();
    }

    // --- Search ---
    function bindSearch() {
        var input = document.getElementById('pos-search');
        if (!input) return;
        input.addEventListener('input', function () {
            var term = this.value.toLowerCase().trim();
            var cards = document.querySelectorAll('.pos-product-card');
            cards.forEach(function (card) {
                var name = (card.getAttribute('data-nombre') || '').toLowerCase();
                if (!term || name.indexOf(term) !== -1) {
                    card.classList.remove('hidden');
                } else {
                    card.classList.add('hidden');
                }
            });
        });
    }

    // --- Cart operations ---
    function findProduct(id) {
        for (var i = 0; i < allProducts.length; i++) {
            if (allProducts[i].id == id) return allProducts[i];
        }
        return null;
    }

    function findCartItem(id) {
        for (var i = 0; i < cart.length; i++) {
            if (cart[i].producto_id == id) return i;
        }
        return -1;
    }

    function addItem(productId) {
        var product = findProduct(productId);
        if (!product) return;
        var idx = findCartItem(productId);
        if (idx >= 0) {
            cart[idx].cantidad++;
        } else {
            cart.push({
                producto_id: product.id,
                nombre: product.nombre,
                precio: parseFloat(product.precio) || 0,
                cantidad: 1,
                stock: parseInt(product.stock) || 0
            });
        }
        renderCart();
    }

    function removeItem(productId) {
        var idx = findCartItem(productId);
        if (idx >= 0) {
            cart.splice(idx, 1);
            renderCart();
        }
    }

    function updateQty(productId, delta) {
        var idx = findCartItem(productId);
        if (idx < 0) return;
        cart[idx].cantidad += delta;
        if (cart[idx].cantidad <= 0) {
            cart.splice(idx, 1);
        }
        renderCart();
    }

    function clearCart() {
        cart = [];
        lastSale = null;
        document.getElementById('pos-payment').value = '';
        document.getElementById('pos-change').textContent = '0.00';
        document.getElementById('pos-print-btn').style.display = 'none';
        renderCart();
    }

    // --- Render ---
    function getTotal() {
        var total = 0;
        for (var i = 0; i < cart.length; i++) {
            total += cart[i].cantidad * cart[i].precio;
        }
        return total;
    }

    function renderCart() {
        var container = document.getElementById('pos-cart-items');
        var totalEl = document.getElementById('pos-total');
        var registerBtn = document.getElementById('pos-register-btn');

        if (cart.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Carrito vacío</p>';
            totalEl.textContent = '$0.00';
            registerBtn.disabled = true;
            calcChange();
            return;
        }

        var html = '';
        for (var i = 0; i < cart.length; i++) {
            var item = cart[i];
            var subtotal = (item.cantidad * item.precio).toFixed(2);
            html += '<div class="pos-cart-row">';
            html += '  <span class="pos-cart-name">' + escapeHtml(item.nombre) + '</span>';
            html += '  <span class="pos-cart-qty">';
            html += '    <button class="btn btn-sm btn-outline-secondary pos-qty-btn" onclick="POS.updateQty(' + item.producto_id + ', -1)">-</button>';
            html += '    <strong class="mx-1">' + item.cantidad + '</strong>';
            html += '    <button class="btn btn-sm btn-outline-secondary pos-qty-btn" onclick="POS.updateQty(' + item.producto_id + ', 1)">+</button>';
            html += '  </span>';
            html += '  <span class="pos-cart-price">$' + subtotal + '</span>';
            html += '  <span class="pos-cart-remove" onclick="POS.removeItem(' + item.producto_id + ')" title="Eliminar">';
            html += '    <i class="bi bi-x-circle"></i>';
            html += '  </span>';
            html += '</div>';
        }
        container.innerHTML = html;

        var total = getTotal();
        totalEl.textContent = '$' + total.toFixed(2);
        registerBtn.disabled = false;
        calcChange();
    }

    function calcChange() {
        var total = getTotal();
        var pagoEl = document.getElementById('pos-payment');
        var changeEl = document.getElementById('pos-change');
        var pago = parseFloat(pagoEl.value) || 0;
        var cambio = pago - total;
        changeEl.textContent = cambio >= 0 ? cambio.toFixed(2) : '0.00';
    }

    // --- Register sale ---
    function registerSale() {
        if (cart.length === 0) return;

        var total = getTotal();
        var pago = parseFloat(document.getElementById('pos-payment').value) || 0;
        if (pago < total) {
            alert('El pago debe ser mayor o igual al total.');
            return;
        }

        // Get CSRF token
        var csrfInput = document.querySelector('input[name="__anti-forgery-token"]');
        var csrfToken = csrfInput ? csrfInput.value : '';

        var payload = {
            items: cart.map(function (item) {
                return {
                    producto_id: item.producto_id,
                    cantidad: item.cantidad,
                    precio: item.precio
                };
            }),
            pago: pago
        };

        var btn = document.getElementById('pos-register-btn');
        btn.disabled = true;
        btn.textContent = 'Registrando...';

        fetch('/api/pos/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': csrfToken,
                'x-requested-with': 'XMLHttpRequest'
            },
            body: JSON.stringify(payload)
        })
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                if (data.ok) {
                    lastSale = {
                        venta_id: data.venta_id,
                        items: cart.slice(),
                        total: data.total,
                        pago: pago,
                        cambio: data.cambio,
                        fecha: new Date().toLocaleString()
                    };
                    // Flash success
                    var card = document.querySelector('.col-lg-4 .card');
                    if (card) {
                        card.classList.add('pos-sale-success');
                        setTimeout(function () { card.classList.remove('pos-sale-success'); }, 700);
                    }
                    alert('Venta registrada con éxito. #' + data.venta_id);
                    document.getElementById('pos-print-btn').style.display = '';
                    cart = [];
                    document.getElementById('pos-payment').value = '';
                    renderCart();
                } else {
                    alert('Error: ' + (data.error || 'Error desconocido'));
                }
            })
            .catch(function (err) {
                alert('Error de red: ' + err.message);
            })
            .finally(function () {
                btn.disabled = false;
                btn.textContent = 'Registrar Venta';
            });
    }

    // --- Print receipt ---
    function printReceipt() {
        if (!lastSale) {
            alert('No hay venta para imprimir.');
            return;
        }
        var s = lastSale;
        var w = window.open('', '_blank', 'width=400,height=600');
        var html = '<html><head><title>Recibo #' + s.venta_id + '</title>';
        html += '<style>';
        html += 'body { font-family: "Courier New", monospace; font-size: 12px; width: 300px; margin: 20px auto; }';
        html += 'h2 { text-align: center; margin-bottom: 4px; }';
        html += '.line { border-top: 1px dashed #000; margin: 8px 0; }';
        html += 'table { width: 100%; border-collapse: collapse; }';
        html += 'td { padding: 2px 0; }';
        html += '.right { text-align: right; }';
        html += '.bold { font-weight: bold; }';
        html += '</style></head><body>';
        html += '<h2>Recibo de Venta</h2>';
        html += '<p style="text-align:center;">Venta #' + s.venta_id + '<br>' + s.fecha + '</p>';
        html += '<div class="line"></div>';
        html += '<table>';
        html += '<tr class="bold"><td>Producto</td><td class="right">Cant</td><td class="right">Precio</td></tr>';
        for (var i = 0; i < s.items.length; i++) {
            var it = s.items[i];
            html += '<tr><td>' + escapeHtml(it.nombre) + '</td>';
            html += '<td class="right">' + it.cantidad + '</td>';
            html += '<td class="right">$' + (it.cantidad * it.precio).toFixed(2) + '</td></tr>';
        }
        html += '</table>';
        html += '<div class="line"></div>';
        html += '<table>';
        html += '<tr class="bold"><td>Total:</td><td class="right">$' + s.total.toFixed(2) + '</td></tr>';
        html += '<tr><td>Pago:</td><td class="right">$' + s.pago.toFixed(2) + '</td></tr>';
        html += '<tr><td>Cambio:</td><td class="right">$' + s.cambio.toFixed(2) + '</td></tr>';
        html += '</table>';
        html += '<div class="line"></div>';
        html += '<p style="text-align:center;">¡Gracias por su compra!</p>';
        html += '</body></html>';
        w.document.write(html);
        w.document.close();
        w.focus();
        w.print();
    }

    // --- Helpers ---
    function escapeHtml(str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    // Init on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    return {
        addItem: addItem,
        removeItem: removeItem,
        updateQty: updateQty,
        clearCart: clearCart,
        calcChange: calcChange,
        registerSale: registerSale,
        printReceipt: printReceipt
    };
})();
