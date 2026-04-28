CREATE TABLE orden_productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_orden INTEGER,
    id_producto INTEGER,
    cantidad INTEGER,
    precio_unitario REAL,
    total REAL,
    FOREIGN KEY (id_orden) REFERENCES ordenes_trabajo(id),
    FOREIGN KEY (id_producto) REFERENCES productos(id)
);