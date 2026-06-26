-- ============================================================
-- MIGRACIÓN 002 — Autenticación y gastos por usuario
-- ============================================================

-- Tabla de perfiles
CREATE TABLE IF NOT EXISTS profiles (
    id      UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    nombre  TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Trigger más robusto que no falla si hay conflicto
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO profiles (id, nombre)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'nombre', split_part(NEW.email, '@', 1), 'Usuario')
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- Agregar columnas a gastos
ALTER TABLE gastos
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS tipo TEXT NOT NULL DEFAULT 'personal'
        CHECK (tipo IN ('personal', 'compartido'));

-- Actualizar gastos existentes con el primer usuario (si los hay)
UPDATE gastos SET user_id = (SELECT id FROM auth.users LIMIT 1)
WHERE user_id IS NULL;

-- FK apunta a profiles para que PostgREST vea la relación
ALTER TABLE gastos DROP CONSTRAINT IF EXISTS gastos_user_id_fkey;
ALTER TABLE gastos ADD CONSTRAINT gastos_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES profiles(id);

-- Activar RLS
ALTER TABLE gastos   ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Eliminar políticas anteriores
DROP POLICY IF EXISTS "acceso_total"      ON gastos;
DROP POLICY IF EXISTS "acceso_total"      ON categorias;
DROP POLICY IF EXISTS "ver_gastos"        ON gastos;
DROP POLICY IF EXISTS "insertar_gastos"   ON gastos;
DROP POLICY IF EXISTS "eliminar_gastos"   ON gastos;
DROP POLICY IF EXISTS "ver_perfiles"      ON profiles;
DROP POLICY IF EXISTS "editar_mi_perfil"  ON profiles;
DROP POLICY IF EXISTS "ver_categorias"    ON categorias;

-- Políticas de gastos
CREATE POLICY "ver_gastos" ON gastos FOR SELECT
    USING (auth.uid() = user_id OR tipo = 'compartido');

CREATE POLICY "insertar_gastos" ON gastos FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "eliminar_gastos" ON gastos FOR DELETE
    USING (auth.uid() = user_id);

-- Políticas de perfiles
CREATE POLICY "ver_perfiles" ON profiles FOR SELECT
    USING (auth.role() = 'authenticated');

CREATE POLICY "editar_mi_perfil" ON profiles FOR ALL
    USING (auth.uid() = id);

-- Categorías: acceso para todos los autenticados
ALTER TABLE categorias ENABLE ROW LEVEL SECURITY;
CREATE POLICY "ver_categorias" ON categorias FOR SELECT
    USING (auth.role() = 'authenticated');

-- Vista actualizada
DROP VIEW IF EXISTS gastos_completo;
CREATE VIEW gastos_completo AS
SELECT
    g.id,
    g.monto,
    c.nombre                                AS categoria,
    c.color                                 AS color_categoria,
    COALESCE(g.descripcion, '')             AS descripcion,
    g.fecha,
    TO_CHAR(g.fecha, 'DD/MM/YYYY HH24:MI') AS fecha_formato,
    g.fuente,
    g.tipo,
    g.user_id,
    p.nombre                                AS autor,
    COALESCE(g.yape_destinatario, '')       AS yape_destinatario,
    g.created_at
FROM gastos g
LEFT JOIN categorias c ON c.id = g.categoria_id
LEFT JOIN profiles   p ON p.id = g.user_id
ORDER BY g.fecha DESC;

-- Recargar cache de PostgREST
NOTIFY pgrst, 'reload schema';
