CREATE TABLE movimientos (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id      INT,
    tipo_movimiento  VARCHAR(10) NOT NULL CHECK (tipo_movimiento IN ('venta', 'compra')),
    fecha_movimiento DATE DEFAULT (date('now')),
    cantidad         INT NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE INDEX idx_movimientos_producto_id ON movimientos(producto_id);
