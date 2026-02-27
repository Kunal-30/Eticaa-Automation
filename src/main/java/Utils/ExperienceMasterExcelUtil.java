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

/**
 * Manages the Experience master Excel: reads min/max year ranges
 * and builds random experience filter rows for automation.
 *
 * Template path: src/test/resources/ExperienceMaster.xlsx
 * Columns: MinYears, MaxYears
 */
public class ExperienceMasterExcelUtil {

    public static final String EXPERIENCE_MASTER_SHEET = "ExperienceRanges";
    public static final String COL_MIN_YEARS = "MinYears";
    public static final String COL_MAX_YEARS = "MaxYears";

    /** Default ranges to write when creating template (same as previously hardcoded). */
    private static final int[][] DEFAULT_RANGES = { {0, 1}, {2, 5}, {6, 10}, {11, 20} };

    /**
     * Ensure the master Excel exists; if it doesn't, create a template
     * with header and default min/max ranges.
     */
    public static void createEmptyTemplateIfMissing(String resourcePath) {
        try {
            Path path = Paths.get(resourcePath);
            if (Files.exists(path)) {
                System.out.println("[EXPERIENCE MASTER] Excel already exists: " + resourcePath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(EXPERIENCE_MASTER_SHEET);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_MIN_YEARS);
                headerRow.createCell(1).setCellValue(COL_MAX_YEARS);
                for (int i = 0; i < DEFAULT_RANGES.length; i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(DEFAULT_RANGES[i][0]);
                    row.createCell(1).setCellValue(DEFAULT_RANGES[i][1]);
                }
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[EXPERIENCE MASTER] Template created with default ranges: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[EXPERIENCE MASTER] Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Reads all experience ranges from Excel (MinYears, MaxYears columns, skip header).
     * Rows with empty min or max are skipped (no values passed for that row).
     * Returns list of {min, max} pairs.
     */
    public static List<int[]> readExperienceRanges(InputStream in) {
        List<int[]> ranges = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(EXPERIENCE_MASTER_SHEET);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Integer min = getCellIntOrNull(row.getCell(0));
                Integer max = getCellIntOrNull(row.getCell(1));
                if (min == null || max == null) continue; // skip empty cells – no values passed
                if (min < 0 || max < 0) continue;          // skip invalid
                ranges.add(new int[] { min, max });
            }
        } catch (Exception e) {
            System.err.println("[EXPERIENCE MASTER] Failed to read: " + e.getMessage());
        }
        return ranges;
    }

    /** Returns null if cell is empty/blank so the row can be skipped. */
    private static Integer getCellIntOrNull(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case BLANK:
                return null;
            case STRING:
                String s = cell.getStringCellValue().trim();
                if (s.isEmpty()) return null;
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            default:
                return null;
        }
    }

    /**
     * Picks up to {@code count} random experience ranges from the list.
     */
    public static List<int[]> getRandomExperienceRanges(List<int[]> allRanges, int count) {
        if (allRanges == null || allRanges.isEmpty()) return Collections.emptyList();
        List<int[]> shuffled = new ArrayList<>(allRanges);
        Collections.shuffle(shuffled);
        int take = Math.min(count, shuffled.size());
        List<int[]> result = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            result.add(shuffled.get(i));
        }
        return result;
    }

    /** Creates ExperienceMaster.xlsx in src/test/resources if missing. Run from project root: mvn compile exec:java -Dexec.mainClass="Utils.ExperienceMasterExcelUtil" */
    public static void main(String[] args) {
        String projectRoot = System.getProperty("user.dir");
        String path = Paths.get(projectRoot, "src", "test", "resources", "ExperienceMaster.xlsx").toString();
        createEmptyTemplateIfMissing(path);
        System.out.println("Done. File: " + path);
    }
}
