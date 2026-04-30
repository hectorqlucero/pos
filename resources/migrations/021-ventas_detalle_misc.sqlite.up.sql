PRAGMA foreign_keys=off;
BEGIN TRANSACTION;

CREATE TABLE ventas_detalle_new (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id        INT NOT NULL,
    producto_id     INT,
    nombre          VARCHAR(200) NOT NULL DEFAULT '',
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (venta_id)    REFERENCES ventas(id)    ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

INSERT INTO ventas_detalle_new (id, venta_id, producto_id, nombre, cantidad, precio_unitario, subtotal)
SELECT id, venta_id, producto_id, '', cantidad, precio_unitario, subtotal
FROM ventas_detalle;

DROP TABLE ventas_detalle;
ALTER TABLE ventas_detalle_new RENAME TO ventas_detalle;

CREATE INDEX idx_ventas_detalle_venta_id    ON ventas_detalle(venta_id);
CREATE INDEX idx_ventas_detalle_producto_id ON ventas_detalle(producto_id);

COMMIT;
PRAGMA foreign_keys=on;
