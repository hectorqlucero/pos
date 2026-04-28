(function () {
    'use strict';

    function init() {
        initSelectAll();
        initPrintLabelsButton();
    }

    function initSelectAll() {
        var selectAllCheckbox = document.querySelector('.select-all-checkbox');
        if (!selectAllCheckbox) return;

        selectAllCheckbox.addEventListener('change', function () {
            var isChecked = this.checked;
            document.querySelectorAll('.product-checkbox').forEach(function (checkbox) {
                checkbox.checked = isChecked;
            });
            updatePrintButtonState();
        });

        document.querySelectorAll('.product-checkbox').forEach(function (checkbox) {
            checkbox.addEventListener('change', function () {
                updatePrintButtonState();
            });
        });
    }

    function updatePrintButtonState() {
        var checkedCount = document.querySelectorAll('.product-checkbox:checked').length;
        var printBtn = document.querySelector('.print-labels-btn');
        if (printBtn) {
            printBtn.disabled = checkedCount === 0;
        }
    }

    function initPrintLabelsButton() {
        var printBtn = document.querySelector('.print-labels-btn');
        if (!printBtn) return;

        printBtn.addEventListener('click', function () {
            var checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
            if (checkedBoxes.length === 0) {
                alert('Seleccione al menos un producto para imprimir etiquetas.');
                return;
            }

            var productoIds = Array.from(checkedBoxes).map(function (cb) {
                return cb.value;
            });

            printBtn.disabled = true;
            printBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Generando...';

            var csrfInput = document.querySelector('input[name="__anti-forgery-token"]');
            var csrfToken = csrfInput ? csrfInput.value : '';

            fetch('/api/pos/print-labels', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': csrfToken,
                    'x-requested-with': 'XMLHttpRequest'
                },
                body: JSON.stringify({ producto_ids: productoIds })
            })
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (err) {
                        throw new Error(err.error || 'Error al generar etiquetas');
                    });
                }
                return response.text();
            })
            .then(function (html) {
                var printWindow = window.open('', '_blank');
                if (!printWindow) {
                    alert('El bloqueador de ventanas emergentes impide abrir la impresión.');
                    return;
                }
                printWindow.document.write(html);
                printWindow.document.close();
                setTimeout(function () {
                    printWindow.focus();
                    printWindow.print();
                }, 250);
            })
            .catch(function (err) {
                alert('Error: ' + err.message);
            })
            .finally(function () {
                printBtn.disabled = false;
                printBtn.innerHTML = '<i class="bi bi-qr-code me-2"></i>Imprimir Etiquetas';
            });
        });

        printBtn.disabled = true;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();