package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.olympo_back.Student.StudentResponse;
import co.sena.edu.olympo_back.Teacher.TeacherResponse;
import co.sena.edu.olympo_back.Administrative.AdministrativeResponse;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

@Service
public class MinuteDocxService {

    private static final Logger logger = LoggerFactory.getLogger(MinuteDocxService.class);

    @Autowired
    private EmailMinuteTemplateService emailMinuteTemplateService;

    public byte[] generateMinuteDocx(CommitteeEventDto event,
                                     List<StudentResponse> students,
                                     List<TeacherResponse> teachers,
                                     List<AdministrativeResponse> administratives,
                                     List<Novelty> novelties) throws Exception {

        logger.info("[DOCX] Iniciando generación de acta con orden correcto");
        System.out.println("[DOCX] Generando acta con tablas en orden correcto...");

        try {
            Map<String, String> templateData = prepareTemplateData(event, students, teachers, administratives, novelties);
            String htmlContent = emailMinuteTemplateService.processMinuteTemplate(event, teachers, administratives, templateData);

            byte[] docxBytes = convertHtmlToDocxSimplified(htmlContent);

            logger.info("[DOCX] Acta generada exitosamente con orden correcto");
            System.out.println("[DOCX] ✅ Acta generada con tablas en orden correcto");

            return docxBytes;

        } catch (Exception e) {
            logger.error("[DOCX] Error generando acta: {}", e.getMessage(), e);
            System.out.println("[DOCX] ❌ Error: " + e.getMessage());
            throw e;
        }
    }

