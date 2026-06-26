// ============================================================
// FINANZAS PERSONAL — Google Apps Script
// Sincronización automática desde Supabase → Google Sheets
//
// INSTRUCCIONES:
// 1. Abre Google Sheets → Extensiones → Apps Script
// 2. Pega este código completo
// 3. Reemplaza SUPABASE_URL y SUPABASE_ANON_KEY con tus valores
// 4. Ejecuta setupTrigger() una sola vez para activar sync automático
// 5. La hoja se actualizará cada 5 minutos
// ============================================================

const SUPABASE_URL      = 'https://XXXXXXXXXX.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'; // tu anon key

// ─── Punto de entrada principal ───────────────────────────────────────────────
function syncToSheets() {
  try {
    const gastos     = fetchGastos();
    const categorias = fetchCategorias();

    actualizarHojaGastos(gastos);
    actualizarHojaResumen(gastos);
    actualizarHojaMensual(gastos);
    actualizarHojaCategorias(categorias);

    Logger.log(`✅ Sincronización completada: ${gastos.length} gastos`);
  } catch (e) {
    Logger.log('❌ Error: ' + e.message);
  }
}

// ─── Fetch de datos ────────────────────────────────────────────────────────────
function fetchGastos() {
  const url = `${SUPABASE_URL}/rest/v1/gastos_completo?order=fecha.desc`;
  const resp = UrlFetchApp.fetch(url, headers());
  return JSON.parse(resp.getContentText());
}

function fetchCategorias() {
  const url = `${SUPABASE_URL}/rest/v1/categorias?order=nombre.asc`;
  const resp = UrlFetchApp.fetch(url, headers());
  return JSON.parse(resp.getContentText());
}

function headers() {
  return {
    headers: {
      'apikey': SUPABASE_ANON_KEY,
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json'
    }
  };
}

// ─── Hoja: Gastos ─────────────────────────────────────────────────────────────
function actualizarHojaGastos(gastos) {
  const ss    = SpreadsheetApp.getActiveSpreadsheet();
  let   sheet = ss.getSheetByName('📋 Gastos') || ss.insertSheet('📋 Gastos');

  sheet.clearContents();
  sheet.clearFormats();

  const cabeceras = ['ID', 'Monto (S/)', 'Categoría', 'Descripción', 'Fecha', 'Hora', 'Fuente', 'Destinatario Yape'];
  sheet.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);

  // Formato de cabecera
  const headerRange = sheet.getRange(1, 1, 1, cabeceras.length);
  headerRange.setBackground('#6B27E8').setFontColor('#FFFFFF').setFontWeight('bold');

  if (gastos.length > 0) {
    const filas = gastos.map(g => {
      const fecha = new Date(g.fecha);
      return [
        g.id,
        g.monto,
        g.categoria || 'Sin categoría',
        g.descripcion || '',
        Utilities.formatDate(fecha, 'America/Lima', 'dd/MM/yyyy'),
        Utilities.formatDate(fecha, 'America/Lima', 'HH:mm'),
        g.fuente,
        g.yape_destinatario || ''
      ];
    });

    sheet.getRange(2, 1, filas.length, cabeceras.length).setValues(filas);

    // Formato columna Monto
    sheet.getRange(2, 2, filas.length, 1).setNumberFormat('"S/ "#,##0.00');

    // Color filas Yape
    gastos.forEach((g, i) => {
      if (g.fuente === 'yape') {
        sheet.getRange(i + 2, 1, 1, cabeceras.length).setBackground('#F3E5F5');
      }
    });
  }

  sheet.autoResizeColumns(1, cabeceras.length);
  agregarTotalFinal(sheet, gastos.length + 1, 2, gastos.length + 2);
}

// ─── Hoja: Resumen por Categoría ──────────────────────────────────────────────
function actualizarHojaResumen(gastos) {
  const ss    = SpreadsheetApp.getActiveSpreadsheet();
  let   sheet = ss.getSheetByName('📊 Por Categoría') || ss.insertSheet('📊 Por Categoría');

  sheet.clearContents();
  sheet.clearFormats();

  const cabeceras = ['Categoría', 'Total (S/)', '# Gastos', '% del Total'];
  sheet.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);
  sheet.getRange(1, 1, 1, cabeceras.length)
    .setBackground('#6B27E8').setFontColor('#FFFFFF').setFontWeight('bold');

  const totalGlobal = gastos.reduce((s, g) => s + g.monto, 0);

  const agrupado = {};
  gastos.forEach(g => {
    const cat = g.categoria || 'Sin categoría';
    if (!agrupado[cat]) agrupado[cat] = { total: 0, count: 0 };
    agrupado[cat].total += g.monto;
    agrupado[cat].count++;
  });

  const filas = Object.entries(agrupado)
    .sort((a, b) => b[1].total - a[1].total)
    .map(([cat, data]) => [
      cat,
      data.total,
      data.count,
      totalGlobal > 0 ? data.total / totalGlobal : 0
    ]);

  if (filas.length > 0) {
    sheet.getRange(2, 1, filas.length, 4).setValues(filas);
    sheet.getRange(2, 2, filas.length, 1).setNumberFormat('"S/ "#,##0.00');
    sheet.getRange(2, 4, filas.length, 1).setNumberFormat('0.0%');
  }

  // Fila de total
  const totalRow = filas.length + 2;
  sheet.getRange(totalRow, 1, 1, 4).setValues([['TOTAL', totalGlobal, gastos.length, '100%']]);
  sheet.getRange(totalRow, 1, 1, 4).setFontWeight('bold').setBackground('#E8EAF6');
  sheet.getRange(totalRow, 2, 1, 1).setNumberFormat('"S/ "#,##0.00');

  sheet.autoResizeColumns(1, 4);
}

