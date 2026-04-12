CREATE TABLE ventas (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12,2) NOT NULL DEFAULT 0,
    pago DECIMAL(12,2) NOT NULL DEFAULT 0,
    cambio DECIMAL(12,2) NOT NULL DEFAULT 0,
    usuario_id INT,
    estado ENUM('completada', 'cancelada') NOT NULL DEFAULT 'completada',
    FOREIGN KEY (usuario_id) REFERENCES users(id)
);

CREATE TABLE ventas_detalle (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
