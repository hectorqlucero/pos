create table icambios (
    id int not null auto_increment primary key,
    inventario_id int not null,
    cantidad_anterior int not null,
    cantidad_nueva int not null,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)