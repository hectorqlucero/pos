// Add label printing functionality to productos grid
(function() {
    'use strict';

    function init() {
        // Only run on productos page
        if (!window.location.pathname.includes('/admin/productos')) {
            return;
        }

        // Wait for table to be rendered
        setTimeout(addLabelPrintingControls, 500);
    }

    function addLabelPrintingControls() {
        var table = document.querySelector('table.dataTable');
        if (!table) {
            // Try again if not found
            setTimeout(addLabelPrintingControls, 500);
            return;
        }

        // Check if controls already added
        if (document.querySelector('.label-print-controls')) {
            return;
        }

        var container = document.createElement('div');
        container.className = 'label-print-controls mb-3';
        container.innerHTML = '<button class="btn btn-primary print-labels-btn" disabled><i class="bi bi-qr-code"></i> Imprimir Etiquetas</button>';

        var tableWrapper = table.closest('.table-responsive');
        if (tableWrapper) {
            tableWrapper.parentNode.insertBefore(container, tableWrapper);
        }

        // Add checkboxes to each row
        var thead = table.querySelector('thead tr');
        if (thead) {
            var th = document.createElement('th');
            th.style.width = '40px';
            th.innerHTML = '<input type="checkbox" class="select-all-checkbox">';
            thead.insertBefore(th, thead.firstChild);
        }

        var tbody = table.querySelector('tbody');
        if (tbody) {
            var rows = tbody.querySelectorAll('tr');
            rows.forEach(function(row) {
                var td = document.createElement('td');
                td.style.textAlign = 'center';
                td.innerHTML = '<input type="checkbox" class="product-checkbox" value="">';
                row.insertBefore(td, row.firstChild);
            });
        }

        // Bind events
        initCheckboxEvents(container);
    }

    function initCheckboxEvents(container) {
        var printBtn = container.querySelector('.print-labels-btn');
        var selectAll = container.querySelector('.select-all-checkbox');

        if (!printBtn || !selectAll) {
            return;
        }

        selectAll.addEventListener('change', function() {
            var checked = this.checked;
            document.querySelectorAll('.product-checkbox').forEach(function(cb) {
                cb.checked = checked;
            });
            updateButtonState();
        });

        document.addEventListener('change', function(e) {
            if (e.target.classList.contains('product-checkbox')) {
                updateButtonState();
            }
        });

        printBtn.addEventListener('click', function() {
            var checked = document.querySelectorAll('.product-checkbox:checked');
            if (checked.length === 0) {
                alert('Seleccione al menos un producto');
                return;
            }

            var ids = Array.from(checked).map(function(cb) {
                return cb.value;
            });

            printBtn.disabled = true;
            printBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Generando...';

            var csrf = document.querySelector('input[name="__anti-forgery-token"]');
            var token = csrf ? csrf.value : '';

            fetch('/api/pos/print-labels', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': token,
                    'x-requested-with': 'XMLHttpRequest'
                },
                body: JSON.stringify({producto_ids: ids})
            })
            .then(function(res) { return res.text(); })
            .then(function(html) {
                var win = window.open('', '_blank');
                win.document.write(html);
                win.document.close();
                setTimeout(function() { win.print(); }, 500);
            })
            .catch(function(err) {
                alert('Error: ' + err.message);
            })
            .finally(function() {
                printBtn.disabled = false;
                printBtn.innerHTML = '<i class="bi bi-qr-code"></i> Imprimir Etiquetas';
            });
        });
    }

    function updateButtonState() {
        var checked = document.querySelectorAll('.product-checkbox:checked').length;
        var btn = document.querySelector('.print-labels-btn');
        if (btn) btn.disabled = checked === 0;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();