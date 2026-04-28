create table clientes (
    id integer primary key autoincrement,
    nombre text,
    telefono text,
    email text,
    direccion text,
    rfc text,
    fecha_registro text default (datetime('now')),
    activo integer default 1
);