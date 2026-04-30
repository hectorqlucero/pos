CREATE TABLE cotizaciones (
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

CREATE INDEX idx_cotizaciones_fecha ON cotizaciones(fecha);
CREATE INDEX idx_cotizaciones_estado ON cotizaciones(estado);
CREATE INDEX idx_cotizaciones_cliente_nombre ON cotizaciones(cliente_nombre);
CREATE INDEX idx_cotizaciones_cliente_telefono ON cotizaciones(cliente_telefono);

CREATE TABLE cotizaciones_detalle (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    cotizacion_id   INT NOT NULL,
    producto_id     INT,
    nombre          VARCHAR(200) NOT NULL,
    cantidad        INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    tipo            VARCHAR(20) NOT NULL DEFAULT 'producto'
                CHECK (tipo IN ('servicio', 'producto', 'misc')),
    FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE SET NULL
);

CREATE INDEX idx_cotizaciones_detalle_cotizacion_id ON cotizaciones_detalle(cotizacion_id);
CREATE INDEX idx_cotizaciones_detalle_producto_id ON cotizaciones_detalle(producto_id);