PRAGMA foreign_keys=off;
BEGIN TRANSACTION;

ALTER TABLE cotizaciones ADD COLUMN venta_id INT;
ALTER TABLE ventas_detalle ADD COLUMN categoria VARCHAR(20) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_cotizaciones_venta_id ON cotizaciones(venta_id);

COMMIT;
PRAGMA foreign_keys=on;
