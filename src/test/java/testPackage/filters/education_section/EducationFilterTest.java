package testPackage.filters.education_section;

import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.ScrollHelper;
import Utils.SSHTunnelManager;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.apache.poi.ss.usermodel.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import testPackage.helpers.LoginHelper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Feature("Advanced Filters - Educational Details")
public class EducationFilterTest extends basePage {

    private static final int SAMPLES_PER_PAGE = 4; // open up to 4 candidates per page
    private static final int ROWS_PER_PAGE = 25;   // assumed rows per page in results

    private void clickAndTypeIntoInput(By locator, String value, String label) {
        if (value == null || value.isEmpty()) return;
        try {
            org.openqa.selenium.support.ui.WebDriverWait wait =
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10));

            WebElement input = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(locator));

            ScrollHelper.scrollIntoView(driver, input);
            input.click();
            Thread.sleep(150);

            // Type the keyword, then press ENTER to confirm it
            input.sendKeys(value);
            Thread.sleep(150);
            input.sendKeys(org.openqa.selenium.Keys.ENTER);
            Thread.sleep(200);

            System.out.println("[EDUCATION] " + label + " typed and ENTER pressed.");
        } catch (Exception e) {
            System.out.println("[EDUCATION] " + label + " input interaction failed: " + e.getMessage());
        }
    }

    private static class EducationRow {
        final String ugMode;
        final String ugDegree;
        final String ugInstitution;
        final String ugSpecialization;
        final String ugYear;
        final String pgMode;
        final String pgDegree;
        final String pgInstitution;
        final String pgSpecialization;
        final String pgYear;

        EducationRow(String ugMode, String ugDegree, String ugInstitution, String ugSpecialization, String ugYear,
                     String pgMode, String pgDegree, String pgInstitution, String pgSpecialization, String pgYear) {
            this.ugMode = ugMode;
            this.ugDegree = ugDegree;
            this.ugInstitution = ugInstitution;
            this.ugSpecialization = ugSpecialization;
            this.ugYear = ugYear;
            this.pgMode = pgMode;
            this.pgDegree = pgDegree;
            this.pgInstitution = pgInstitution;
            this.pgSpecialization = pgSpecialization;
            this.pgYear = pgYear;
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (EducationFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (EducationFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    private List<EducationRow> readEducationRows(String excelPath) throws Exception {
        List<EducationRow> rows = new ArrayList<>();
        if (!Files.exists(Paths.get(excelPath))) return rows;

        try (InputStream in = Files.newInputStream(Paths.get(excelPath));
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet("EducationFilters");
            if (sheet == null) sheet = wb.getSheetAt(0);

            Row header = sheet.getRow(0);
            if (header == null) return rows;

            // Map headers to column indices
            int ugModeCol = -1, ugDegreeCol = -1, ugInstitutionCol = -1, ugSpecCol = -1, ugYearCol = -1;
            int pgModeCol = -1, pgDegreeCol = -1, pgInstitutionCol = -1, pgSpecCol = -1, pgYearCol = -1;

            for (int c = 0; c <= header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String h = cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                if (h.equalsIgnoreCase("UG_Mode")) ugModeCol = c;
                else if (h.equalsIgnoreCase("UG_Degree")) ugDegreeCol = c;
                else if (h.equalsIgnoreCase("UG_Institution")) ugInstitutionCol = c;
                else if (h.equalsIgnoreCase("UG_Specialization")) ugSpecCol = c;
                else if (h.equalsIgnoreCase("UG_Year")) ugYearCol = c;
                else if (h.equalsIgnoreCase("PG_Mode")) pgModeCol = c;
                else if (h.equalsIgnoreCase("PG_Degree")) pgDegreeCol = c;
                else if (h.equalsIgnoreCase("PG_Institution")) pgInstitutionCol = c;
                else if (h.equalsIgnoreCase("PG_Specialization")) pgSpecCol = c;
                else if (h.equalsIgnoreCase("PG_Year")) pgYearCol = c;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String ugMode = getCellString(row.getCell(ugModeCol));
                String ugDegree = getCellString(row.getCell(ugDegreeCol));
                String ugInstitution = getCellString(row.getCell(ugInstitutionCol));
                String ugSpec = getCellString(row.getCell(ugSpecCol));
                String ugYear = getCellString(row.getCell(ugYearCol));
                String pgMode = getCellString(row.getCell(pgModeCol));
                String pgDegree = getCellString(row.getCell(pgDegreeCol));
                String pgInstitution = getCellString(row.getCell(pgInstitutionCol));
                String pgSpec = getCellString(row.getCell(pgSpecCol));
                String pgYear = getCellString(row.getCell(pgYearCol));

                // Skip completely empty rows
                if (ugMode.isEmpty() && ugDegree.isEmpty() && ugInstitution.isEmpty() &&
                    ugSpec.isEmpty() && ugYear.isEmpty() &&
                    pgMode.isEmpty() && pgDegree.isEmpty() && pgInstitution.isEmpty() &&
                    pgSpec.isEmpty() && pgYear.isEmpty()) {
                    continue;
                }

                rows.add(new EducationRow(
                    ugMode, ugDegree, ugInstitution, ugSpec, ugYear,
                    pgMode, pgDegree, pgInstitution, pgSpec, pgYear
                ));
            }
        }
        return rows;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    @Test
    @Description("Education – read UG/PG filters from Excel, open Educational Details filter UI, and (next) apply filters per row")
    public void educationFilter_FromExcel_Skeleton() throws Exception {
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_education",
            "education_filter.xlsx"
        ).toString();

        List<EducationRow> eduRows = readEducationRows(excelPath);
        if (eduRows.isEmpty()) {
            System.out.println("[EDUCATION] No rows in education_filter.xlsx. Fill EducationFilters sheet first.");
            return;
        }
        System.out.println("[EDUCATION] Loaded " + eduRows.size() + " rows from education_filter.xlsx.");

        // Take a small subset (up to 3 rows) to drive filters this run
        List<EducationRow> rowsToRun = eduRows.size() <= 3 ? eduRows : eduRows.subList(0, 3);

        for (int i = 0; i < rowsToRun.size(); i++) {
            EducationRow row = rowsToRun.get(i);
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[EDUCATION] Starting filter run " + (i + 1) + " with UG/PG from Excel:");
            System.out.println("  UG_Mode=" + row.ugMode +
                ", UG_Degree=" + row.ugDegree +
                ", UG_Institution=" + row.ugInstitution +
                ", UG_Spec=" + row.ugSpecialization +
                ", UG_Year=" + row.ugYear);
            System.out.println("  PG_Mode=" + row.pgMode +
                ", PG_Degree=" + row.pgDegree +
                ", PG_Institution=" + row.pgInstitution +
                ", PG_Spec=" + row.pgSpecialization +
                ", PG_Year=" + row.pgYear);
            System.out.println("=".repeat(80));

            // Ensure clean state for this row
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            // Open Educational Details tab
            try {
                By eduDetailsTab = By.xpath("//span[text() = 'Educational Details']");
                WebElement tab = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(eduDetailsTab));
                ScrollHelper.scrollIntoViewAtTop(driver, tab);
                tab.click();
                Thread.sleep(600);
            } catch (Exception e) {
                System.out.println("[EDUCATION] Educational Details tab not found/clickable: " + e.getMessage());
                return;
            }

            // Open the Education filter panel
            try {
                By eduFilterButton = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div[1]/div[3]/div[2]/div[2]/div[2]/div[4]/div/div/div/button");
                WebElement btn = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(eduFilterButton));
                ScrollHelper.scrollIntoView(driver, btn);
                btn.click();
                Thread.sleep(600);
                System.out.println("[EDUCATION] Education filter panel opened successfully.");
            } catch (Exception e) {
                System.out.println("[EDUCATION] Education filter button not found/clickable: " + e.getMessage());
                continue;
            }

            // Decide UG/PG modes once, then choose locators/fields based on that (your locators are unchanged)
            String ugMode = row.ugMode == null ? "" : row.ugMode.trim();
            String pgMode = row.pgMode == null ? "" : row.pgMode.trim();

            boolean ugIsSpecific = "Specific".equalsIgnoreCase(ugMode);
            boolean ugIsNone     = "None".equalsIgnoreCase(ugMode);
            boolean ugIsAny      = !ugIsSpecific && !ugIsNone; // default Any

            boolean pgIsSpecific = "Specific".equalsIgnoreCase(pgMode);
            boolean pgIsNone     = "None".equalsIgnoreCase(pgMode);
            boolean pgIsAny      = !pgIsSpecific && !pgIsNone; // default Any

            System.out.println("[EDUCATION] Resolved UG_Mode=" + ugMode + " (Specific=" + ugIsSpecific + ", Any=" + ugIsAny + ", None=" + ugIsNone + ")");
            System.out.println("[EDUCATION] Resolved PG_Mode=" + pgMode + " (Specific=" + pgIsSpecific + ", Any=" + pgIsAny + ", None=" + pgIsNone + ")");

            // 1) Click UG mode button according to resolved mode
            try {
                By ugModeBtnLocator;
                if (ugIsSpecific) {
                    ugModeBtnLocator = By.xpath("(//button[text() = 'Specific'])[1]");
                } else if (ugIsNone) {
                    ugModeBtnLocator = By.xpath("(//button[text() = 'None'])[1]");
                } else {
                    ugModeBtnLocator = By.xpath("(//button[text() = 'Any'])[1]");
                }
                WebElement ugModeBtn = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(ugModeBtnLocator));
                ScrollHelper.scrollIntoView(driver, ugModeBtn);
                ugModeBtn.click();
                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println("[EDUCATION] UG mode button click failed: " + e.getMessage());
            }

            // 2) Click PG mode button according to resolved mode
            try {
                By pgModeBtnLocator;
                if (pgIsSpecific) {
                    pgModeBtnLocator = By.xpath("(//button[text() = 'Specific'])[2]");
                } else if (pgIsNone) {
                    pgModeBtnLocator = By.xpath("(//button[text() = 'None'])[2]");
                } else {
                    pgModeBtnLocator = By.xpath("(//button[text() = 'Any'])[2]");
                }
                WebElement pgModeBtn = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(pgModeBtnLocator));
                ScrollHelper.scrollIntoView(driver, pgModeBtn);
                pgModeBtn.click();
                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println("[EDUCATION] PG mode button click failed: " + e.getMessage());
            }

            // 3) Decide UG locators for the chosen mode (locators themselves are exactly what you provided)
            By ugDegreeLocator = null;
            By ugInstLocator   = null;
            By ugSpecLocator   = null;
            By ugYearBtn       = null;

            if (ugIsSpecific) {
                // Your "Specific" mappings
                ugDegreeLocator = By.xpath("(//input[@placeholder='Type degree name...'])[1]");
                ugInstLocator   = By.xpath("(//input[@placeholder='Type institute name...'])[1]");
                ugSpecLocator   = By.xpath("(//input[@placeholder='Type specialization...'])[1]");
                ugYearBtn       = By.xpath("(//button[text() = 'Select Year'])[1]");
            } else if (ugIsAny) {
                // Your "Any" mappings (only institute is used)
                ugInstLocator   = By.xpath("(//input[@placeholder='Type institute name...'])[1]");
            } else {
                // None => no UG text fields used
            }

            // 4) Decide PG locators for the chosen mode (locators themselves are exactly what you provided)
            By pgDegreeLocator = null;
            By pgInstLocator   = null;
            By pgSpecLocator   = null;
            By pgYearBtn       = null;

            if (pgIsSpecific) {
                pgDegreeLocator = By.xpath("(//input[@placeholder='Type degree name...'])[2]");
                pgInstLocator   = By.xpath("(//input[@placeholder='Type institute name...'])[2]");
                pgSpecLocator   = By.xpath("(//input[@placeholder='Type specialization...'])[2]");
                pgYearBtn       = By.xpath("(//button[text() = 'Select Year'])[2]");
            } else if (pgIsAny) {
                pgInstLocator   = By.xpath("(//input[@placeholder='Type institute name...'])[2]");
            } else {
                // None => no PG text fields used
            }

            // 5) Now actually type values based on resolved locators + Excel values (using helper that clicks before typing)
            if (ugDegreeLocator != null) {
                clickAndTypeIntoInput(ugDegreeLocator, row.ugDegree, "UG Degree");
            }
            if (ugInstLocator != null) {
                String label = ugIsSpecific ? "UG Institution" : "UG Institution (Any mode)";
                clickAndTypeIntoInput(ugInstLocator, row.ugInstitution, label);
            }
            if (ugSpecLocator != null) {
                clickAndTypeIntoInput(ugSpecLocator, row.ugSpecialization, "UG Specialization");
            }
            if (ugYearBtn != null && row.ugYear != null && !row.ugYear.trim().isEmpty()) {
                try {
                    WebElement trigger = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(ugYearBtn));
                    ScrollHelper.scrollIntoView(driver, trigger);
                    trigger.click();
                    Thread.sleep(300);

                    // Year options are buttons like //button[text()='2031'], year comes from Excel (row.ugYear)
                    By yearOption = By.xpath("//button[text() = '" + row.ugYear + "']");
                    WebElement opt = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(yearOption));
                    ScrollHelper.scrollIntoView(driver, opt);
                    opt.click();
                    Thread.sleep(300);

                    // After selecting year, click Close button in year selector
                    try {
                        By closeBtn = By.xpath("//button[text() = 'Close']");
                        WebElement close = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(closeBtn));
                        ScrollHelper.scrollIntoView(driver, close);
                        close.click();
                        Thread.sleep(300);
                    } catch (Exception closeEx) {
                        System.out.println("[EDUCATION] UG Year 'Close' button not found/clickable: " + closeEx.getMessage());
                    }
                } catch (Exception e) {
                    System.out.println("[EDUCATION] UG Year selection failed: " + e.getMessage());
                }
            }

            if (pgDegreeLocator != null) {
                clickAndTypeIntoInput(pgDegreeLocator, row.pgDegree, "PG Degree");
            }
            if (pgInstLocator != null) {
                String label = pgIsSpecific ? "PG Institution" : "PG Institution (Any mode)";
                clickAndTypeIntoInput(pgInstLocator, row.pgInstitution, label);
            }
            if (pgSpecLocator != null) {
                clickAndTypeIntoInput(pgSpecLocator, row.pgSpecialization, "PG Specialization");
            }
            if (pgYearBtn != null && row.pgYear != null && !row.pgYear.trim().isEmpty()) {
                try {
                    WebElement trigger = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(pgYearBtn));
                    ScrollHelper.scrollIntoView(driver, trigger);
                    trigger.click();
                    Thread.sleep(300);

                    // Year options are buttons like //button[text()='2031'], year comes from Excel (row.pgYear)
                    By yearOption = By.xpath("//button[text() = '" + row.pgYear + "']");
                    WebElement opt = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(yearOption));
                    ScrollHelper.scrollIntoView(driver, opt);
                    opt.click();
                    Thread.sleep(300);

                    // After selecting year, click Close button in year selector
                    try {
                        By closeBtn = By.xpath("//button[text() = 'Close']");
                        WebElement close = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(closeBtn));
                        ScrollHelper.scrollIntoView(driver, close);
                        close.click();
                        Thread.sleep(300);
                    } catch (Exception closeEx) {
                        System.out.println("[EDUCATION] PG Year 'Close' button not found/clickable: " + closeEx.getMessage());
                    }
                } catch (Exception e) {
                    System.out.println("[EDUCATION] PG Year selection failed: " + e.getMessage());
                }
            }

            // Click Apply Filter for this education configuration
            try {
                // Use the exact Apply Filter button: //button[text() ='Apply Filter']
                By applyBtn = By.xpath("//button[text() = 'Apply Filter']");
                WebElement apply = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(applyBtn));
                ScrollHelper.scrollIntoView(driver, apply);
                apply.click();
                candidatesPage.waitForLoaderGone();
                Thread.sleep(1500);
                System.out.println("[EDUCATION] Applied Education filter for row " + (i + 1));
            } catch (Exception e) {
                System.out.println("[EDUCATION] Apply Filter button not found/clickable for row " + (i + 1) + ": " + e.getMessage());
                continue;
            }

        // After applying filter, sample candidates across pages and log Education details
            try {
                int totalCount = candidatesPage.getResultCount();
                if (totalCount <= 0) {
                    System.out.println("[EDUCATION] No results for Education row " + (i + 1) + ". Skipping sampling.");
                    continue;
                }
                System.out.println("[EDUCATION] Total candidates after Education filter for row " + (i + 1) + ": " + totalCount);

                int totalPages = (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;

                class CandidateEducationSummary {
                    final String candidateName;
                    final String url;
                    final String allEducations;
                    final String searchedFilter;
                    final String matchedFilter;
                    final boolean matched;

                    CandidateEducationSummary(String candidateName, String url,
                                              String allEducations, String searchedFilter,
                                              String matchedFilter, boolean matched) {
                        this.candidateName = candidateName;
                        this.url = url;
                        this.allEducations = allEducations;
                        this.searchedFilter = searchedFilter;
                        this.matchedFilter = matchedFilter;
                        this.matched = matched;
                    }
                }

                List<CandidateEducationSummary> summaries = new ArrayList<>();
                String parentHandle = driver.getWindowHandle();

                // Build a human-readable "searched filter" description from the Excel row
                StringBuilder searched = new StringBuilder();
                if (!row.ugMode.isEmpty()) {
                    searched.append("UG[").append(row.ugMode).append(": ");
                    if (!row.ugDegree.isEmpty()) searched.append("Degree=").append(row.ugDegree).append(", ");
                    if (!row.ugInstitution.isEmpty()) searched.append("Inst=").append(row.ugInstitution).append(", ");
                    if (!row.ugSpecialization.isEmpty()) searched.append("Spec=").append(row.ugSpecialization).append(", ");
                    if (!row.ugYear.isEmpty()) searched.append("Year=").append(row.ugYear);
                    searched.append("] ");
                }
                if (!row.pgMode.isEmpty()) {
                    searched.append("PG[").append(row.pgMode).append(": ");
                    if (!row.pgDegree.isEmpty()) searched.append("Degree=").append(row.pgDegree).append(", ");
                    if (!row.pgInstitution.isEmpty()) searched.append("Inst=").append(row.pgInstitution).append(", ");
                    if (!row.pgSpecialization.isEmpty()) searched.append("Spec=").append(row.pgSpecialization).append(", ");
                    if (!row.pgYear.isEmpty()) searched.append("Year=").append(row.pgYear);
                    searched.append("]");
                }
                String searchedFilterDesc = searched.toString().trim();

                for (int page = 1; page <= totalPages; page++) {
                    if (page > 1) {
                        candidatesPage.scrollToBottom();
                        candidatesPage.selectPage(page);
                        candidatesPage.waitForLoaderGone();
                        Thread.sleep(1000);
                    }

                    List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
                    if (links.isEmpty()) continue;
                    List<Integer> indices = new ArrayList<>();
                    for (int idx = 0; idx < links.size(); idx++) indices.add(idx);
                    java.util.Collections.shuffle(indices);
                    int toOpen = Math.min(SAMPLES_PER_PAGE, indices.size());

                    for (int k = 0; k < toOpen; k++) {
                        int idxOnPage = indices.get(k);
                        WebElement link = links.get(idxOnPage);
                        try {
                            String nameOnList = link.getText();
                            candidatesPage.openCandidateProfile(link);
                            Thread.sleep(1500);
                            for (String h : driver.getWindowHandles()) {
                                if (!h.equals(parentHandle)) {
                                    driver.switchTo().window(h);
                                    break;
                                }
                            }
                            String detailsUrl = driver.getCurrentUrl();

                            // Scroll to Education section on details page
                            List<String> eduLines = new ArrayList<>();
                            String matchedEdu = "-";
                            boolean matched = false;
                            try {
                                By eduSectionBy = By.xpath("//h2[normalize-space(text())='Education']/parent::div");
                                WebElement section = driver.findElement(eduSectionBy);
                                ScrollHelper.scrollIntoView(driver, section);
                                Thread.sleep(500);

                                List<WebElement> cards = section.findElements(
                                    By.xpath(".//div[contains(@class,'flex items-start') and .//svg[contains(@class,'lucide-graduation-cap')]]")
                                );
                                for (WebElement card : cards) {
                                    try {
                                        WebElement h3 = card.findElement(By.xpath(".//h3"));
                                        String title = h3.getText() == null ? "" : h3.getText().trim();
                                        WebElement instP = card.findElement(By.xpath(".//p[contains(@class,'text-sm') and contains(@class,'text-gray-600')]"));
                                        String inst = instP.getText() == null ? "" : instP.getText().trim();
                                        String combined = title + " | " + inst;
                                        eduLines.add(combined);

                                        // Basic match logic: check if institution/year from Excel appear in this entry
                                        String lc = combined.toLowerCase();
                                        boolean thisMatch = false;
                                        if (!row.ugInstitution.isEmpty() && lc.contains(row.ugInstitution.toLowerCase())) thisMatch = true;
                                        if (!row.pgInstitution.isEmpty() && lc.contains(row.pgInstitution.toLowerCase())) thisMatch = true;
                                        if (!row.ugYear.isEmpty() && lc.contains(row.ugYear.toLowerCase())) thisMatch = true;
                                        if (!row.pgYear.isEmpty() && lc.contains(row.pgYear.toLowerCase())) thisMatch = true;
                                        if (thisMatch && !matched) {
                                            matched = true;
                                            matchedEdu = combined;
                                        }
                                    } catch (Exception cardEx) {
                                        System.out.println("[EDUCATION] Failed to parse an Education card: " + cardEx.getMessage());
                                    }
                                }
                            } catch (Exception secEx) {
                                System.out.println("[EDUCATION] Education section not found or not parsable: " + secEx.getMessage());
                            }

                            String allEdu = eduLines.isEmpty() ? "-" : String.join(" || ", eduLines);
                            summaries.add(new CandidateEducationSummary(
                                nameOnList == null ? "" : nameOnList.trim(),
                                detailsUrl,
                                allEdu,
                                searchedFilterDesc,
                                matchedEdu,
                                matched
                            ));

                            driver.close();
                            driver.switchTo().window(parentHandle);
                            Thread.sleep(500);
                        } catch (Exception e) {
                            try {
                                driver.switchTo().window(parentHandle);
                            } catch (Exception ignored) {
                            }
                            System.out.println("[EDUCATION] Error while sampling candidate on page " + page + ": " + e.getMessage());
                        }
                    }
                }

                // Print summary table for this Education row
                System.out.println("\n[EDUCATION] Summary for Excel row " + (i + 1) + ":");
                System.out.println("Searched filter: " + searchedFilterDesc);
                System.out.printf("%-30s | %-70s | %-80s | %-30s | %-10s%n",
                    "Candidate Name", "Candidate URL", "All Educations (details page)", "Matched Filter", "Matched?");
                System.out.println("-".repeat(240));
                for (CandidateEducationSummary s : summaries) {
                    System.out.printf("%-30s | %-70s | %-80s | %-30s | %-10s%n",
                        s.candidateName,
                        s.url,
                        s.allEducations,
                        s.matchedFilter,
                        s.matched ? "MATCH" : "UNMATCHED");
                }

            } catch (Exception e) {
                System.out.println("[EDUCATION] Sampling/logging after Education filter failed for row " + (i + 1) + ": " + e.getMessage());
            }
            // After finishing this Education row, clear filters and reset UI for the next row
            try {
                candidatesPage.clickClearFiltersIfPresent();
                Thread.sleep(500);
                candidatesPage.openAdvanceSearch();
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("[EDUCATION] Failed to clear filters / reopen Advance Search after row " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
}

 