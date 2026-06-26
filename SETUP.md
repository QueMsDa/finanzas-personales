# Finanzas Personal — Guía de Configuración

## Arquitectura

```
[Android App] ←──Realtime──→ [Supabase PostgreSQL]
                                      │
                    ┌─────────────────┴───────────────────┐
                    ↓                                       ↓
           [Google Sheets]                          [Excel + VBA]
           (auto-sync 5min)                    (sync manual o al abrir)
```

---

## PASO 1 — Crear base de datos en Supabase (gratis)

1. Ve a **supabase.com** → "Start your project" → crea cuenta con Google
2. Crea un nuevo proyecto:
   - **Nombre**: `finanzas-personal`
   - **Contraseña DB**: anótala (no la necesitarás directamente)
   - **Región**: `South America (São Paulo)` — más cercana a Perú
3. Espera ~2 minutos a que el proyecto se inicialice
4. Ve a **SQL Editor** → "New query"
5. Copia y pega el contenido del archivo `supabase/migrations/001_initial_schema.sql`
6. Presiona **Run** (▶)

### Obtener tus credenciales:
- Ve a **Settings** → **API**
- Copia:
  - **Project URL**: algo como `https://abcdefgh.supabase.co`
  - **anon public key**: empieza con `eyJ...`

---

## PASO 2 — Configurar la App Android

### Requisitos
- **Android Studio** (descarga en developer.android.com/studio)
- **JDK 11+** (viene incluido en Android Studio)

### Pasos:
1. Abre Android Studio
2. **File → Open** → selecciona la carpeta `FinanzasApp/android/`
3. Copia el archivo `local.properties.example` → renómbralo `local.properties`
4. Edita `local.properties` y reemplaza:
   ```properties
   SUPABASE_URL=https://TU-PROYECTO.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
5. Conecta tu teléfono Android con USB (activa "Depuración USB" en Ajustes → Opciones de desarrollador)
6. En Android Studio: **Run → Run 'app'** (▶)

### Sin cable USB (instalar por WiFi):
- En Android Studio: **Run → Pair Devices Using WiFi**

---

## PASO 3 — Configurar Google Sheets

1. Crea una hoja de Google Sheets nueva (drive.google.com)
2. Ve a **Extensiones → Apps Script**
3. Borra el código que hay y pega el contenido de `sheets/sync.gs`
4. Reemplaza al inicio del script:
   ```javascript
   const SUPABASE_URL      = 'https://TU-PROYECTO.supabase.co';
   const SUPABASE_ANON_KEY = 'eyJ...tu-clave...';
   ```
5. Guarda (Ctrl+S)
6. En el menú de Scripts, ejecuta **setupTrigger** (solo una vez)
   - Te pedirá permisos → acepta todo
7. Desde ahora, la hoja se actualizará **cada 5 minutos** automáticamente
8. También puedes ir al menú **💰 Finanzas → 🔄 Sincronizar ahora**

### Hojas que se crean automáticamente:
| Hoja | Contenido |
|------|-----------|
| 📋 Gastos | Todos los gastos con fecha, categoría, fuente |
| 📊 Por Categoría | Totales agrupados por categoría |
| 📅 Por Mes | Resumen mensual de gastos |
| ⚙️ Categorías | Lista de categorías disponibles |

---

## PASO 4 — Configurar Excel (opcional)

### Opción A: Power Query (recomendada, sin código)
1. Excel → **Datos → Obtener datos → Desde otras fuentes → Desde web**
2. URL: `https://TU-PROYECTO.supabase.co/rest/v1/gastos_completo?order=fecha.desc`
3. Click "Avanzado" → agrega encabezado:
   - Nombre: `apikey` — Valor: `tu_anon_key`
4. Cargar tabla → listo
5. Para auto-actualizar: clic derecho en la tabla → **Propiedades de rango de datos** → activar "Actualizar al abrir archivo"

### Opción B: Macro VBA
1. En Excel: **Alt + F11** → Insertar → Módulo
2. Pega el contenido de `excel/sync.vba`
3. Cambia `SUPABASE_URL` y `SUPABASE_KEY` en las constantes al inicio
4. Ejecuta la macro `SyncFinanzas`

---

## Uso de la App Android

### Agregar gasto manual
1. Tab **"Agregar"** (ícono +)
2. Ingresa monto, selecciona categoría, descripción opcional
3. Toca **"Guardar Gasto"**

### Importar captura de Yape
1. Tab **"Yape"** (ícono teléfono)
2. Toca "Seleccionar captura de Yape"
3. Elige la captura de pantalla de tu galería
4. La app detecta automáticamente el monto y el destinatario
5. Puedes editar si es necesario
6. Toca **"Guardar gasto de Yape"**

### Ver historial
1. Tab **"Historial"**
2. Busca por texto o filtra por categoría
3. Desliza para eliminar (aparece un diálogo de confirmación)

### Dashboard
- Muestra el total gastado en el **mes actual**
- Gráfico de barras por categoría
- Últimos 5 gastos

---

## Categorías por defecto
- 🍽️ Alimentación
- 🚗 Transporte  
- 🎬 Entretenimiento
- 🏥 Salud
- 📚 Educación
- 🏠 Hogar
- 👕 Ropa
- 🛒 Mercado
- 📱 Yape/Transferencia
- ➕ Otros

---

## Preguntas frecuentes

**¿Los datos se sincronizan en tiempo real?**
Sí, la app usa Supabase Realtime (WebSocket). Si hay un cambio desde otro dispositivo o la PC, la app lo refleja inmediatamente.

**¿Funciona sin internet?**
No por ahora — todos los datos se guardan directamente en Supabase. Se puede agregar caché local en una versión futura.

**¿Es seguro dejar la anon key en la app?**
La anon key de Supabase solo permite operaciones permitidas por las políticas (en este caso, acceso completo porque es uso personal). No da acceso a la consola de Supabase ni a otros proyectos.

**¿El OCR de Yape funciona con todas las versiones?**
Funciona con las capturas de la versión actual de Yape Perú. El patrón buscado es `S/ XX.XX`. Si el formato cambia, edita `YapeRecognizer.kt` para ajustar el regex.
