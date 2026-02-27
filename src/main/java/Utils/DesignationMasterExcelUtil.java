package Utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the Designation master Excel: reads all designation names
 * and builds random Designation filter rows for automation.
 *
 * Template path: src/test/resources/DesignationMaster.xlsx
 */
public class DesignationMasterExcelUtil {

    public static final String DESIGNATION_MASTER_SHEET = "Designations";
    public static final String COL_DESIGNATION_NAME = "DesignationName";
    private static final Random RANDOM = new Random();

    /**
     * Ensure the master Excel exists; if it doesn't, create an empty template
     * with just the header so the user can fill it manually.
     */
    public static void createEmptyTemplateIfMissing(String resourcePath) {
        try {
            Path path = Paths.get(resourcePath);
            if (Files.exists(path)) {
                System.out.println("[DESIGNATION MASTER] Excel already exists: " + resourcePath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(DESIGNATION_MASTER_SHEET);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_DESIGNATION_NAME);
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[DESIGNATION MASTER] Empty template created: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[DESIGNATION MASTER] Failed to create template: " + e.getMessage());
        }
    }

    /** Reads all designation names from the DesignationMaster Excel (first column, skip header). */
    public static List<String> readDesignationNames(InputStream in) {
        List<String> names = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(DESIGNATION_MASTER_SHEET);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getCellString(row.getCell(0));
                if (!name.isEmpty()) names.add(name);
            }
        } catch (Exception e) {
            System.err.println("[DESIGNATION MASTER] Failed to read: " + e.getMessage());
        }
        return names;
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

    /**
     * Picks {@code count} random designations.
     * Returns FilterRows with FilterType=Designation, IncludeExclude=Include.
     */
    public static List<FilterRow> getRandomDesignationFilterRows(List<String> allDesignations, int count) {
        if (allDesignations == null || allDesignations.isEmpty()) return Collections.emptyList();
        List<String> shuffled = new ArrayList<>(allDesignations);
        Collections.shuffle(shuffled);
        int take = Math.min(count, shuffled.size());
        List<FilterRow> rows = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            String designation = shuffled.get(i);
            rows.add(new FilterRow("Designation", designation, null, "Include", ""));
        }
        return rows;
    }
}

