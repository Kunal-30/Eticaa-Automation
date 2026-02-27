package Utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages the Location master Excel: creates it from location_names.txt if missing,
 * reads all location names, and builds random Location filter rows with different modes.
 */
public class LocationMasterExcelUtil {

    public static final String LOCATION_MASTER_SHEET = "Locations";
    public static final String COL_LOCATION_NAME = "LocationName";
    private static final String[] MODES = {
        "Either",
        "Current Location",
        "Preferred Location",
        "Both"
    };
    private static final Random RANDOM = new Random();

    /** Creates LocationMaster.xlsx from location_names.txt if the Excel does not exist. */
    public static void createLocationMasterFromTextIfMissing(String excelPath, InputStream namesTxtStream) {
        try {
            Path path = Paths.get(excelPath);
            if (Files.exists(path)) {
                System.out.println("[LOCATION MASTER] Excel already exists: " + excelPath);
                return;
            }
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            List<String> names = new ArrayList<>();
            if (namesTxtStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(namesTxtStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) names.add(trimmed);
                    }
                }
            }
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet(LOCATION_MASTER_SHEET);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_LOCATION_NAME);
                for (int i = 0; i < names.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(names.get(i));
                }
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[LOCATION MASTER] Created: " + excelPath + " with " + names.size() + " locations.");
            }
        } catch (Exception e) {
            System.err.println("[LOCATION MASTER] Failed to create: " + e.getMessage());
        }
    }

    /** Reads all locations from the LocationMaster Excel (first column, skip header). */
    public static List<String> readLocationNames(InputStream excelStream) {
        List<String> names = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(excelStream)) {
            Sheet sheet = wb.getSheet(LOCATION_MASTER_SHEET);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getCellString(row.getCell(0));
                if (!name.isEmpty()) names.add(name);
            }
        } catch (Exception e) {
            System.err.println("[LOCATION MASTER] Failed to read: " + e.getMessage());
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
     * Picks {@code count} random locations and assigns a random Mode to each.
     * Modes: Either / Current Location / Preferred Location / Both.
     * For Both we will use the same city for current and preferred (UI gets two inputs; value is same).
     * Returns filter rows with FilterType=Location, IncludeExclude=Include.
     */
    public static List<FilterRow> getRandomLocationFilterRows(List<String> allLocations, int count) {
        if (allLocations == null || allLocations.isEmpty()) return Collections.emptyList();
        List<String> shuffled = new ArrayList<>(allLocations);
        Collections.shuffle(shuffled);
        int take = Math.min(count, shuffled.size());
        List<FilterRow> rows = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            String city = shuffled.get(i);
            String mode = MODES[RANDOM.nextInt(MODES.length)];
            rows.add(new FilterRow("Location", city, mode, "Include", ""));
        }
        return rows;
    }
}

