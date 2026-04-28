-- SQLite does not support DROP COLUMN directly in older versions
-- For simplicity, we recreate the table without the column
-- This is a destructive operation - in production, consider using a more complex approach
CREATE TABLE productos_backup AS SELECT id, nombre, precio, categoria, imagen FROM productos;
DROP TABLE productos;
ALTER TABLE productos_backup RENAME TO productos;