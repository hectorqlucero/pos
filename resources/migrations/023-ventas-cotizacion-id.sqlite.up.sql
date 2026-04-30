PRAGMA foreign_keys=off;
BEGIN TRANSACTION;

ALTER TABLE ventas ADD COLUMN cotizacion_id INT;

CREATE INDEX IF NOT EXISTS idx_ventas_cotizacion_id ON ventas(cotizacion_id);

COMMIT;
PRAGMA foreign_keys=on;
