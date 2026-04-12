
CREATE TRIGGER inventario_cambios
AFTER UPDATE ON inventario
FOR EACH ROW
INSERT INTO icambios (
    inventario_id,
    cantidad_anterior,
    cantidad_nueva,
    fecha
)
SELECT
    OLD.id,
    OLD.cantidad,
    NEW.cantidad,
    NOW()
FROM DUAL
WHERE OLD.cantidad <> NEW.cantidad

