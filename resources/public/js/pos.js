var POS = (function () {
    'use strict';

    var cart = [];
    var lastSale = null;
    var allProducts = [];
    var qrCodeCache = {};

    function generateQRBase64(text, size) {
        var key = text + '_' + size;
        if (qrCodeCache[key]) return qrCodeCache[key];

        if (typeof QRCode === 'undefined') {
            return null;
        }
        var canvas = document.createElement('canvas');
        try {
            QRCode.toCanvas(canvas, text, { width: size, margin: 1 });
            qrCodeCache[key] = canvas.toDataURL();
            return qrCodeCache[key];
        } catch (e) {
            return null;
        }
    }

    function makeProductQRText(productId) {
        return 'PROD-' + productId;
    }

    function getProductQR(productId) {
        var qrText = makeProductQRText(productId);
        var qrBase64 = generateQRBase64(qrText, 60);
        if (!qrBase64) {
            return '<span class="text-muted" style="font-size:10px;">QR</span>';
        }
        return '<img src="' + qrBase64 + '" style="width:40px;height:40px;border:1px solid #ddd;border-radius:3px;" title="QR: ' + qrText + '">';
    }

    function getProductData() {
        var el = document.getElementById('pos-app');
        if (!el) return [];
        try { return JSON.parse(el.getAttribute('data-productos') || '[]'); }
        catch (e) { return []; }
    }

    function init() {
        allProducts = getProductData();
        loadQuoteCart();
        var input = document.getElementById('pos-search');
        if (input) {
            // Live search: show/hide cards as the user types (non-numeric)
            input.addEventListener('input', function () {
                var term = this.value.trim();
                // Only do client-side filtering for non-numeric search
                if (!/^\d+$/.test(term)) {
                    var lowerTerm = term.toLowerCase();
                    document.querySelectorAll('.pos-product-card').forEach(function (card) {
                        var name = (card.getAttribute('data-nombre') || '').toLowerCase();
                        card.classList.toggle('hidden', !(!term || name.indexOf(term) !== -1));
                    });
                }
            });
            // Barcode scanner support: detect Enter key on numeric input
            input.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    var term = this.value.trim();
                    if (/^\d+$/.test(term)) {
                        e.preventDefault();
                        handleBarcodeScan(term);
                    }
                }
            });
        }
    }

    var quoteId = null;

    function loadQuoteCart() {
        try {
            var stored = localStorage.getItem('cotizacion_cart');
            if (!stored) return;
            var data = JSON.parse(stored);
            if (!data || !Array.isArray(data.items) || data.items.length === 0) return;
            quoteId = data.cotizacion_id || null;
            cart = data.items.map(function (item) {
                return {
                    producto_id: item.producto_id,
                    nombre: item.nombre,
                    precio: parseFloat(item.precio) || 0,
                    cantidad: item.cantidad || 1,
                    categoria: item.categoria || ''
                };
            });
            localStorage.removeItem('cotizacion_cart');
            renderCart();
        } catch (e) {
            console.warn('No quote cart loaded', e);
        }
    }

    function handleBarcodeScan(barcode) {
        var csrfInput = document.querySelector('input[name="__anti-forgery-token"]');
        var csrfToken = csrfInput ? csrfInput.value : '';
        var input = document.getElementById('pos-search');

        fetch('/api/pos/search?q=' + encodeURIComponent(barcode), {
            method: 'GET',
            headers: {
                'X-CSRF-Token': csrfToken,
                'x-requested-with': 'XMLHttpRequest'
            }
        })
            .then(function (resp) { return resp.json(); })
            .then(function (data) {
                if (data.ok && data.scanned) {
                    addItem(data.scanned.id);
                    input.value = '';
                    // Reset card visibility
                    document.querySelectorAll('.pos-product-card').forEach(function (card) {
                        card.classList.remove('hidden');
                    });
                } else if (data.ok && data.data && data.data.length === 0) {
                    alert('Producto no encontrado');
                }
            })
            .catch(function (err) {
                console.error('Barcode scan error:', err);
            });
    }

    // --- Cart helpers ---
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
                stock: parseInt(product.stock) || 0,
                categoria: product.categoria || ''
            });
        }
        renderCart();
    }

    function addMiscCharge() {
        var name = prompt('Nombre del cargo misc. (ej: Manode obra):');
        if (!name || !name.trim()) return;
        var priceStr = prompt('Monto del cargo:');
        var price = parseFloat(priceStr);
        if (isNaN(price) || price <= 0) {
            alert('Monto inválido');
            return;
        }
        var miscId = 'MISC-' + Date.now();
        cart.push({
            producto_id: null,
            nombre: name.trim(),
            precio: price,
            cantidad: 1,
            stock: 0,
            categoria: 'misc'
        });
        renderCart();
    }

    function findCartItemByName(name) {
        for (var i = 0; i < cart.length; i++) {
            if (cart[i].nombre === name && cart[i].producto_id === null) return i;
        }
        return -1;
    }

    function removeItem(productId, isMisc) {
        var idx;
        if (isMisc) {
            idx = findCartItemByName(productId);
        } else {
            idx = findCartItem(productId);
        }
        if (idx >= 0) { cart.splice(idx, 1); renderCart(); }
    }

    function updateQty(productId, delta, isMisc) {
        var idx;
        if (isMisc) {
            idx = findCartItemByName(productId);
        } else {
            idx = findCartItem(productId);
        }
        if (idx < 0) return;
        cart[idx].cantidad += delta;
        if (cart[idx].cantidad <= 0) cart.splice(idx, 1);
        renderCart();
    }

    function getTotal() {
        return cart.reduce(function (sum, item) {
            return sum + item.cantidad * item.precio;
        }, 0);
    }

    // --- Render the cart panel ---
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
        cart.forEach(function (item) {
            var subtotal = (item.cantidad * item.precio).toFixed(2);
            var isMisc = item.producto_id === null;
            var escapedName = escapeHtml(item.nombre).replace(/'/g, "\\'");
            html += '<div class="pos-cart-row">';
            html += '<div class="pos-cart-qr">' + (isMisc ? '<span class="text-muted">-</span>' : getProductQR(item.producto_id)) + '</div>';
            html += '<span class="pos-cart-name">' + escapeHtml(item.nombre) + '</span>';
            html += '<span class="pos-cart-qty">';
            if (isMisc) {
                html += '<button class="btn btn-sm btn-outline-secondary pos-qty-btn"'
                    + " onclick=\"POS.updateQty('" + escapedName + "', -1, true)\">-</button>";
            } else {
                html += '<button class="btn btn-sm btn-outline-secondary pos-qty-btn"'
                    + ' onclick="POS.updateQty(' + item.producto_id + ', -1, false)">-</button>';
            }
            html += '<strong class="mx-1">' + item.cantidad + '</strong>';
            if (isMisc) {
                html += '<button class="btn btn-sm btn-outline-secondary pos-qty-btn"'
                    + " onclick=\"POS.updateQty('" + escapedName + "', 1, true)\">+</button>";
            } else {
                html += '<button class="btn btn-sm btn-outline-secondary pos-qty-btn"'
                    + ' onclick="POS.updateQty(' + item.producto_id + ', 1, false)">+</button>';
            }
            html += '</span>';
            html += '<span class="pos-cart-price">$' + subtotal + '</span>';
            if (isMisc) {
                html += '<span class="pos-cart-remove"'
                    + " onclick=\"POS.removeItem('" + escapedName + "', true)\">"
                    + '<i class="bi bi-x-circle"></i></span>';
            } else {
                html += '<span class="pos-cart-remove"'
                    + ' onclick="POS.removeItem(' + item.producto_id + ', false)">'
                    + '<i class="bi bi-x-circle"></i></span>';
            }
            html += '</div>';
        });

        container.innerHTML = html;
        totalEl.textContent = '$' + getTotal().toFixed(2);
        registerBtn.disabled = false;
        calcChange();
    }

    function calcChange() {
        var total = getTotal();
        var pago = parseFloat(document.getElementById('pos-payment').value) || 0;
        var cambio = pago - total;
        document.getElementById('pos-change').textContent =
            cambio >= 0 ? cambio.toFixed(2) : '0.00';
    }

    function clearCart() {
        cart = [];
        lastSale = null;
        document.getElementById('pos-payment').value = '';
        document.getElementById('pos-change').textContent = '0.00';
        document.getElementById('pos-print-btn').style.display = 'none';
        renderCart();
    }

    // --- Register the sale via fetch ---
    function registerSale() {
        if (cart.length === 0) return;

        var total = getTotal();
        var pago = parseFloat(document.getElementById('pos-payment').value) || 0;
        if (pago < total) {
            alert('El pago debe ser mayor o igual al total.');
            return;
        }

        // Read the CSRF token injected by anti-forgery-field
        var csrfInput = document.querySelector('input[name="__anti-forgery-token"]');
        var csrfToken = csrfInput ? csrfInput.value : '';

        var payload = {
            cotizacion_id: quoteId,
            items: cart.map(function (item) {
                return {
                    producto_id: item.producto_id,
                    nombre: item.nombre,
                    cantidad: item.cantidad,
                    precio: item.precio,
                    categoria: item.categoria || ''
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
                        venta_id: data.venta_id, items: cart.slice(),
                        site_name: data.site_name,
                        total: data.total, pago: pago,
                        cambio: data.cambio,
                        fecha: new Date().toLocaleString()
                    };
                    alert('Venta registrada #' + data.venta_id);
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

    // --- Print a receipt in a new window ---
    function printReceipt() {
        if (!lastSale) { alert('No hay venta para imprimir.'); return; }
        var s = lastSale;
        var w = window.open('', '_blank', 'width=400,height=600');
        if (!w) { alert('El bloqueador de ventanas emergentes impidió abrir el recibo.'); return; }
        var rows = s.items.map(function (it) {
            return '<tr><td>' + escapeHtml(it.nombre) + '</td>'
                + '<td style="text-align:right">' + it.cantidad + '</td>'
                + '<td style="text-align:right">$' + (it.cantidad * it.precio).toFixed(2) + '</td></tr>';
        }).join('');
        w.document.write(
            '<html><head><title>Recibo #' + s.venta_id + '</title>'
            + '<style>body{font-family:monospace;font-size:12px;width:300px;margin:20px auto}'
            + 'table{width:100%}.line{border-top:1px dashed #000;margin:8px 0}</style></head><body>'
            + '<h2 style="text-align:center">' + s.site_name + '</h2>'
            + '<h3 style="text-align:center">Recibo de Venta</h3>'
            + '<p style="text-align:center">Venta #' + s.venta_id + '<br>' + s.fecha + '</p>'
            + '<div class="line"></div>'
            + '<table><tr><th>Producto</th><th>Cant</th><th>Total</th></tr>' + rows + '</table>'
            + '<div class="line"></div>'
            + '<table>'
            + '<tr><td>Total:</td><td style="text-align:right">$' + s.total.toFixed(2) + '</td></tr>'
            + '<tr><td>Pago:</td><td style="text-align:right">$' + s.pago.toFixed(2) + '</td></tr>'
            + '<tr><td>Cambio:</td><td style="text-align:right">$' + s.cambio.toFixed(2) + '</td></tr>'
            + '</table>'
            + '<div class="line"></div>'
            + '<p style="text-align:center">¡Gracias por su compra!</p>'
            + '</body></html>'
        );
        w.document.close();
        w.focus();
        w.print();
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    return {
        addItem: addItem, removeItem: removeItem, updateQty: updateQty,
        clearCart: clearCart, calcChange: calcChange,
        registerSale: registerSale, printReceipt: printReceipt,
        addMiscCharge: addMiscCharge
    };
})();
