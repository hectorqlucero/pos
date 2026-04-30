PRAGMA foreign_keys=off;
BEGIN TRANSACTION;

CREATE TABLE ventas_old (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha     DATETIME DEFAULT CURRENT_TIMESTAMP,
    total     DECIMAL(12,2) NOT NULL DEFAULT 0,
    pago      DECIMAL(12,2) NOT NULL DEFAULT 0,
    cambio    DECIMAL(12,2) NOT NULL DEFAULT 0,
    usuario_id INT,
    estado    VARCHAR(20) NOT NULL DEFAULT 'completada'
              CHECK (estado IN ('completada', 'cancelada')),
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO ventas_old (id, fecha, total, pago, cambio, usuario_id, estado)
SELECT id, fecha, total, pago, cambio, usuario_id, estado FROM ventas;

DROP TABLE ventas;
ALTER TABLE ventas_old RENAME TO ventas;

CREATE INDEX idx_ventas_usuario_id ON ventas(usuario_id);

COMMIT;
PRAGMA foreign_keys=on;
