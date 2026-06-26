' ============================================================
' FINANZAS PERSONAL — Excel VBA + Power Query
' Sincronización desde Supabase → Excel
'
' OPCIÓN A (Recomendada): Power Query
'   1. Excel → Datos → Obtener datos → Desde web
'   2. URL: https://XXXXXXXX.supabase.co/rest/v1/gastos_completo?order=fecha.desc
'   3. Encabezados: apikey = tu_anon_key
'   4. Cargar y activar actualización automática
'
' OPCIÓN B: Esta macro VBA
'   1. Alt+F11 → Insertar módulo
'   2. Pega este código
'   3. Cambia SUPABASE_URL y SUPABASE_KEY
'   4. Ejecuta SyncFinanzas() o asigna un botón
' ============================================================

Const SUPABASE_URL As String = "https://XXXXXXXXXX.supabase.co"
Const SUPABASE_KEY As String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

Sub SyncFinanzas()
    Dim wb As Workbook
    Set wb = ThisWorkbook

    Application.ScreenUpdating = False
    Application.StatusBar = "Sincronizando con Supabase..."

    On Error GoTo ErrorHandler

    Call SyncGastos(wb)
    Call SyncResumen(wb)
    Call SyncMensual(wb)

    Application.StatusBar = "✅ Sincronización completada: " & Now()
    MsgBox "Datos actualizados correctamente.", vbInformation, "Finanzas Personal"
    GoTo Cleanup

ErrorHandler:
    MsgBox "Error al sincronizar: " & Err.Description, vbCritical, "Error"

Cleanup:
    Application.ScreenUpdating = True
    Application.StatusBar = False
End Sub

' ─── Sync Gastos ──────────────────────────────────────────────────────────────
Private Sub SyncGastos(wb As Workbook)
    Dim ws As Worksheet
    On Error Resume Next
    Set ws = wb.Sheets("Gastos")
    On Error GoTo 0
    If ws Is Nothing Then Set ws = wb.Sheets.Add(After:=wb.Sheets(wb.Sheets.Count))
    ws.Name = "Gastos"

    Dim json As String
    json = FetchFromSupabase("/rest/v1/gastos_completo?order=fecha.desc")

    ws.Cells.Clear

    Dim cabeceras As Variant
    cabeceras = Array("ID", "Monto (S/)", "Categoria", "Descripcion", "Fecha", "Fuente", "Destinatario Yape")
    Dim i As Integer
    For i = 0 To UBound(cabeceras)
        ws.Cells(1, i + 1).Value = cabeceras(i)
    Next i

    ' Estilo cabecera
    With ws.Range("A1:G1")
        .Interior.Color = RGB(107, 39, 232)
        .Font.Color = RGB(255, 255, 255)
        .Font.Bold = True
    End With

    ' Parsear JSON simple (para formato básico)
    Dim items() As String
    items = ParseJsonArray(json)

    Dim row As Long
    row = 2
    Dim item As Variant
    For Each item In items
        If Len(Trim(item)) > 5 Then
            ws.Cells(row, 1).Value = ExtractField(item, "id")
            ws.Cells(row, 2).Value = CDbl(ExtractField(item, "monto"))
            ws.Cells(row, 3).Value = ExtractField(item, "categoria")
            ws.Cells(row, 4).Value = ExtractField(item, "descripcion")
            ws.Cells(row, 5).Value = Left(ExtractField(item, "fecha"), 10)
            ws.Cells(row, 6).Value = ExtractField(item, "fuente")
            ws.Cells(row, 7).Value = ExtractField(item, "yape_destinatario")
            ws.Cells(row, 2).NumberFormat = """S/ ""#,##0.00"
            row = row + 1
        End If
    Next item

    ' Total
    ws.Cells(row, 1).Value = "TOTAL"
    ws.Cells(row, 2).Formula = "=SUM(B2:B" & (row - 1) & ")"
    ws.Cells(row, 2).NumberFormat = """S/ ""#,##0.00"
    ws.Range("A" & row & ":G" & row).Interior.Color = RGB(232, 234, 246)
    ws.Range("A" & row & ":G" & row).Font.Bold = True

    ws.Columns("A:G").AutoFit
End Sub

