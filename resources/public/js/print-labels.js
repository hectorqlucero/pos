var selectedIds = [];

function toggleLabel(id) {
    var cb = document.getElementById('label-' + id);
    if (cb) {
        cb.checked = !cb.checked;
        updatePrintButton();
    }
}

function updatePrintButton() {
    var checked = document.querySelectorAll('.label-checkbox:checked');
    var btn = document.getElementById('print-selected-btn');
    if (btn) btn.disabled = checked.length === 0;
    selectedIds = Array.from(checked).map(function(c) { return c.value; });
}

function selectAllLabels() {
    document.querySelectorAll('.label-checkbox').forEach(function(cb) { cb.checked = true; });
    updatePrintButton();
}

function deselectAllLabels() {
    document.querySelectorAll('.label-checkbox').forEach(function(cb) { cb.checked = false; });
    updatePrintButton();
}

function printSelectedLabels() {
    if (selectedIds.length === 0) {
        alert('Seleccione al menos un producto');
        return;
    }
    var csrf = document.querySelector('input[name="__anti-forgery-token"]');
    var token = csrf ? csrf.value : '';
    fetch('/api/pos/print-labels', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-Token': token,
            'x-requested-with': 'XMLHttpRequest'
        },
        body: JSON.stringify({producto_ids: selectedIds})
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (!data.ok) {
            alert('Error: ' + data.error);
            return;
        }
        var labelsHtml = data.labels;
        var win = window.open('', '_blank');
        win.document.write('<!DOCTYPE html><html><head><title>Etiquetas</title><link rel="stylesheet" href="/vendor/bootstrap.min.css"><link rel="stylesheet" href="/vendor/bootstrap-icons.css"><style>@media print{ .col-6{ break-inside:avoid; } }</style></head><body class="p-3"><div class="row g-3">' + labelsHtml + '</div></body></html>');
        win.document.close();
        setTimeout(function() { win.print(); }, 500);
    })
    .catch(function(err) {
        alert('Error: ' + err.message);
    });
}