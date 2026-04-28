CREATE TABLE orden_servicios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    id_orden INTEGER,
    id_servicio INTEGER,
    FOREIGN KEY (id_orden) REFERENCES ordenes_trabajo(id),
    FOREIGN KEY (id_servicio) REFERENCES servicios(id)
);