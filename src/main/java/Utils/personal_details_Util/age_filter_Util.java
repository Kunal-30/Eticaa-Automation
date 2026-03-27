package Utils.personal_details_Util;

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
 * Age filter Excel util.
 *
 * Template path: src/test/resources/filters_personal_details/age_filter.xlsx
 * Sheet name   : AgeFilters
 * Columns      : MinAge, MaxAge
 */
public class age_filter_Util {

    public static final String SHEET_NAME = "AgeFilters";
    public static final String COL_MIN_AGE = "MinAge";
    public static final String COL_MAX_AGE = "MaxAge";

    public static void createTemplateIfMissing(String excelPath) {
        try {
            Path path = Paths.get(excelPath);
            if (Files.exists(path)) {
                System.out.println("[AGE FILTER] Excel already exists: " + excelPath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(SHEET_NAME);
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue(COL_MIN_AGE);
                header.createCell(1).setCellValue(COL_MAX_AGE);

                int[][] samples = { {25, 35}, {30, 40}, {35, 45} };
                for (int i = 0; i < samples.length; i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(samples[i][0]);
                    row.createCell(1).setCellValue(samples[i][1]);
                }

                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[AGE FILTER] Template created: " + excelPath);
            }
        } catch (Exception e) {
            System.err.println("[AGE FILTER] Failed to create template: " + e.getMessage());
        }
    }

    public static List<int[]> readAgeRanges(InputStream in) {
        List<int[]> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Integer min = getCellInt(row.getCell(0));
                Integer max = getCellInt(row.getCell(1));
                if (min == null || max == null) continue;
                result.add(new int[]{min, max});
            }
        } catch (Exception e) {
            System.err.println("[AGE FILTER] Failed to read: " + e.getMessage());
        }
        return result;
    }

    private static Integer getCellInt(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue().trim();
                if (s.isEmpty()) return null;
                try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            default:
                return null;
        }
    }

    public static List<int[]> getRandomRanges(List<int[]> all, int count) {
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<int[]> copy = new ArrayList<>(all);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}