// ─── Hoja: Resumen Mensual ────────────────────────────────────────────────────
function actualizarHojaMensual(gastos) {
  const ss    = SpreadsheetApp.getActiveSpreadsheet();
  let   sheet = ss.getSheetByName('📅 Por Mes') || ss.insertSheet('📅 Por Mes');

  sheet.clearContents();
  sheet.clearFormats();

  const cabeceras = ['Mes', 'Total (S/)', '# Gastos', 'Promedio por gasto'];
  sheet.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);
  sheet.getRange(1, 1, 1, cabeceras.length)
    .setBackground('#6B27E8').setFontColor('#FFFFFF').setFontWeight('bold');

  const porMes = {};
  gastos.forEach(g => {
    const fecha = new Date(g.fecha);
    const key   = Utilities.formatDate(fecha, 'America/Lima', 'yyyy-MM');
    const label = Utilities.formatDate(fecha, 'America/Lima', 'MMMM yyyy');
    if (!porMes[key]) porMes[key] = { label, total: 0, count: 0 };
    porMes[key].total += g.monto;
    porMes[key].count++;
  });

  const filas = Object.entries(porMes)
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([_, data]) => [
      data.label,
      data.total,
      data.count,
      data.count > 0 ? data.total / data.count : 0
    ]);

  if (filas.length > 0) {
    sheet.getRange(2, 1, filas.length, 4).setValues(filas);
    sheet.getRange(2, 2, filas.length, 1).setNumberFormat('"S/ "#,##0.00');
    sheet.getRange(2, 4, filas.length, 1).setNumberFormat('"S/ "#,##0.00');
  }

  sheet.autoResizeColumns(1, 4);
}

// ─── Hoja: Categorías ─────────────────────────────────────────────────────────
function actualizarHojaCategorias(categorias) {
  const ss    = SpreadsheetApp.getActiveSpreadsheet();
  let   sheet = ss.getSheetByName('⚙️ Categorías') || ss.insertSheet('⚙️ Categorías');

  sheet.clearContents();
  sheet.clearFormats();

  const cabeceras = ['ID', 'Nombre', 'Color', 'Ícono'];
  sheet.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);
  sheet.getRange(1, 1, 1, cabeceras.length)
    .setBackground('#6B27E8').setFontColor('#FFFFFF').setFontWeight('bold');

  if (categorias.length > 0) {
    const filas = categorias.map(c => [c.id, c.nombre, c.color, c.icono]);
    sheet.getRange(2, 1, filas.length, 4).setValues(filas);
  }

  sheet.autoResizeColumns(1, 4);
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function agregarTotalFinal(sheet, row, col, numGastos) {
  if (numGastos <= 0) return;
  sheet.getRange(row, col - 1).setValue('TOTAL');
  sheet.getRange(row, col).setFormula(`=SUM(B2:B${numGastos})`);
  sheet.getRange(row, col).setNumberFormat('"S/ "#,##0.00');
  sheet.getRange(row, col - 1, 1, 2).setFontWeight('bold').setBackground('#E8EAF6');
}

// ─── Configurar trigger automático ────────────────────────────────────────────
function setupTrigger() {
  // Elimina triggers anteriores de esta función
  ScriptApp.getProjectTriggers()
    .filter(t => t.getHandlerFunction() === 'syncToSheets')
    .forEach(t => ScriptApp.deleteTrigger(t));

  // Trigger cada 5 minutos
  ScriptApp.newTrigger('syncToSheets')
    .timeBased()
    .everyMinutes(5)
    .create();

  Logger.log('✅ Trigger configurado: sincronización cada 5 minutos');
  syncToSheets(); // Sincronización inmediata
}

// Ejecutar sync manual desde el menú
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('💰 Finanzas')
    .addItem('🔄 Sincronizar ahora', 'syncToSheets')
    .addItem('⚙️ Activar auto-sync (5 min)', 'setupTrigger')
    .addToUi();
}
