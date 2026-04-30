var Cotizaciones = (function () {
    'use strict';

    var items = [];
    var searchTimeout = null;
    var clientSearchTimeout = null;

    function init() {
        if (typeof IT !== 'undefined' && Array.isArray(IT)) {
            items = IT.slice();
        }
        renderCart();
        syncServiceCheckboxes();
    }

    function addItem(productId, nombre, precio, tipo) {
        if (tipo !== 'misc') {
            var idx = findItemIndex(productId, tipo);
            if (idx >= 0) {
                items[idx].cantidad++;
                renderCart();
                return;
            }
        }

        items.push({
            producto_id: productId,
            nombre: nombre,
            precio: parseFloat(precio) || 0,
            cantidad: 1,
            tipo: tipo
        });
        renderCart();
    }

    function findItemIndex(productId, tipo) {
        for (var i = 0; i < items.length; i++) {
            if (items[i].tipo !== tipo) continue;
            if (items[i].producto_id == productId) return i;
        }
        return -1;
    }

    function removeItem(idx) {
        if (idx >= 0 && idx < items.length) {
            items.splice(idx, 1);
            renderCart();
        }
    }

    function updateQty(idx, delta) {
        if (idx >= 0 && idx < items.length) {
            items[idx].cantidad += delta;
            if (items[idx].cantidad <= 0) {
                items.splice(idx, 1);
            }
            renderCart();
        }
    }

    function getTotal() {
        return items.reduce(function (sum, item) {
            return sum + item.cantidad * item.precio;
        }, 0);
    }

    function renderCart() {
        var tbody = document.getElementById('cart-items');
        var totalEl = document.getElementById('cart-total');
        if (!tbody) return;

        if (items.length === 0) {
            tbody.innerHTML = '<tr><td class="text-center text-muted" colspan="6">Sin items</td></tr>';
            if (totalEl) totalEl.textContent = '$0.00';
            syncServiceCheckboxes();
            return;
        }

        var html = '';
        items.forEach(function (item, idx) {
            var subtotal = (item.cantidad * item.precio).toFixed(2);
            html += '<tr>';
            html += '<td>' + (idx + 1) + '</td>';
            html += '<td>' + escapeHtml(item.nombre) + '</td>';
            html += '<td>';
            html += '<button class="btn btn-sm btn-outline-secondary" onclick="Cotizaciones.updateQty(' + idx + ', -1)">-</button>';
            html += '<span class="mx-2">' + item.cantidad + '</span>';
            html += '<button class="btn btn-sm btn-outline-secondary" onclick="Cotizaciones.updateQty(' + idx + ', 1)">+</button>';
            html += '</td>';
            html += '<td>$' + item.precio.toFixed(2) + '</td>';
            html += '<td>$' + subtotal + '</td>';
            html += '<td><button class="btn btn-sm btn-outline-danger" onclick="Cotizaciones.removeItem(' + idx + ')"><i class="bi bi-trash"></i></button></td>';
            html += '</tr>';
        });

        tbody.innerHTML = html;
        if (totalEl) totalEl.textContent = '$' + getTotal().toFixed(2);
    }

    function getCsrfToken() {
        var input = document.querySelector('input[name="__anti-forgery-token"]');
        return input ? input.value : '';
    }

    function searchProducts(term) {
        if (searchTimeout) clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function () {
            if (!term || term.length < 2) {
                document.getElementById('search-results').innerHTML = '';
                return;
            }
            fetch('/api/cotizaciones/productos?q=' + encodeURIComponent(term), {
                method: 'GET',
                headers: {
                    'X-CSRF-Token': getCsrfToken(),
                    'x-requested-with': 'XMLHttpRequest'
                }
            })
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    if (data.ok) {
                        renderSearchResults(data.data);
                    }
                })
                .catch(function (err) {
                    console.error('Search error:', err);
                });
        }, 300);
    }

    function renderSearchResults(products) {
        var container = document.getElementById('search-results');
        if (!container) return;
        container.innerHTML = '';
        if (!products || products.length === 0) {
            container.innerHTML = '<p class="text-muted">No se encontraron productos</p>';
            return;
        }

        products.forEach(function (p) {
            var col = document.createElement('div');
            col.className = 'col-6 col-md-4 mb-2';

            var card = document.createElement('div');
            card.className = 'card h-100 border';
            card.style.cursor = 'pointer';
            card.addEventListener('click', function () {
                addItem(p.id, p.nombre, p.precio, 'producto');
                hideProductModal();
            });

            var body = document.createElement('div');
            body.className = 'card-body text-center p-2';

            var nameDiv = document.createElement('div');
            nameDiv.className = 'text-truncate';
            nameDiv.textContent = p.nombre || '';

            var categoryBadge = document.createElement('span');
            categoryBadge.className = 'badge bg-secondary';
            categoryBadge.textContent = p.categoria || 'producto';

            var priceBadge = document.createElement('span');
            priceBadge.className = 'badge bg-success ms-2';
            priceBadge.textContent = '$' + (p.precio || 0);

            body.appendChild(nameDiv);
            body.appendChild(categoryBadge);
            body.appendChild(priceBadge);
            card.appendChild(body);
            col.appendChild(card);
            container.appendChild(col);
        });
    }

    function searchClientes(term) {
        if (clientSearchTimeout) clearTimeout(clientSearchTimeout);
        clientSearchTimeout = setTimeout(function () {
            var resultsContainer = document.getElementById('cliente-results');
            if (!term || term.length < 2) {
                if (resultsContainer) resultsContainer.innerHTML = '';
                return;
            }
            fetch('/api/cotizaciones/clientes?q=' + encodeURIComponent(term), {
                method: 'GET',
                headers: {
                    'X-CSRF-Token': getCsrfToken(),
                    'x-requested-with': 'XMLHttpRequest'
                }
            })
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    if (data.ok) {
                        renderClientSuggestions(data.data);
                    }
                })
                .catch(function (err) {
                    console.error('Client search error:', err);
                });
        }, 250);
    }

    function renderClientSuggestions(clients) {
        var container = document.getElementById('cliente-results');
        if (!container) return;
        if (!clients || clients.length === 0) {
            container.innerHTML = '<div class="list-group-item text-muted">No se encontraron clientes</div>';
            return;
        }
        var html = '';
        clients.forEach(function (c) {
            var nombre = escapeHtml(c.nombre).replace(/'/g, "\\'");
            var telefono = escapeHtml(c.telefono || '').replace(/'/g, "\\'");
            html += '<button type="button" class="list-group-item list-group-item-action" onclick="Cotizaciones.selectCliente(' + c.id + ', \'' + nombre + '\', \'' + telefono + '\')">';
            html += '<div><strong>' + escapeHtml(c.nombre) + '</strong></div>';
            html += '<div class="text-muted small">' + escapeHtml(c.telefono || '') + '</div>';
            html += '</button>';
        });
        container.innerHTML = html;
    }

    function selectCliente(id, nombre, telefono) {
        var idInput = document.getElementById('cliente-id');
        var nameInput = document.getElementById('cliente-nombre');
        var phoneInput = document.getElementById('cliente-telefono');
        var searchInput = document.getElementById('cliente-search');
        var results = document.getElementById('cliente-results');

        if (idInput) idInput.value = id;
        if (nameInput) nameInput.value = nombre;
        if (phoneInput) phoneInput.value = telefono;
        if (searchInput) searchInput.value = '';
        if (results) results.innerHTML = '';
    }

    function toggleServicio(element) {
        var checked = element.checked;
        var productId = element.dataset.id;
        var nombre = element.dataset.nombre;
        var precio = parseFloat(element.dataset.precio) || 0;

        if (checked) {
            addItem(productId, nombre, precio, 'servicio');
        } else {
            removeService(productId);
        }
    }

    function removeService(productId) {
        for (var i = 0; i < items.length; i++) {
            if (items[i].tipo === 'servicio' && items[i].producto_id == productId) {
                items.splice(i, 1);
                renderCart();
                return;
            }
        }
    }

    function addMiscCharge() {
        var name = prompt('Nombre del cargo extra:');
        if (!name || !name.trim()) return;
        var priceStr = prompt('Monto del cargo:');
        var price = parseFloat(priceStr);
        if (isNaN(price) || price <= 0) {
            alert('Monto inválido');
            return;
        }
        items.push({
            producto_id: null,
            nombre: name.trim(),
            precio: price,
            cantidad: 1,
            tipo: 'misc'
        });
        renderCart();
    }

    function processToPOS() {
        var cotizacionEl = document.getElementById('cotizacion-id');
        var cotizacionId = cotizacionEl ? cotizacionEl.value : null;
        if (cotizacionId === '') cotizacionId = null;
        var transferItems = items.map(function (item) {
            return {
                producto_id: item.producto_id,
                nombre: item.nombre,
                precio: item.precio,
                cantidad: item.cantidad,
                categoria: item.tipo
            };
        });
        localStorage.setItem('cotizacion_cart', JSON.stringify({
            cotizacion_id: cotizacionId,
            items: transferItems
        }));
        window.location.href = '/pos';
    }

    function hideProductModal() {
        var modalEl = document.getElementById('searchProductModal');
        if (!modalEl) return;
        if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
            var modal = bootstrap.Modal.getInstance(modalEl);
            if (!modal) {
                modal = new bootstrap.Modal(modalEl);
            }
            modal.hide();
            return;
        }
        // fallback if Bootstrap is not available
        modalEl.classList.remove('show');
        modalEl.style.display = 'none';
        document.body.classList.remove('modal-open');
        var backdrops = document.querySelectorAll('.modal-backdrop');
        backdrops.forEach(function (backdrop) {
            backdrop.parentNode.removeChild(backdrop);
        });
    }

    function save() {
        var id = document.getElementById('cotizacion-id').value;
        var clienteId = document.getElementById('cliente-id').value;
        var nombre = document.getElementById('cliente-nombre').value;
        var telefono = document.getElementById('cliente-telefono').value;
        var notas = document.getElementById('notas').value;
        var estado = document.getElementById('estado').value;

        if (!nombre) {
            alert('Por favor ingrese el nombre del cliente');
            return;
        }

        var payload = {
            id: id || null,
            cliente_id: clienteId || null,
            cliente_nombre: nombre,
            cliente_telefono: telefono,
            notas: notas,
            estado: estado,
            items: items
        };

        var btn = document.querySelector('button.btn-success');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Guardando...';
        }

        fetch('/api/cotizaciones/guardar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': getCsrfToken(),
                'x-requested-with': 'XMLHttpRequest'
            },
            body: JSON.stringify(payload)
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.ok) {
                    alert('Cotización guardada #' + data.id);
                    window.location.href = '/cotizaciones';
                } else {
                    alert('Error: ' + (data.error || 'Error desconocido'));
                }
            })
            .catch(function (err) {
                alert('Error de red: ' + err.message);
            })
            .finally(function () {
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = 'Guardar';
                }
            });
    }

    function deleteItem(id) {
        if (!confirm('¿Está seguro de eliminar esta cotización?')) return;
        fetch('/api/cotizaciones/eliminar?id=' + id, {
            method: 'POST',
            headers: {
                'X-CSRF-Token': getCsrfToken(),
                'x-requested-with': 'XMLHttpRequest'
            }
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.ok) {
                    window.location.reload();
                } else {
                    alert('Error: ' + (data.error || 'Error al eliminar'));
                }
            })
            .catch(function (err) {
                alert('Error: ' + err.message);
            });
    }

    function refund() {
        var id = document.getElementById('cotizacion-id').value;
        if (!id) return;
        if (!confirm('¿Confirma que desea reembolsar/cancelar esta cotización?')) return;
        fetch('/api/cotizaciones/reembolsar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': getCsrfToken(),
                'x-requested-with': 'XMLHttpRequest'
            },
            body: JSON.stringify({ id: id })
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.ok) {
                    alert('Cotización reembolsada y cancelada.');
                    window.location.href = '/cotizaciones';
                } else {
                    alert('Error: ' + (data.error || 'Error al reembolsar'));
                }
            })
            .catch(function (err) {
                alert('Error: ' + err.message);
            });
    }

    function escapeHtml(str) {
        if (!str) return '';
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
        addItem: addItem,
        removeItem: removeItem,
        updateQty: updateQty,
        save: save,
        delete: deleteItem,
        searchProducts: searchProducts,
        searchClientes: searchClientes,
        selectCliente: selectCliente,
        toggleServicio: toggleServicio,
        addMiscCharge: addMiscCharge,
        processToPOS: processToPOS,
        refund: refund,
        hideProductModal: hideProductModal,
        escapeHtml: escapeHtml
    };
})();