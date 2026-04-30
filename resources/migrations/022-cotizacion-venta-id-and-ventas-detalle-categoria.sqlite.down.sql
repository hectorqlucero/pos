PRAGMA foreign_keys=off;
BEGIN TRANSACTION;

CREATE TABLE cotizaciones_old (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha       DATETIME DEFAULT CURRENT_TIMESTAMP,
    cliente_nombre VARCHAR(100),
    cliente_telefono VARCHAR(20),
    total       DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado      VARCHAR(20) NOT NULL DEFAULT 'borrador'
                CHECK (estado IN ('borrador', 'enviada', 'aceptada', 'rechazada', 'cancelada')),
    notas       TEXT,
    usuario_id  INT,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO cotizaciones_old (id, fecha, cliente_nombre, cliente_telefono, total, estado, notas, usuario_id)
SELECT id, fecha, cliente_nombre, cliente_telefono, total, estado, notas, usuario_id
FROM cotizaciones;

DROP TABLE cotizaciones;
ALTER TABLE cotizaciones_old RENAME TO cotizaciones;

CREATE TABLE ventas_detalle_old (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id        INT NOT NULL,
    producto_id     INT,
    nombre          VARCHAR(200) NOT NULL,
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (venta_id)    REFERENCES ventas(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

INSERT INTO ventas_detalle_old (id, venta_id, producto_id, nombre, cantidad, precio_unitario, subtotal)
SELECT id, venta_id, producto_id, nombre, cantidad, precio_unitario, subtotal
FROM ventas_detalle;

DROP TABLE ventas_detalle;
ALTER TABLE ventas_detalle_old RENAME TO ventas_detalle;

CREATE INDEX idx_ventas_detalle_venta_id ON ventas_detalle(venta_id);
CREATE INDEX idx_ventas_detalle_producto_id ON ventas_detalle(producto_id);

COMMIT;
PRAGMA foreign_keys=on;
