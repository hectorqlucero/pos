create table bicicletas (
    id integer primary key autoincrement,
    id_cliente integer,
    marca_modelo text,
    tipo text,
    numero_serie text,
    anio_aproximado integer,
     foreign key (id_cliente) references clientes(id)
);