var qrScanner = null;
var qrScannerActive = false;
var lastScannedTime = 0;

window.initQRRscanner = function() {
    if (qrScannerActive) return;
    
    var readerEl = document.getElementById('qr-reader');
    if (!readerEl) return;
    
    qrScannerActive = true;
    qrScanner = new Html5QrcodeScanner('qr-reader', { 
        fps: 10, 
        qrbox: 250, 
        rememberLastUsedCamera: true
    });
    
    qrScanner.render(function(decodedText) {
        var now = Date.now();
        if (now - lastScannedTime < 1500) return;
        lastScannedTime = now;
        
        fetch('/api/pos/parse-qr?qr=' + encodeURIComponent(decodedText))
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (data.ok && data.product) {
                    if (window.POS) {
                        POS.addItem(data.product.id);
                    }
                    // Mark for reopen
                    localStorage.setItem('reopen_qr_modal', '1');
                } else {
                    alert('Producto no encontrado');
                }
            })
            .catch(function(err) {
                console.error('Error parsing QR:', err);
            });
    });
};

function reopenQRModalIfNeeded() {
    var shouldReopen = localStorage.getItem('reopen_qr_modal');
    if (shouldReopen === '1') {
        localStorage.removeItem('reopen_qr_modal');
        
        var modalEl = document.getElementById('qrScannerModal');
        if (modalEl && !modalEl.classList.contains('show')) {
            closeQRScanner();
            var bsModal = new bootstrap.Modal(modalEl, { 
                backdrop: 'static', 
                keyboard: false 
            });
            bsModal.show();
            setTimeout(function() {
                window.initQRRscanner();
            }, 300);
        }
    }
}

function showScanFeedback(productName) {
    var feedback = document.getElementById('qr-scan-feedback');
    if (!feedback) {
        feedback = document.createElement('div');
        feedback.id = 'qr-scan-feedback';
        feedback.className = 'alert alert-success mt-2';
        feedback.style.display = 'none';
        var readerEl = document.getElementById('qr-reader');
        if (readerEl) {
            readerEl.parentNode.insertBefore(feedback, readerEl.nextSibling);
        }
    }
    feedback.textContent = '✓ Agregado: ' + productName;
    feedback.style.display = 'block';
    setTimeout(function() {
        feedback.style.display = 'none';
    }, 1500);
}

function reopenQRModal() {
    var modalEl = document.getElementById('qrScannerModal');
    if (!modalEl) return;
    
    closeQRScanner();
    
    var bsModal = new bootstrap.Modal(modalEl, { 
        backdrop: 'static', 
        keyboard: false 
    });
    bsModal.show();
    
    setTimeout(function() {
        window.initQRRscanner();
    }, 300);
}

function closeQRScanner() {
    if (qrScanner) {
        try { qrScanner.clear(); } catch(e) {}
        qrScanner = null;
    }
    qrScannerActive = false;
}

document.addEventListener('DOMContentLoaded', function() {
    var modal = document.getElementById('qrScannerModal');
    if (modal) {
        modal.addEventListener('shown.bs.modal', function() {
            window.initQRRscanner();
        });
        modal.addEventListener('hidden.bs.modal', function() {
            closeQRScanner();
        });
    }
    
    // Poll for reopen flag every 500ms
    setInterval(function() {
        var shouldReopen = localStorage.getItem('reopen_qr_modal');
        if (shouldReopen === '1') {
            localStorage.removeItem('reopen_qr_modal');
            var modalEl = document.getElementById('qrScannerModal');
            if (modalEl && !modalEl.classList.contains('show')) {
                closeQRScanner();
                var bsModal = new bootstrap.Modal(modalEl, { 
                    backdrop: 'static', 
                    keyboard: false 
                });
                bsModal.show();
                setTimeout(function() {
                    window.initQRRscanner();
                }, 500);
            }
        }
    }, 500);
});