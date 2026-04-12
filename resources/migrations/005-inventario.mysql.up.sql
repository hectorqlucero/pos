create table inventario (
    id int not null auto_increment primary key,
    producto_id int,
    cantidad int default 0,
    provedor_id int,
    ultima_actualizacion date default (CURRENT_DATE),
    FOREIGN key (producto_id) REFERENCES productos(id),
    FOREIGN key (provedor_id) REFERENCES provedores(id)
)