create table productos (
    id int not null auto_increment primary key,
    nombre varchar(255),
    precio decimal(10,2),
    categoria varchar(255),
    imagen varchar(255)
);
