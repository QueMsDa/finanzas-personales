-- ============================================================
-- FINANZAS APP - Esquema inicial de Supabase
-- Pegar en: Supabase Dashboard > SQL Editor > New query
-- ============================================================

-- Tabla de categorías
CREATE TABLE IF NOT EXISTS categorias (
    id         BIGSERIAL PRIMARY KEY,
    nombre     TEXT NOT NULL UNIQUE,
    color      TEXT NOT NULL DEFAULT '#6200EE',
    icono      TEXT NOT NULL DEFAULT 'shopping_cart',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabla de gastos
CREATE TABLE IF NOT EXISTS gastos (
    id                BIGSERIAL PRIMARY KEY,
    monto             NUMERIC(10, 2) NOT NULL CHECK (monto > 0),
    categoria_id      BIGINT REFERENCES categorias (id) ON DELETE SET NULL,
    descripcion       TEXT,
    fecha             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fuente            TEXT NOT NULL DEFAULT 'manual' CHECK (fuente IN ('manual', 'yape')),
    yape_destinatario TEXT,
    created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para consultas rápidas
CREATE INDEX IF NOT EXISTS idx_gastos_fecha        ON gastos (fecha DESC);
CREATE INDEX IF NOT EXISTS idx_gastos_categoria_id ON gastos (categoria_id);
CREATE INDEX IF NOT EXISTS idx_gastos_fuente       ON gastos (fuente);

-- Habilitar Realtime para la tabla gastos
ALTER TABLE gastos REPLICA IDENTITY FULL;
ALTER PUBLICATION supabase_realtime ADD TABLE gastos;

-- Desactivar RLS para uso personal (una sola persona, sin autenticación)
ALTER TABLE categorias DISABLE ROW LEVEL SECURITY;
ALTER TABLE gastos     DISABLE ROW LEVEL SECURITY;

-- ============================================================
-- Categorías por defecto (Perú - Soles)
-- ============================================================
INSERT INTO categorias (nombre, color, icono) VALUES
    ('Alimentación',    '#F44336', 'restaurant'),
    ('Transporte',      '#2196F3', 'directions_car'),
    ('Entretenimiento', '#9C27B0', 'movie'),
    ('Salud',           '#4CAF50', 'local_hospital'),
    ('Educación',       '#FF9800', 'school'),
    ('Hogar',           '#795548', 'home'),
    ('Ropa',            '#E91E63', 'checkroom'),
    ('Mercado',         '#009688', 'shopping_basket'),
    ('Yape/Transferencia', '#6B27E8', 'phone_iphone'),
    ('Otros',           '#607D8B', 'more_horiz')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- Vista: Resumen por mes y categoría (para Google Sheets)
-- ============================================================
CREATE OR REPLACE VIEW resumen_mensual AS
SELECT
    DATE_TRUNC('month', g.fecha)  AS mes,
    c.nombre                       AS categoria,
    c.color                        AS color,
    SUM(g.monto)                   AS total,
    COUNT(*)                       AS cantidad
FROM gastos g
LEFT JOIN categorias c ON c.id = g.categoria_id
GROUP BY DATE_TRUNC('month', g.fecha), c.nombre, c.color
ORDER BY mes DESC, total DESC;

-- Vista: Gastos con nombre de categoría (para exportar)
CREATE OR REPLACE VIEW gastos_completo AS
SELECT
    g.id,
    g.monto,
    c.nombre                              AS categoria,
    c.color                               AS color_categoria,
    COALESCE(g.descripcion, '')           AS descripcion,
    g.fecha,
    TO_CHAR(g.fecha, 'DD/MM/YYYY HH24:MI') AS fecha_formato,
    g.fuente,
    COALESCE(g.yape_destinatario, '')     AS yape_destinatario,
    g.created_at
FROM gastos g
LEFT JOIN categorias c ON c.id = g.categoria_id
ORDER BY g.fecha DESC;
