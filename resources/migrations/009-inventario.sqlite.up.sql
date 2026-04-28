CREATE TABLE inventario (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id          INT,
    cantidad             INT DEFAULT 0,
    provedor_id          INT,
    ultima_actualizacion DATE DEFAULT (date('now')),
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE,
    FOREIGN KEY (provedor_id) REFERENCES provedores(id) ON DELETE SET NULL
);

CREATE INDEX idx_inventario_producto_id ON inventario(producto_id);
CREATE INDEX idx_inventario_provedor_id ON inventario(provedor_id);
