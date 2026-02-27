package Utils;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the Advanced Filter test data Excel template (columns + sample values)
 * and reads filter rows for automation.
 * Template path: src/test/resources/AdvancedFilterTestData.xlsx
 */
public class AdvancedFilterExcelUtil {

    public static final String TEMPLATE_SHEET_NAME = "Filters";
    public static final String COL_FILTER_TYPE = "FilterType";
    public static final String COL_FILTER_VALUE = "FilterValue";
    public static final String COL_TYPE = "Type";
    public static final String COL_INCLUDE_EXCLUDE = "IncludeExclude";
    public static final String COL_NOTES = "Notes";

    /**
     * Creates the Excel template with headers and sample rows if the file does not exist.
     * Type: Current + Past / Current / Past. IncludeExclude: Include / Exclude (default Include).
     */
    public static void createTemplateIfMissing(String resourcePath) {
        try {
            Path path = Paths.get(resourcePath);
            if (Files.exists(path)) {
                System.out.println("[EXCEL] Template already exists: " + resourcePath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(TEMPLATE_SHEET_NAME);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_FILTER_TYPE);
                headerRow.createCell(1).setCellValue(COL_FILTER_VALUE);
                headerRow.createCell(2).setCellValue(COL_TYPE);
                headerRow.createCell(3).setCellValue(COL_INCLUDE_EXCLUDE);
                headerRow.createCell(4).setCellValue(COL_NOTES);

                String[][] sampleRows = {
                    { "Company", "TCS", "Current + Past", "Include", "" },
                    { "Company", "TCS", "Current", "Include", "" },
                    { "Company", "TCS", "Past", "Include", "" },
                    { "Location", "Mumbai", "Current + Past", "Include", "" },
                    { "Designation", "Java Developer", "Current + Past", "Include", "" },
                };
                for (int i = 0; i < sampleRows.length; i++) {
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < sampleRows[i].length; j++) {
                        row.createCell(j).setCellValue(sampleRows[i][j]);
                    }
                }
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[EXCEL] Template created: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[EXCEL] Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Reads all filter rows from the Excel file (skips header, skips empty rows).
     * Columns: 0=FilterType, 1=FilterValue, 2=Type, 3=IncludeExclude, 4=Notes.
     * If only 3 columns present (old format), 2=Notes and Type/IncludeExclude default to Current + Past / Include.
     */
    public static List<FilterRow> readFilterRows(InputStream in) {
        List<FilterRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(TEMPLATE_SHEET_NAME);
            if (sheet == null) sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            int numCols = headerRow != null ? headerRow.getLastCellNum() : 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String filterType = getCellString(row.getCell(0));
                String filterValue = getCellString(row.getCell(1));
                if (filterType.isEmpty() && filterValue.isEmpty()) continue;
                String type = null;
                String includeExclude = null;
                String notes = null;
                if (numCols >= 5) {
                    type = getCellString(row.getCell(2));
                    includeExclude = getCellString(row.getCell(3));
                    notes = getCellString(row.getCell(4));
                } else {
                    notes = getCellString(row.getCell(2));
                }
                rows.add(new FilterRow(filterType, filterValue, type, includeExclude, notes));
            }
        } catch (Exception e) {
            System.err.println("[EXCEL] Failed to read: " + e.getMessage());
        }
        return rows;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    /** Run this to create the template in src/test/resources (e.g. from IDE). */
    public static void main(String[] args) {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "AdvancedFilterTestData.xlsx").toString();
        createTemplateIfMissing(path);
    }
}
