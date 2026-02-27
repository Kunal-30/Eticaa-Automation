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
 * Manages the company master Excel: creates it from company_names.txt if missing,
 * reads all company names, and builds random Company filter rows (Past / Current / Current + Past).
 */
public class CompanyMasterExcelUtil {

    public static final String COMPANY_MASTER_SHEET = "Companies";
    public static final String COL_COMPANY_NAME = "CompanyName";
    private static final String[] TYPES = { "Past", "Current", "Current + Past" };
    private static final Random RANDOM = new Random();

    /**
     * Creates CompanyMaster.xlsx from company_names.txt if the Excel does not exist.
     */
    public static void createCompanyMasterFromTextIfMissing(String excelPath, InputStream namesTxtStream) {
        try {
            Path path = Paths.get(excelPath);
            if (Files.exists(path)) {
                System.out.println("[COMPANY MASTER] Excel already exists: " + excelPath);
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
                Sheet sheet = wb.createSheet(COMPANY_MASTER_SHEET);
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue(COL_COMPANY_NAME);
                for (int i = 0; i < names.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(names.get(i));
                }
                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("[COMPANY MASTER] Created: " + excelPath + " with " + names.size() + " companies.");
            }
        } catch (Exception e) {
            System.err.println("[COMPANY MASTER] Failed to create: " + e.getMessage());
        }
    }

    /**
     * Reads all company names from the CompanyMaster Excel (first column, skip header).
     */
    public static List<String> readCompanyNames(InputStream excelStream) {
        List<String> names = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(excelStream)) {
            Sheet sheet = wb.getSheet(COMPANY_MASTER_SHEET);
            if (sheet == null) sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getCellString(row.getCell(0));
                if (!name.isEmpty()) names.add(name);
            }
        } catch (Exception e) {
            System.err.println("[COMPANY MASTER] Failed to read: " + e.getMessage());
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
     * Picks {@code count} random companies and assigns a random Type (Past / Current / Current + Past) to each.
     * Returns filter rows with FilterType=Company, IncludeExclude=Include.
     */
    public static List<FilterRow> getRandomCompanyFilterRows(List<String> allCompanies, int count) {
        if (allCompanies == null || allCompanies.isEmpty()) return Collections.emptyList();
        List<String> shuffled = new ArrayList<>(allCompanies);
        Collections.shuffle(shuffled);
        int take = Math.min(count, shuffled.size());
        List<FilterRow> rows = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            String company = shuffled.get(i);
            String type = TYPES[RANDOM.nextInt(TYPES.length)];
            rows.add(new FilterRow("Company", company, type, "Include", ""));
        }
        return rows;
    }
}