' ─── Sync Resumen por categoría ───────────────────────────────────────────────
Private Sub SyncResumen(wb As Workbook)
    Dim ws As Worksheet
    On Error Resume Next
    Set ws = wb.Sheets("Por Categoria")
    On Error GoTo 0
    If ws Is Nothing Then Set ws = wb.Sheets.Add(After:=wb.Sheets(wb.Sheets.Count))
    ws.Name = "Por Categoria"

    Dim json As String
    json = FetchFromSupabase("/rest/v1/resumen_mensual?order=total.desc")

    ws.Cells.Clear

    Dim cabeceras As Variant
    cabeceras = Array("Mes", "Categoria", "Total (S/)", "Cantidad")
    Dim i As Integer
    For i = 0 To UBound(cabeceras)
        ws.Cells(1, i + 1).Value = cabeceras(i)
    Next i

    With ws.Range("A1:D1")
        .Interior.Color = RGB(107, 39, 232)
        .Font.Color = RGB(255, 255, 255)
        .Font.Bold = True
    End With

    Dim items() As String
    items = ParseJsonArray(json)

    Dim row As Long
    row = 2
    Dim item As Variant
    For Each item In items
        If Len(Trim(item)) > 5 Then
            ws.Cells(row, 1).Value = Left(ExtractField(item, "mes"), 7)
            ws.Cells(row, 2).Value = ExtractField(item, "categoria")
            ws.Cells(row, 3).Value = CDbl(ExtractField(item, "total"))
            ws.Cells(row, 4).Value = CLng(ExtractField(item, "cantidad"))
            ws.Cells(row, 3).NumberFormat = """S/ ""#,##0.00"
            row = row + 1
        End If
    Next item

    ws.Columns("A:D").AutoFit
End Sub

' ─── Sync Mensual ─────────────────────────────────────────────────────────────
Private Sub SyncMensual(wb As Workbook)
    Dim ws As Worksheet
    On Error Resume Next
    Set ws = wb.Sheets("Por Mes")
    On Error GoTo 0
    If ws Is Nothing Then Set ws = wb.Sheets.Add(After:=wb.Sheets(wb.Sheets.Count))
    ws.Name = "Por Mes"

    Dim json As String
    json = FetchFromSupabase("/rest/v1/resumen_mensual?select=mes,total.sum()&order=mes.desc")

    ws.Cells.Clear

    With ws.Range("A1:B1")
        ws.Cells(1, 1).Value = "Mes"
        ws.Cells(1, 2).Value = "Total (S/)"
        .Interior.Color = RGB(107, 39, 232)
        .Font.Color = RGB(255, 255, 255)
        .Font.Bold = True
    End With

    ws.Columns("A:B").AutoFit
End Sub

' ─── HTTP Request helper ───────────────────────────────────────────────────────
Private Function FetchFromSupabase(endpoint As String) As String
    Dim http As Object
    Set http = CreateObject("MSXML2.XMLHTTP.6.0")

    http.Open "GET", SUPABASE_URL & endpoint, False
    http.setRequestHeader "apikey", SUPABASE_KEY
    http.setRequestHeader "Authorization", "Bearer " & SUPABASE_KEY
    http.setRequestHeader "Content-Type", "application/json"
    http.Send

    If http.Status = 200 Then
        FetchFromSupabase = http.responseText
    Else
        Err.Raise vbObjectError, , "HTTP " & http.Status & ": " & http.responseText
    End If
End Function

' ─── Mini parser JSON (para objetos planos) ───────────────────────────────────
Private Function ParseJsonArray(json As String) As String()
    Dim parts() As String
    Dim clean As String

    ' Quita el [ ] exterior
    clean = Mid(json, 2, Len(json) - 2)

    ' Divide por },{ (simplificado — funciona para objetos sin anidamiento profundo)
    clean = Replace(clean, "},{", "}" & Chr(1) & "{")
    parts = Split(clean, Chr(1))

    ParseJsonArray = parts
End Function

Private Function ExtractField(json As String, field As String) As String
    Dim pattern As String
    pattern = """" & field & """"

    Dim pos As Long
    pos = InStr(json, pattern)
    If pos = 0 Then
        ExtractField = ""
        Exit Function
    End If

    pos = pos + Len(pattern) + 1  ' salta al ":"
    Do While Mid(json, pos, 1) = " " Or Mid(json, pos, 1) = ":"
        pos = pos + 1
    Loop

    Dim firstChar As String
    firstChar = Mid(json, pos, 1)

    Dim endPos As Long
    If firstChar = """" Then
        pos = pos + 1
        endPos = InStr(pos, json, """")
        If endPos = 0 Then endPos = Len(json)
        ExtractField = Mid(json, pos, endPos - pos)
    ElseIf firstChar = "n" Then  ' null
        ExtractField = ""
    Else
        endPos = pos
        Do While endPos <= Len(json) And _
            Mid(json, endPos, 1) <> "," And _
            Mid(json, endPos, 1) <> "}" And _
            Mid(json, endPos, 1) <> "]"
            endPos = endPos + 1
        Loop
        ExtractField = Trim(Mid(json, pos, endPos - pos))
    End If
End Function

' ─── Auto-sync al abrir el archivo ────────────────────────────────────────────
' Para activar: en el editor VBA, ve a "ThisWorkbook" y agrega:
' Private Sub Workbook_Open()
'     Call SyncFinanzas
' End Sub