    private byte[] convertHtmlToDocxSimplified(String htmlContent) throws Exception {
        logger.info("[DOCX] Conversión SIN contenedor - tablas reales en orden");
        System.out.println("[DOCX] Procesando HTML SIN contenedor para tablas reales...");

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Configurar márgenes
            configureDocumentMargins(document);

            // Parsear HTML y procesar directamente en el documento
            Document doc = Jsoup.parse(htmlContent);
            Element body = doc.body();

            if (body != null) {
                processHtmlDirectlyInDocument(document, body.children());
            }

            document.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            logger.error("[DOCX] Error en conversión: {}", e.getMessage(), e);
            throw e;
        }
    }

    // NUEVA ESTRATEGIA: Procesar HTML directamente en el documento (sin contenedor)
    // para que las tablas reales aparezcan en orden correcto
    private void processHtmlDirectlyInDocument(XWPFDocument document, Elements elements) {
        logger.info("[DOCX] Procesando HTML DIRECTAMENTE en documento para orden correcto");

        for (Element element : elements) {
            String tagName = element.tagName().toLowerCase();

            switch (tagName) {
                case "div":
                    if (element.hasClass("acta-container")) {
                        processHtmlDirectlyInDocument(document, element.children());
                    } else {
                        processHtmlDirectlyInDocument(document, element.children());
                    }
                    break;

                case "h1":
                    if (element.hasClass("acta-title")) {
                        addTitleToDocument(document, element.text(), 12, true);
                    } else {
                        addTitleToDocument(document, element.text(), 14, true);
                    }
                    break;

                case "h2":
                    addSectionTitleToDocument(document, element.text());
                    break;

                case "h3":
                    addSubsectionTitleToDocument(document, element.text());
                    break;

                case "table":
                    // CREAR TABLA REAL directamente en el documento
                    logger.info("[DOCX] Creando tabla REAL en documento en posición correcta");
                    System.out.println("[DOCX] ✅ Tabla REAL en posición correcta");
                    createRealWordTableInDocument(document, element);
                    break;

                case "p":
                    addParagraphToDocument(document, element.text());
                    break;

                case "ol":
                case "ul":
                    addListToDocument(document, element);
                    break;

                default:
                    if (element.hasText() && element.children().isEmpty()) {
                        addParagraphToDocument(document, element.text());
                    } else {
                        processHtmlDirectlyInDocument(document, element.children());
                    }
                    break;
            }
        }
    }

    // Crear tabla REAL de Word directamente en el documento
    private void createRealWordTableInDocument(XWPFDocument document, Element htmlTable) {
        Elements rows = htmlTable.select("tr");
        if (rows.isEmpty()) return;

        logger.info("[DOCX] Creando tabla REAL directamente en documento");
        System.out.println("[DOCX] ✅ Creando tabla REAL de Word...");

        try {
            // Identificar si es tabla de metadatos
            boolean isMetaTable = isMetadataTable(htmlTable);

            // Calcular número de columnas (SIN columnas extra para metadatos)
            int maxCols = getActualColumns(rows, isMetaTable);

            // Crear tabla nativa REAL de Word
            XWPFTable table = document.createTable(rows.size(), maxCols);

            // Configurar propiedades de la tabla REAL
            configureRealWordTable(table);

            // Llenar la tabla con datos reales (manejando colspan correctamente)
            fillRealWordTableCorrectly(table, rows, isMetaTable);

            // Agregar espacio después de la tabla
            XWPFParagraph spaceParagraph = document.createParagraph();
            spaceParagraph.setSpacingAfter(300);

            logger.info("[DOCX] ✅ Tabla REAL creada exitosamente en documento");

        } catch (Exception e) {
            logger.error("[DOCX] ❌ Error creando tabla real: {}", e.getMessage());
            e.printStackTrace();

            // Fallback: crear párrafo con error
            XWPFParagraph errorPara = document.createParagraph();
            XWPFRun errorRun = errorPara.createRun();
            errorRun.setText("ERROR TABLA: " + extractTableData(rows));
            errorRun.setColor("FF0000");
        }
    }

    // Calcular columnas REALES sin contar celdas vacías de colspan
    private int getActualColumns(Elements rows, boolean isMetaTable) {
        if (isMetaTable) {
            // Para tabla de metadatos, usar número de celdas con contenido real
            int maxActualCells = 0;
            for (Element row : rows) {
                Elements cells = row.select("th, td");
                int actualCells = 0;
                for (Element cell : cells) {
                    if (!cleanText(cell.text()).isEmpty()) {
                        actualCells++;
                    }
                }
                maxActualCells = Math.max(maxActualCells, actualCells);
            }
            return Math.max(maxActualCells, 2); // Mínimo 2 columnas para metadatos
        } else {
            // Para tablas estándar, usar el método anterior
            return getMaxColumns(rows);
        }
    }

    // Llenar tabla considerando colspan CORRECTAMENTE
    private void fillRealWordTableCorrectly(XWPFTable table, Elements htmlRows, boolean isMetaTable) {
        for (int rowIndex = 0; rowIndex < htmlRows.size(); rowIndex++) {
            Element htmlRow = htmlRows.get(rowIndex);
            Elements htmlCells = htmlRow.select("th, td");
            XWPFTableRow wordRow = table.getRow(rowIndex);

            if (isMetaTable) {
                fillMetadataRowCorrectly(wordRow, htmlCells, rowIndex == 0);
            } else {
                fillStandardRowCorrectly(wordRow, htmlCells, rowIndex == 0);
            }
        }
    }

    // Llenar fila de metadatos CORRECTAMENTE sin columnas extra
    private void fillMetadataRowCorrectly(XWPFTableRow wordRow, Elements htmlCells, boolean isHeader) {
        int cellIndex = 0;

        for (Element htmlCell : htmlCells) {
            String cellText = cleanText(htmlCell.text());
            if (!cellText.isEmpty() && cellIndex < wordRow.getTableCells().size()) {
                XWPFTableCell wordCell = wordRow.getCell(cellIndex);

                // Configurar contenido de celda
                configureRealWordCell(wordCell, htmlCell, isHeader);

                // Manejar colspan si existe
                String colspan = htmlCell.attr("colspan");
                if (!colspan.isEmpty()) {
                    int colSpan = Integer.parseInt(colspan);
                    if (colSpan > 1 && cellIndex + colSpan <= wordRow.getTableCells().size()) {
                        // Mergear celdas horizontalmente
                        for (int i = 0; i < colSpan; i++) {
                            XWPFTableCell cell = wordRow.getCell(cellIndex + i);
                            CTTcPr tcPr = cell.getCTTc().getTcPr();
                            if (tcPr == null) {
                                tcPr = cell.getCTTc().addNewTcPr();
                            }

                            CTHMerge hMerge = tcPr.addNewHMerge();
                            if (i == 0) {
                                hMerge.setVal(STMerge.RESTART);
                            } else {
                                hMerge.setVal(STMerge.CONTINUE);
                            }
                        }
                        cellIndex += colSpan;
                    } else {
                        cellIndex++;
                    }
                } else {
                    cellIndex++;
                }
            }
        }
    }

    // Llenar fila estándar
    private void fillStandardRowCorrectly(XWPFTableRow wordRow, Elements htmlCells, boolean isHeader) {
        for (int cellIndex = 0; cellIndex < htmlCells.size() && cellIndex < wordRow.getTableCells().size(); cellIndex++) {
            Element htmlCell = htmlCells.get(cellIndex);
            XWPFTableCell wordCell = wordRow.getCell(cellIndex);

            // Configurar contenido de celda REAL
            configureRealWordCell(wordCell, htmlCell, isHeader);

            // Manejar colspan si existe
            handleRealColspan(wordRow, cellIndex, htmlCell);
        }
    }

    private void configureRealWordCell(XWPFTableCell cell, Element htmlCell, boolean isHeader) {
        // Limpiar párrafos existentes
        cell.removeParagraph(0);

        // Crear párrafo para el contenido
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);

        // Crear run con el texto
        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(htmlCell.text()));
        run.setFontFamily("Arial");

        if (isHeader) {
            // Estilo para headers
            run.setBold(true);
            run.setFontSize(10);

            // Fondo gris para headers
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }

            CTShd shd = tcPr.addNewShd();
            shd.setVal(STShd.CLEAR);
            shd.setColor("auto");
            shd.setFill("D9D9D9"); // Gris claro
        } else {
            // Estilo para celdas normales
            run.setFontSize(9);
        }
    }

    private void handleRealColspan(XWPFTableRow row, int cellIndex, Element htmlCell) {
        String colspan = htmlCell.attr("colspan");
        if (!colspan.isEmpty()) {
            try {
                int colSpan = Integer.parseInt(colspan);
                if (colSpan > 1) {
                    // Mergear celdas horizontalmente
                    for (int i = 0; i < colSpan && (cellIndex + i) < row.getTableCells().size(); i++) {
                        XWPFTableCell cell = row.getCell(cellIndex + i);
                        CTTcPr tcPr = cell.getCTTc().getTcPr();
                        if (tcPr == null) {
                            tcPr = cell.getCTTc().addNewTcPr();
                        }

                        CTHMerge hMerge = tcPr.addNewHMerge();
                        if (i == 0) {
                            hMerge.setVal(STMerge.RESTART);
                        } else {
                            hMerge.setVal(STMerge.CONTINUE);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn("[DOCX] Error procesando colspan: {}", colspan);
            }
        }
    }

    // Método para obtener el máximo de columnas de las filas HTML
    private int getMaxColumns(Elements rows) {
        int maxCols = 0;
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            int colCount = 0;
            for (Element cell : cells) {
                String colspan = cell.attr("colspan");
                colCount += colspan.isEmpty() ? 1 : Integer.parseInt(colspan);
            }
            maxCols = Math.max(maxCols, colCount);
        }
        return Math.max(maxCols, 1);
    }

    private void configureRealWordTable(XWPFTable table) {
        // Configurar propiedades básicas de la tabla
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }

        // Ancho de tabla al 100%
        CTTblWidth tblWidth = tblPr.addNewTblW();
        tblWidth.setW(BigInteger.valueOf(5000));
        tblWidth.setType(STTblWidth.PCT);

        // Bordes de la tabla REALES
        CTTblBorders borders = tblPr.addNewTblBorders();
        addRealTableBorder(borders.addNewTop());
        addRealTableBorder(borders.addNewBottom());
        addRealTableBorder(borders.addNewLeft());
        addRealTableBorder(borders.addNewRight());
        addRealTableBorder(borders.addNewInsideH());
        addRealTableBorder(borders.addNewInsideV());

        // Espaciado entre celdas
        CTTblCellMar cellMar = tblPr.addNewTblCellMar();
        cellMar.addNewTop().setW(BigInteger.valueOf(100));
        cellMar.addNewBottom().setW(BigInteger.valueOf(100));
        cellMar.addNewLeft().setW(BigInteger.valueOf(150));
        cellMar.addNewRight().setW(BigInteger.valueOf(150));
    }

    private void addRealTableBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(8)); // Grosor de borde
        border.setColor("000000"); // Color negro
    }

    // Métodos para agregar contenido directamente al documento
    private void addTitleToDocument(XWPFDocument document, String text, int fontSize, boolean center) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(center ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(200);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text));
        run.setBold(true);
        run.setFontSize(fontSize);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Título agregado: {}", text);
    }

    private void addSectionTitleToDocument(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(280);
        paragraph.setSpacingAfter(120);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text).toUpperCase());
        run.setBold(true);
        run.setFontSize(11);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Sección agregada: {}", text);
    }

    private void addSubsectionTitleToDocument(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(200);
        paragraph.setSpacingAfter(120);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text).toUpperCase());
        run.setBold(true);
        run.setFontSize(10);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Subsección agregada: {}", text);
    }

    private void addParagraphToDocument(XWPFDocument document, String text) {
        if (text == null || text.trim().isEmpty()) return;

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.BOTH);
        paragraph.setSpacingAfter(100);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text));
        run.setFontSize(9);
        run.setFontFamily("Arial");
    }

    private void addListToDocument(XWPFDocument document, Element listElement) {
        Elements items = listElement.select("li");
        boolean isOrdered = listElement.tagName().equals("ol");

        for (int i = 0; i < items.size(); i++) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(400);
            paragraph.setSpacingAfter(80);

            XWPFRun run = paragraph.createRun();
            String prefix = isOrdered ? (i + 1) + ". " : "• ";
            run.setText(prefix + cleanText(items.get(i).text()));
            run.setFontSize(9);
            run.setFontFamily("Arial");
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    // NUEVA ESTRATEGIA: Procesar tablas EN SU LUGAR correcto, DENTRO del contenedor
    private void processHtmlInOrder(XWPFTableCell containerCell, XWPFDocument document, Elements elements) {
        logger.info("[DOCX] Procesando HTML manteniendo orden EXACTO de elementos");

        for (Element element : elements) {
            String tagName = element.tagName().toLowerCase();

            switch (tagName) {
                case "div":
                    if (element.hasClass("acta-container")) {
                        processHtmlInOrder(containerCell, document, element.children());
                    } else {
                        processElementInContainer(containerCell, element, document);
                    }
                    break;

                case "h1":
                    if (element.hasClass("acta-title")) {
                        addTitle(containerCell, element.text(), 12, true);
                    } else {
                        addTitle(containerCell, element.text(), 14, true);
                    }
                    break;

                case "h2":
                    addSectionTitle(containerCell, element.text());
                    break;

                case "h3":
                    addSubsectionTitle(containerCell, element.text());
                    break;

                case "table":
                    // ARREGLAR: Crear tabla DENTRO del contenedor, no fuera
                    logger.info("[DOCX] Creando tabla DENTRO del contenedor en posición correcta");
                    System.out.println("[DOCX] ✅ Tabla DENTRO del contenedor");
                    createTableInsideContainer(containerCell, element);
                    break;

                case "p":
                    addParagraph(containerCell, element.text());
                    break;

                case "ol":
                case "ul":
                    addList(containerCell, element);
                    break;

                default:
                    if (element.hasText() && element.children().isEmpty()) {
                        addParagraph(containerCell, element.text());
                    } else {
                        processElementInContainer(containerCell, element, document);
                    }
                    break;
            }
        }
    }

    // NUEVO: Crear tabla DENTRO del contenedor usando párrafos estructurados
    private void createTableInsideContainer(XWPFTableCell containerCell, Element htmlTable) {
        Elements rows = htmlTable.select("tr");
        if (rows.isEmpty()) return;

        logger.info("[DOCX] Creando tabla visual DENTRO del contenedor");
        System.out.println("[DOCX] ✅ Creando tabla visual estructurada...");

        try {
            // Identificar si es la tabla de metadatos por su contenido
            boolean isMetaTable = isMetadataTable(htmlTable);

            // Agregar espacio antes de la tabla
            XWPFParagraph spaceBefore = containerCell.addParagraph();
            spaceBefore.setSpacingAfter(150);

            if (isMetaTable) {
                createMetadataTableInContainer(containerCell, rows);
            } else {
                createStandardTableInContainer(containerCell, rows);
            }

            // Agregar espacio después de la tabla
            XWPFParagraph spaceAfter = containerCell.addParagraph();
            spaceAfter.setSpacingAfter(200);

            logger.info("[DOCX] ✅ Tabla visual creada dentro del contenedor");

        } catch (Exception e) {
            logger.error("[DOCX] ❌ Error creando tabla visual: {}", e.getMessage());
            // Fallback: mostrar datos de la tabla como texto
            XWPFParagraph errorPara = containerCell.addParagraph();
            XWPFRun errorRun = errorPara.createRun();
            errorRun.setText("DATOS DE TABLA: " + extractTableData(rows));
            errorRun.setFontSize(8);
        }
    }

    // Identificar si es la tabla de metadatos
    private boolean isMetadataTable(Element htmlTable) {
        Elements rows = htmlTable.select("tr");
        if (rows.size() > 0) {
            Element firstRow = rows.get(0);
            Elements cells = firstRow.select("th, td");
            for (Element cell : cells) {
                String text = cell.text().toUpperCase();
                if (text.contains("CIUDAD") || text.contains("FECHA") || text.contains("HORA")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Crear tabla de metadatos DENTRO del contenedor (sin columnas extra)
    private void createMetadataTableInContainer(XWPFTableCell containerCell, Elements rows) {
        logger.info("[DOCX] Creando tabla de metadatos estructurada");

        // Marco superior especial para metadatos
        createTableBorderInContainer(containerCell, "╔═══════════════════════════════════════════════════════════════════════════╗");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element htmlRow = rows.get(rowIndex);
            Elements cells = htmlRow.select("th, td");

            // Para metadatos, procesar solo las celdas que tienen contenido
            List<String> cellTexts = new ArrayList<>();
            for (Element cell : cells) {
                String cellText = cleanText(cell.text());
                String colspan = cell.attr("colspan");

                if (!cellText.isEmpty()) {
                    // Si tiene colspan, expandir el ancho
                    if (!colspan.isEmpty()) {
                        int colSpan = Integer.parseInt(colspan);
                        cellTexts.add(expandTextForColspan(cellText, colSpan));
                    } else {
                        cellTexts.add(cellText);
                    }
                }
            }

            if (!cellTexts.isEmpty()) {
                createMetadataRowInContainer(containerCell, cellTexts, rowIndex == 0);

                // Línea separadora después del header
                if (rowIndex == 0 && rows.size() > 1) {
                    createTableBorderInContainer(containerCell, "╠═══════════════════════════════════════════════════════════════════════════╣");
                } else if (rowIndex == 1 && rows.size() > 2) {
                    createTableBorderInContainer(containerCell, "╠═══════════════════════════════════════════════════════════════════════════╣");
                }
            }
        }

        // Marco inferior
        createTableBorderInContainer(containerCell, "╚═══════════════════════════════════════════════════════════════════════════╝");
    }

    // Expandir texto para colspan
    private String expandTextForColspan(String text, int colspan) {
        if (colspan <= 1) return text;

        // Calcular ancho total considerando el colspan
        int baseWidth = 30; // Ancho base por columna
        int totalWidth = baseWidth * colspan + (colspan - 1) * 3; // +3 por separadores

        if (text.length() > totalWidth) {
            return text.substring(0, totalWidth - 3) + "...";
        }

        // Centrar en el ancho total
        int padding = (totalWidth - text.length()) / 2;
        int rightPadding = totalWidth - text.length() - padding;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }

    // Crear fila de metadatos
    private void createMetadataRowInContainer(XWPFTableCell containerCell, List<String> cellTexts, boolean isHeader) {
        XWPFParagraph rowPara = containerCell.addParagraph();
        rowPara.setAlignment(ParagraphAlignment.CENTER);
        rowPara.setSpacingBefore(isHeader ? 100 : 60);
        rowPara.setSpacingAfter(isHeader ? 100 : 60);

        StringBuilder rowContent = new StringBuilder("║");

        for (int i = 0; i < cellTexts.size(); i++) {
            String cellText = cellTexts.get(i);
            rowContent.append(" ").append(cellText).append(" ║");
        }

        XWPFRun rowRun = rowPara.createRun();
        rowRun.setText(rowContent.toString());
        rowRun.setFontFamily("Courier New");

        if (isHeader) {
            rowRun.setBold(true);
            rowRun.setFontSize(10);
        } else {
            rowRun.setFontSize(9);
        }
    }

    // Crear tabla estándar DENTRO del contenedor
    private void createStandardTableInContainer(XWPFTableCell containerCell, Elements rows) {
        logger.info("[DOCX] Creando tabla estándar estructurada");

        // Marco superior
        createTableBorderInContainer(containerCell, "┌─────────────────────────────────────────────────────────────────────────┐");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element htmlRow = rows.get(rowIndex);
            Elements cells = htmlRow.select("th, td");

            createStandardRowInContainer(containerCell, cells, rowIndex == 0);

            // Línea separadora después del header
            if (rowIndex == 0 && rows.size() > 1) {
                createTableBorderInContainer(containerCell, "├─────────────────────────────────────────────────────────────────────────┤");
            }
        }

        // Marco inferior
        createTableBorderInContainer(containerCell, "└─────────────────────────────────────────────────────────────────────────┘");
    }

    // Crear fila estándar
    private void createStandardRowInContainer(XWPFTableCell containerCell, Elements cells, boolean isHeader) {
        XWPFParagraph rowPara = containerCell.addParagraph();
        rowPara.setAlignment(ParagraphAlignment.CENTER);
        rowPara.setSpacingBefore(isHeader ? 80 : 50);
        rowPara.setSpacingAfter(isHeader ? 80 : 50);

        StringBuilder rowContent = new StringBuilder("│");

        // Anchos estándar para tabla de novedades
        int[] columnWidths = {4, 8, 8, 12, 20, 16, 10, 15}; // Ajustado para evitar overflow

        for (int i = 0; i < cells.size() && i < columnWidths.length; i++) {
            String cellText = cleanText(cells.get(i).text());
            String formattedCell = formatCellForWidth(cellText, columnWidths[i]);
            rowContent.append(" ").append(formattedCell).append(" │");
        }

        XWPFRun rowRun = rowPara.createRun();
        rowRun.setText(rowContent.toString());
        rowRun.setFontFamily("Courier New");

        if (isHeader) {
            rowRun.setBold(true);
            rowRun.setFontSize(9);
        } else {
            rowRun.setFontSize(8);
        }
    }

    // Formatear texto para ancho específico
    private String formatCellForWidth(String text, int width) {
        if (text == null) text = "";

        if (text.length() > width) {
            return text.substring(0, width - 3) + "...";
        }

        // Centrar texto
        int padding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - padding;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }

    // Crear borde de tabla dentro del contenedor
    private void createTableBorderInContainer(XWPFTableCell containerCell, String borderText) {
        XWPFParagraph borderPara = containerCell.addParagraph();
        borderPara.setAlignment(ParagraphAlignment.CENTER);
        borderPara.setSpacingBefore(0);
        borderPara.setSpacingAfter(0);

        XWPFRun borderRun = borderPara.createRun();
        borderRun.setText(borderText);
        borderRun.setFontFamily("Courier New");
        borderRun.setFontSize(8);
        borderRun.setColor("000000");
    }

    private Map<String, String> prepareTemplateData(CommitteeEventDto event,
                                                    List<StudentResponse> students,
                                                    List<TeacherResponse> teachers,
                                                    List<AdministrativeResponse> administratives,
                                                    List<Novelty> novelties) {

        logger.info("[DOCX] Preparando datos REALES del comité (no ficticios)");
        System.out.println("[DOCX] Preparando datos REALES del comité...");
        Map<String, String> data = new HashMap<>();

        // Datos del evento
        if (event != null) {
            data.put("eventId", event.getId() != null ? String.valueOf(event.getId()) : "");
            data.put("eventDate", event.getDate() != null ? event.getDate().toString() : LocalDate.now().toString());
            data.put("eventHour", event.getHour() != null ? event.getHour().toString() : "08:00:00");
            data.put("eventHourInicio", event.getHour() != null ? event.getHour().toString() : "08:00:00");
            data.put("eventHourFin", event.getHour() != null ?
                    event.getHour().toString().replace("08:", "10:").replace("14:", "16:") : "10:00:00");
            data.put("eventSession", event.getSession() != null ? event.getSession() : "Ordinaria");
            data.put("coordinationName", event.getCoordinationName() != null ? event.getCoordinationName() : "Coordinación Académica");
            data.put("committeeId", event.getCommittee() != null && event.getCommittee().getId() != null ?
                    String.valueOf(event.getCommittee().getId()) : "");
        }

        // Datos adicionales para la plantilla
        data.put("actaNumero", String.format("%03d", LocalDate.now().getDayOfYear()));
        data.put("actaAnio", String.valueOf(LocalDate.now().getYear()));
        data.put("nombreReunion", "COMITÉ ACADÉMICO DE TECNOLOGÍA E INNOVACIÓN");
        data.put("eventCity", "Medellín");
        data.put("eventPlace", "Aula Virtual - Microsoft Teams");
        data.put("eventLocation", "SENA - Centro de Tecnología e Innovación - Regional Antioquia");
        data.put("meetingObjectives", "Evaluar y resolver las novedades académicas de los aprendices del centro");
        data.put("trasladoConformidad", "conformidad del coordinador académico correspondiente.");
        data.put("conclusionesHtml", "Se aprobaron las novedades presentadas conforme al reglamento del aprendiz SENA.");

        // Generar datos REALES de las novedades y estudiantes
        data.put("reingresosRows", generateRealReingresosRows(students, novelties));
        data.put("retirosAprobadosRows", generateRealRetirosAprobadosRows(students, novelties));
        data.put("retirosNoAprobadosRows", generateRealRetirosNoAprobadosRows(students, novelties));
        data.put("trasladosAprobadosRows", generateRealTrasladosAprobadosRows(students, novelties));
        data.put("trasladosNoAprobadosRows", generateRealTrasladosNoAprobadosRows(students, novelties));
        data.put("aplazamientosRows", generateRealAplazamientosRows(students, novelties));
        data.put("levantamientosRows", generateRealLevantamientosRows(students, novelties));
        data.put("compromisosRows", generateRealCompromisosRows(event));
        data.put("asistentesRows", generateRealAsistentesRows(teachers, administratives));

        logger.info("[DOCX] Datos REALES preparados: {} placeholders configurados", data.size());
        System.out.println("[DOCX] ✅ Datos REALES preparados: " + data.size() + " placeholders configurados");

        return data;
    }

    // Generar filas de REINGRESOS usando datos reales de los protos
    private String generateRealReingresosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "REINGRESO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    "APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName())) {

                    // Buscar el estudiante correspondiente usando la estructura real del proto
                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>") // Ficha no disponible en el proto actual
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>") // Nivel por defecto
                            .append("<td>").append("Programa Académico").append("</td>") // Programa por defecto
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin reingresos registrados</td></tr>";
    }

    // Generar filas de RETIROS APROBADOS usando datos reales de los protos
    private String generateRealRetirosAprobadosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "RETIRO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    "APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName())) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>")
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin retiros aprobados</td></tr>";
    }

    // Generar filas de RETIROS NO APROBADOS usando datos reales de los protos
    private String generateRealRetirosNoAprobadosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "RETIRO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    ("RECHAZADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()) ||
                     "NO_APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()) ||
                     "DENEGADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()))) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>")
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin retiros no aprobados</td></tr>";
    }

    // Generar filas de TRASLADOS APROBADOS usando datos reales de los protos
    private String generateRealTrasladosAprobadosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "TRASLADO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    "APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName())) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>") // Ficha origen
                            .append("<td>").append(novelty.getStudySheetId() != null ? novelty.getStudySheetId() : "N/A").append("</td>") // Ficha destino
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='9' style='text-align: center; font-style: italic;'>Sin traslados aprobados</td></tr>";
    }

    // Generar filas de TRASLADOS NO APROBADOS usando datos reales de los protos
    private String generateRealTrasladosNoAprobadosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "TRASLADO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    ("RECHAZADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()) ||
                     "NO_APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()) ||
                     "DENEGADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()))) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>")
                            .append("<td>").append(novelty.getStudySheetId() != null ? novelty.getStudySheetId() : "N/A").append("</td>")
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='9' style='text-align: center; font-style: italic;'>Sin traslados no aprobados</td></tr>";
    }

    // Generar filas de APLAZAMIENTOS usando datos reales de los protos
    private String generateRealAplazamientosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    "APLAZAMIENTO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) &&
                    novelty.getNoveltyStatus() != null &&
                    ("RECHAZADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()) ||
                     "NO_APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName()))) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>")
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin aplazamientos rechazados</td></tr>";
    }

    // Generar filas de LEVANTAMIENTOS usando datos reales de los protos
    private String generateRealLevantamientosRows(List<StudentResponse> students, List<Novelty> novelties) {
        StringBuilder rows = new StringBuilder();
        int contador = 1;

        if (novelties != null && students != null) {
            for (Novelty novelty : novelties) {
                if (novelty.getNoveltyType() != null &&
                    ("LEVANTAMIENTO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty()) ||
                     "LEVANTAMIENTO_APLAZAMIENTO".equalsIgnoreCase(novelty.getNoveltyType().getNameNovelty())) &&
                    novelty.getNoveltyStatus() != null &&
                    "APROBADO".equalsIgnoreCase(novelty.getNoveltyStatus().getName())) {

                    StudentResponse student = findStudentById(students, novelty.getStudentId());
                    if (student != null && student.getPerson() != null) {
                        PersonResponse person = student.getPerson();

                        rows.append("<tr>")
                            .append("<td>").append(contador++).append("</td>")
                            .append("<td>").append("N/A").append("</td>")
                            .append("<td>").append(person.getDocumentType() != null ? person.getDocumentType().getName() : "CC").append("</td>")
                            .append("<td>").append(String.valueOf(person.getDocument())).append("</td>")
                            .append("<td>").append(cleanText(person.getName() + " " + person.getLastname())).append("</td>")
                            .append("<td>").append(person.getEmail() != null ? person.getEmail() : "").append("</td>")
                            .append("<td>").append("Tecnólogo").append("</td>")
                            .append("<td>").append("Programa Académico").append("</td>")
                            .append("</tr>");
                    }
                }
            }
        }

        return rows.length() > 0 ? rows.toString() :
            "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin levantamientos registrados</td></tr>";
    }

    // Método auxiliar para encontrar estudiante por ID usando estructura real del proto
    private StudentResponse findStudentById(List<StudentResponse> students, Long studentId) {
        if (students == null || studentId == null) return null;

        return students.stream()
            .filter(student -> student != null && student.getId() == studentId) // Usar == para long
            .findFirst()
            .orElse(null);
    }

    // Generar compromisos REALES basados en el evento (corregir tipo de fecha)
    private String generateRealCompromisosRows(CommitteeEventDto event) {
        StringBuilder rows = new StringBuilder();

        // Compromisos estándar basados en la fecha del evento
        LocalDate eventDate;
        if (event != null && event.getDate() != null) {
            // event.getDate() devuelve String, parsearlo a LocalDate
            eventDate = LocalDate.parse(event.getDate());
        } else {
            eventDate = LocalDate.now();
        }

        rows.append("<tr class='firma-rows'>")
            .append("<td>Registro de novedades académicas aprobadas en Sofia Plus</td>")
            .append("<td>").append(eventDate.plusDays(1)).append("</td>")
            .append("<td>Coordinador Académico</td>")
            .append("<td style='height: 40px;'></td>")
            .append("</tr>");

        rows.append("<tr class='firma-rows'>")
            .append("<td>Notificación a aprendices sobre decisiones tomadas</td>")
            .append("<td>").append(eventDate.plusDays(3)).append("</td>")
            .append("<td>Coordinador Académico</td>")
            .append("<td style='height: 40px;'></td>")
            .append("</tr>");

        if (event != null && event.getCoordinationName() != null) {
            rows.append("<tr class='firma-rows'>")
                .append("<td>Seguimiento a estudiantes con novedades aprobadas</td>")
                .append("<td>").append(eventDate.plusWeeks(1)).append("</td>")
                .append("<td>").append(event.getCoordinationName()).append("</td>")
                .append("<td style='height: 40px;'></td>")
                .append("</tr>");
        }

        return rows.toString();
    }

    // Generar asistentes REALES del comité usando estructura real de los protos
    private String generateRealAsistentesRows(List<TeacherResponse> teachers, List<AdministrativeResponse> administratives) {
        StringBuilder rows = new StringBuilder();

        // Agregar coordinador académico por defecto (siempre presente en comités)
        rows.append("<tr class='firma-rows'>")
                .append("<td>COORDINADOR ACADÉMICO</td>")
                .append("<td>SENA - Centro de Tecnología e Innovación</td>")
                .append("<td>SI</td>")
                .append("<td>Aprobado</td>")
                .append("<td style='height: 40px;'></td>")
                .append("</tr>");

        // Agregar instructores reales (verificar estructura del proto)
        if (teachers != null && !teachers.isEmpty()) {
            for (TeacherResponse teacher : teachers) {
                if (teacher != null && teacher.hasCollaborator() && teacher.getCollaborator().getPerson() != null) {
                    PersonResponse person = teacher.getCollaborator().getPerson();
                    String fullName = cleanText(person.getName() + " " + person.getLastname());

                    rows.append("<tr class='firma-rows'>")
                            .append("<td>").append(fullName).append("</td>")
                            .append("<td>SENA - Instructor</td>")
                            .append("<td>SI</td>")
                            .append("<td>Aprobado</td>")
                            .append("<td style='height: 40px;'></td>")
                            .append("</tr>");
                }
            }
        }

        // Agregar personal administrativo real (verificar estructura del proto)
        if (administratives != null && !administratives.isEmpty()) {
            for (AdministrativeResponse admin : administratives) {
                if (admin != null && admin.hasCollaborator() && admin.getCollaborator().getPerson() != null) {
                    PersonResponse person = admin.getCollaborator().getPerson();
                    String fullName = cleanText(person.getName() + " " + person.getLastname());

                    rows.append("<tr class='firma-rows'>")
                            .append("<td>").append(fullName).append("</td>")
                            .append("<td>SENA - Apoyo Administrativo</td>")
                            .append("<td>SI</td>")
                            .append("<td>Aprobado</td>")
                            .append("<td style='height: 40px;'></td>")
                            .append("</tr>");
                }
            }
        }

        return rows.toString();
    }

    private void processElementInContainer(XWPFTableCell containerCell, Element element, XWPFDocument document) {
        String tagName = element.tagName().toLowerCase();

        switch (tagName) {
            case "div":
                if (element.hasClass("acta-container")) {
                    processHtmlInOrder(containerCell, document, element.children());
                } else {
                    for (Element child : element.children()) {
                        processElementInContainer(containerCell, child, document);
                    }
                }
                break;

            case "h1":
                if (element.hasClass("acta-title")) {
                    addTitle(containerCell, element.text(), 12, true);
                } else {
                    addTitle(containerCell, element.text(), 14, true);
                }
                break;

            case "h2":
                addSectionTitle(containerCell, element.text());
                break;

            case "h3":
                addSubsectionTitle(containerCell, element.text());
                break;

            case "table":
                // Las tablas también se procesan dentro del contenedor cuando aparecen en elementos internos
                createTableInsideContainer(containerCell, element);
                break;

            case "p":
                addParagraph(containerCell, element.text());
                break;

            case "ol":
            case "ul":
                addList(containerCell, element);
                break;

            default:
                if (element.hasText() && element.children().isEmpty()) {
                    addParagraph(containerCell, element.text());
                } else {
                    for (Element child : element.children()) {
                        processElementInContainer(containerCell, child, document);
                    }
                }
                break;
        }
    }

    private String extractTableData(Elements rows) {
        StringBuilder data = new StringBuilder();
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            for (Element cell : cells) {
                data.append(cleanText(cell.text())).append(" | ");
            }
            data.append(" / ");
        }
        return data.toString();
    }

    private void addTitle(XWPFTableCell cell, String text, int fontSize, boolean center) {
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(center ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(200);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text));
        run.setBold(true);
        run.setFontSize(fontSize);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Título agregado: {}", text);
    }

    private void addSectionTitle(XWPFTableCell cell, String text) {
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(280);
        paragraph.setSpacingAfter(120);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text).toUpperCase());
        run.setBold(true);
        run.setFontSize(11);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Sección agregada: {}", text);
    }

    private void addSubsectionTitle(XWPFTableCell cell, String text) {
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(200);
        paragraph.setSpacingAfter(120);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text).toUpperCase());
        run.setBold(true);
        run.setFontSize(10);
        run.setFontFamily("Arial");

        logger.info("[DOCX] Subsección agregada: {}", text);
    }

    private void addParagraph(XWPFTableCell cell, String text) {
        if (text == null || text.trim().isEmpty()) return;

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.BOTH);
        paragraph.setSpacingAfter(100);

        XWPFRun run = paragraph.createRun();
        run.setText(cleanText(text));
        run.setFontSize(9);
        run.setFontFamily("Arial");
    }

    private void addList(XWPFTableCell cell, Element listElement) {
        Elements items = listElement.select("li");
        boolean isOrdered = listElement.tagName().equals("ol");

        for (int i = 0; i < items.size(); i++) {
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.setIndentationLeft(400);
            paragraph.setSpacingAfter(80);

            XWPFRun run = paragraph.createRun();
            String prefix = isOrdered ? (i + 1) + ". " : "• ";
            run.setText(prefix + cleanText(items.get(i).text()));
            run.setFontSize(9);
            run.setFontFamily("Arial");
        }
    }

    private void addBorder(CTBorder border, int size) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(size));
        border.setColor("000000");
    }

    private void configureDocumentMargins(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();

        // Configurar márgenes
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(800));
        pageMar.setBottom(BigInteger.valueOf(800));
        pageMar.setLeft(BigInteger.valueOf(800));
        pageMar.setRight(BigInteger.valueOf(800));

        // NUEVO: Agregar RECUADRO a toda la página usando bordes de página
        CTPageBorders pageBorders = sectPr.addNewPgBorders();
        pageBorders.setOffsetFrom(STPageBorderOffset.PAGE);

        // Configurar borde superior - MÁS DELGADO
        CTBorder topBorder = pageBorders.addNewTop();
        topBorder.setVal(STBorder.SINGLE);
        topBorder.setSz(BigInteger.valueOf(12)); // Grosor del recuadro más delgado (era 24)
        topBorder.setColor("000000");
        topBorder.setSpace(BigInteger.valueOf(24)); // Espacio desde el borde de página

        // Configurar borde inferior - MÁS DELGADO
        CTBorder bottomBorder = pageBorders.addNewBottom();
        bottomBorder.setVal(STBorder.SINGLE);
        bottomBorder.setSz(BigInteger.valueOf(12)); // Más delgado
        bottomBorder.setColor("000000");
        bottomBorder.setSpace(BigInteger.valueOf(24));

        // Configurar borde izquierdo - MÁS DELGADO
        CTBorder leftBorder = pageBorders.addNewLeft();
        leftBorder.setVal(STBorder.SINGLE);
        leftBorder.setSz(BigInteger.valueOf(12)); // Más delgado
        leftBorder.setColor("000000");
        leftBorder.setSpace(BigInteger.valueOf(24));

        // Configurar borde derecho - MÁS DELGADO
        CTBorder rightBorder = pageBorders.addNewRight();
        rightBorder.setVal(STBorder.SINGLE);
        rightBorder.setSz(BigInteger.valueOf(12)); // Más delgado
        rightBorder.setColor("000000");
        rightBorder.setSpace(BigInteger.valueOf(24));

        logger.info("[DOCX] ✅ Recuadro de página configurado con bordes delgados (12pt)");
        System.out.println("[DOCX] ✅ Recuadro DELGADO aplicado a toda la página");
    }
}
