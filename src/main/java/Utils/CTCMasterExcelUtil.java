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
 * Manages the CTC master Excel: reads min/max lakh ranges for Current CTC and Expected CTC filters.
 * Template path: src/test/resources/CTCMaster.xlsx
 * Columns: MinLakhs, MaxLakhs
 */
public class CTCMasterExcelUtil {

    public static final String CTC_MASTER_SHEET = "CTCRanges";
    public static final String COL_MIN_LAKHS = "MinLakhs";
    public static final String COL_MAX_LAKHS = "MaxLakhs";

    /** Default ranges to write when creating template (in lakhs LPA). */
    private static final int[][] DEFAULT_RANGES = { {0, 5}, {5, 10}, {10, 20}, {20, 40}, {40, 80} };

    /**
     * Ensure the master Excel exists; if it doesn't, create a template with header and default ranges.
     */
    public static void createEmptyTemplateIfMissing(String resourcePath) {
        try {
            Path path = Paths.get(resourcePath);
            if (Files.exists(path)) {
                System.out.println("[CTC MASTER] Excel already exists: " + resourcePath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(CTC_MASTER_SHEET);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_MIN_LAKHS);
                headerRow.createCell(1).setCellValue(COL_MAX_LAKHS);
                for (int i = 0; i < DEFAULT_RANGES.length; i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(DEFAULT_RANGES[i][0]);
                    row.createCell(1).setCellValue(DEFAULT_RANGES[i][1]);
                }
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[CTC MASTER] Template created with default ranges: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[CTC MASTER] Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Reads all CTC ranges from Excel (MinLakhs, MaxLakhs columns, skip header).
     * Returns list of {min, max} pairs in lakhs.
     */
    public static List<int[]> readCTCRanges(InputStream in) {
        List<int[]> ranges = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(CTC_MASTER_SHEET);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Integer min = getCellIntOrNull(row.getCell(0));
                Integer max = getCellIntOrNull(row.getCell(1));
                if (min == null || max == null) continue;
                if (min < 0 || max < 0) continue;
                ranges.add(new int[] { min, max });
            }
        } catch (Exception e) {
            System.err.println("[CTC MASTER] Failed to read: " + e.getMessage());
        }
        return ranges;
    }

    private static Integer getCellIntOrNull(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case BLANK: return null;
            case STRING:
                String s = cell.getStringCellValue().trim();
                if (s.isEmpty()) return null;
                try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
            case NUMERIC: return (int) cell.getNumericCellValue();
            default: return null;
        }
    }

    /** Picks up to {@code count} random CTC ranges from the list. */
    public static List<int[]> getRandomCTCRanges(List<int[]> allRanges, int count) {
        if (allRanges == null || allRanges.isEmpty()) return Collections.emptyList();
        List<int[]> shuffled = new ArrayList<>(allRanges);
        Collections.shuffle(shuffled);
        int take = Math.min(count, shuffled.size());
        List<int[]> result = new ArrayList<>(take);
        for (int i = 0; i < take; i++) result.add(shuffled.get(i));
        return result;
    }
}
