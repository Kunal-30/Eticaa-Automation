package Utils.contact_details_Util;

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
 * Contact Details - Mobile filter Excel util.
 *
 * Template path: src/test/resources/filters_contact_details/mobile_filter.xlsx
 * Sheet name   : MobileFilters
 * Column       : Mobile
 */
public class mobile_filter_Util {

    public static final String SHEET_NAME = "MobileFilters";
    public static final String COL_MOBILE = "Mobile";

    public static void createTemplateIfMissing(String excelPath) {
        try {
            Path path = Paths.get(excelPath);
            if (Files.exists(path)) {
                System.out.println("[MOBILE FILTER] Excel already exists: " + excelPath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(SHEET_NAME);
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue(COL_MOBILE);

                String[] samples = {
                    "9876543210",
                    "9123456780",
                    "9090909090",
                    "9998887776"
                };
                for (int i = 0; i < samples.length; i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(samples[i]);
                }

                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[MOBILE FILTER] Template created: " + excelPath);
            }
        } catch (Exception e) {
            System.err.println("[MOBILE FILTER] Failed to create template: " + e.getMessage());
        }
    }

    public static List<String> readMobiles(InputStream in) {
        List<String> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String val = getCellString(row.getCell(0));
                if (val != null && !val.trim().isEmpty()) {
                    result.add(val.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[MOBILE FILTER] Failed to read: " + e.getMessage());
        }
        return result;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return "";
        }
    }

    public static List<String> getRandomMobiles(List<String> all, int count) {
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<String> copy = new ArrayList<>(all);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}

