CREATE TABLE productos (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre   VARCHAR(255),
    precio   DECIMAL(10,2),
    categoria VARCHAR(255),
    imagen   VARCHAR(255)
);
