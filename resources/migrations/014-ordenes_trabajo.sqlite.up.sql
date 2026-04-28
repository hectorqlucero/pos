CREATE TABLE ordenes_trabajo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_orden TEXT,
    id_cliente INTEGER,
    id_bicicleta INTEGER,
    fecha DATE,
    motivo_ingreso TEXT,
    diagnostico TEXT,
    costo_mano_obra REAL,
    total_estimado REAL,
    fecha_entrega_estimada DATE,
    autorizado INTEGER DEFAULT 0, -- 0 = No, 1 = Sí
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id),
    FOREIGN KEY (id_bicicleta) REFERENCES bicicletas(id)
);