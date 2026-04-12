create table movimientos (
    id int not null auto_increment primary key,
    producto_id int,
    tipo_movimiento enum('venta', 'compra') not null,
    fecha_movimiento DATE DEFAULT (CURRENT_DATE),
    cantidad int not null,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
)