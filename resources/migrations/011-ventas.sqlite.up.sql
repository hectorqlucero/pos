CREATE TABLE ventas (
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

CREATE INDEX idx_ventas_usuario_id ON ventas(usuario_id);

CREATE TABLE ventas_detalle (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id        INT NOT NULL,
    producto_id     INT NOT NULL,
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (venta_id)    REFERENCES ventas(id)    ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE INDEX idx_ventas_detalle_venta_id    ON ventas_detalle(venta_id);
CREATE INDEX idx_ventas_detalle_producto_id ON ventas_detalle(producto_id);
