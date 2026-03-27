package testPackage.filters.mixed_advance_filter;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.ScrollHelper;
import Utils.SSHTunnelManager;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.apache.poi.ss.usermodel.*;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;
import testPackage.helpers.LoginHelper;
import Pages.loginPage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Feature("Advanced Filters - Mixed")
public class MixedAdvanceFilterTest extends basePage {

    private static final int SAMPLES_PER_PAGE = 4;
    private static final int ROWS_PER_PAGE = 25;
    private static final int MIXED_RUNS = 3;

    private enum FilterType {
        COMPANY,
        LOCATION,
        DESIGNATION,
        EXPERIENCE_RANGE,
        CURRENT_CTC,
        EXPECTED_CTC,
        GENDER,
        MARITAL_STATUS,
        AGE,
        PHYSICALLY_CHALLENGED,
        CATEGORY,
        DEPARTMENT,
        ROLE,
        INDUSTRY,
        EMPLOYMENT_STATUS,
        LANGUAGE,
        SOCIALS
    }

    // Simpler subset for load test iterations
    private enum SimpleFilterType {
        COMPANY,
        LOCATION,
        DEPARTMENT,
        ROLE,
        INDUSTRY
    }

    private static class MixedCandidateRow {
        final int page;
        final int indexOnPage;
        final String candidateName;
        final String filtersApplied;
        final String candidateValues;
        final boolean allMatched;
        final String url;

        MixedCandidateRow(int page, int indexOnPage, String candidateName,
                          String filtersApplied, String candidateValues,
                          boolean allMatched, String url) {
            this.page = page;
            this.indexOnPage = indexOnPage;
            this.candidateName = candidateName;
            this.filtersApplied = filtersApplied;
            this.candidateValues = candidateValues;
            this.allMatched = allMatched;
            this.url = url;
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (MixedAdvanceFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (MixedAdvanceFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    // Flags so that each section tab is clicked at most once per mixed run
    private boolean personalOpened;
    private boolean professionalOpened;
    private boolean socialsOpened;
    private boolean educationOpened;
    private boolean languageOpened;

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    // === MULTI-USER LOAD SUPPORT ===

    @DataProvider(name = "userEmails", parallel = true)
    public Object[][] userEmailsProvider() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "Users",
            "users_id.xlsx"
        ).toString();

        List<String> emails = new ArrayList<>();
        if (!Files.exists(Paths.get(excelPath))) {
            System.out.println("[MIXED-LOAD] users_id.xlsx not found at: " + excelPath);
            return new Object[0][0];
        }

        try (InputStream in = Files.newInputStream(Paths.get(excelPath));
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet("Users");
            if (sheet == null) sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return new Object[0][0];

            int emailCol = -1;
            for (int c = 0; c <= header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String h = cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                if ("Email".equalsIgnoreCase(h)) {
                    emailCol = c;
                    break;
                }
            }
            if (emailCol < 0) return new Object[0][0];

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cell = row.getCell(emailCol);
                if (cell == null) continue;
                String email = cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                if (!email.isEmpty()) emails.add(email);
            }
        }

        Object[][] data = new Object[emails.size()][1];
        for (int i = 0; i < emails.size(); i++) {
            data[i][0] = emails.get(i);
        }
        return data;
    }

    /**
     * Best-effort close of the currently open filter popup header (the small panel on the right),
     * by clicking its header "X" button. If not found, we quietly ignore and continue.
     */
    private void tryCloseFilterHeader() {
        try {
            By closeBtn = By.xpath(
                "//button[@class='text-gray-400 hover:text-gray-600 cursor-pointer']"
            );
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(closeBtn));
            ScrollHelper.scrollIntoView(driver, btn);
            btn.click();
            Thread.sleep(200);
            System.out.println("[MIXED] Closed filter header popup (X button).");
        } catch (Exception ignored) {
            // If header close is not found, just continue without failing the test
        }
    }

    private void openSectionIfNeeded(By tabLocator, String name, boolean alreadyOpenedFlag) {
        if (alreadyOpenedFlag) return;
        try {
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(tabLocator));
            ScrollHelper.scrollIntoViewAtTop(driver, tab);
            Thread.sleep(300);
            tab.click();
            Thread.sleep(500);
            System.out.println("[MIXED] Opened section: " + name);
        } catch (Exception e) {
            System.out.println("[MIXED] Could not open section '" + name + "': " + e.getMessage());
        }
    }

    private void ensurePersonalDetailsOpen() {
        if (personalOpened) return;
        openSectionIfNeeded(By.xpath("//span[text() = 'Personal Details']"), "Personal Details", personalOpened);
        personalOpened = true;
    }

    private void ensureProfessionalDetailsOpen() {
        if (professionalOpened) return;
        openSectionIfNeeded(By.xpath("//span[text() = 'Professional Details']"), "Professional Details", professionalOpened);
        professionalOpened = true;
    }

    private void ensureSocialsOpen() {
        if (socialsOpened) return;
        openSectionIfNeeded(By.xpath("//span[text() = 'Socials']"), "Socials", socialsOpened);
        socialsOpened = true;
    }

    private void ensureEducationalDetailsOpen() {
        if (educationOpened) return;
        openSectionIfNeeded(By.xpath("//span[text() = 'Educational Details']"), "Educational Details", educationOpened);
        educationOpened = true;
    }

    private void ensureLanguageDetailsOpen() {
        if (languageOpened) return;
        openSectionIfNeeded(By.xpath("//span[text() = 'Language Details']"), "Language Details", languageOpened);
        languageOpened = true;
    }

    private void scrollListToTop(CandidatesPage candidatesPage) {
        try {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (!links.isEmpty()) {
                ScrollHelper.scrollIntoView(driver, links.get(0));
                Thread.sleep(400);
            }
        } catch (Exception ignored) { }
    }

    private void scrollListToBottom(CandidatesPage candidatesPage) {
        try {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (!links.isEmpty()) {
                ScrollHelper.scrollIntoView(driver, links.get(links.size() - 1));
                Thread.sleep(600);
            }
        } catch (Exception ignored) { }
    }

    private static boolean textContains(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return !f.isEmpty() && (v.contains(f) || f.contains(v));
    }

    private static String normalizeStatus(String status) {
        if (status == null) return "";
        String s = status.trim().toLowerCase();
        s = s.replace("full-time", "full time")
             .replace("part-time", "part time")
             .replace("freelance", "freelancer");
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    private static boolean employmentStatusMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = normalizeStatus(filterValue);
        String v = normalizeStatus(valueOnDetails);
        return !f.isEmpty() && (v.contains(f) || f.contains(v));
    }

    private static String normalizeLanguage(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return "";
        // Remove anything in parentheses, collapse spaces, lowercase
        s = s.replaceAll("\\(.*?\\)", "");
        s = s.toLowerCase().replaceAll("\\s+", " ").trim();
        return s;
    }

    private static List<String> readSingleValueSheet(String excelPath, String sheetName, String headerName) {
        List<String> result = new ArrayList<>();
        try (InputStream in = Files.newInputStream(Paths.get(excelPath));
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return result;
            int colIndex = -1;
            for (int c = 0; c <= header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String h = cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                if (h.equalsIgnoreCase(headerName)) {
                    colIndex = c;
                    break;
                }
            }
            if (colIndex < 0) return result;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cell = row.getCell(colIndex);
                String v = getCellString(cell);
                if (!v.trim().isEmpty()) result.add(v.trim());
            }
        } catch (Exception e) {
            System.out.println("[MIXED] Failed to read sheet " + sheetName + ": " + e.getMessage());
        }
        return result;
    }

    private static List<String> readRangeSheet(String excelPath, String sheetName, String minHeader, String maxHeader) {
        List<String> result = new ArrayList<>();
        try (InputStream in = Files.newInputStream(Paths.get(excelPath));
             Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return result;
            int minCol = -1, maxCol = -1;
            for (int c = 0; c <= header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell == null) continue;
                String h = cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                if (h.equalsIgnoreCase(minHeader)) minCol = c;
                if (h.equalsIgnoreCase(maxHeader)) maxCol = c;
            }
            if (minCol < 0 || maxCol < 0) return result;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String minStr = getCellString(row.getCell(minCol)).trim();
                String maxStr = getCellString(row.getCell(maxCol)).trim();
                if (minStr.isEmpty() && maxStr.isEmpty()) continue;
                result.add(minStr + ":" + maxStr);
            }
        } catch (Exception e) {
            System.out.println("[MIXED] Failed to read range sheet " + sheetName + ": " + e.getMessage());
        }
        return result;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    @Test
    @Description("Mixed Advanced Filters – randomly pick filters/values from filters_mixed.xlsx and apply combined search")
    public void mixedAdvanceFilters_FromExcel_Randomized() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_mixed",
            "filters_mixed.xlsx"
        ).toString();

        if (!Files.exists(Paths.get(excelPath))) {
            System.out.println("[MIXED] Mixed filters Excel not found at: " + excelPath);
            return;
        }

        Map<FilterType, List<String>> availableValues = new EnumMap<>(FilterType.class);

        // Sheet -> columns (from your screenshot)
        availableValues.put(FilterType.COMPANY, readSingleValueSheet(excelPath, "CompanyFilters", "CompanyName"));
        availableValues.put(FilterType.LOCATION, readSingleValueSheet(excelPath, "LocationFilters", "LocationName"));
        availableValues.put(FilterType.DESIGNATION, readSingleValueSheet(excelPath, "DesignationFilters", "DesignationName"));
        availableValues.put(FilterType.EXPERIENCE_RANGE, readRangeSheet(excelPath, "ExperienceRanges", "MinYears", "MaxYears"));
        availableValues.put(FilterType.CURRENT_CTC, readRangeSheet(excelPath, "CurrrentCTCFilters", "MinCurrentCTC", "MaxCurrentCTC"));
        availableValues.put(FilterType.EXPECTED_CTC, readRangeSheet(excelPath, "ExpectedCTCFilters", "MinExpectedCTC", "MaxExpectedCTC"));
        availableValues.put(FilterType.GENDER, readSingleValueSheet(excelPath, "GenderFilters", "Gender"));
        availableValues.put(FilterType.MARITAL_STATUS, readSingleValueSheet(excelPath, "MaritalStatusFilters", "MaritalStatus"));
        availableValues.put(FilterType.AGE, readRangeSheet(excelPath, "AgeFilters", "MinAge", "MaxAge"));
        availableValues.put(FilterType.PHYSICALLY_CHALLENGED, readSingleValueSheet(excelPath, "PhysicallyChallengedFilter", "Value"));
        availableValues.put(FilterType.CATEGORY, readSingleValueSheet(excelPath, "CategoryFilters", "Category"));
        availableValues.put(FilterType.DEPARTMENT, readSingleValueSheet(excelPath, "DepartmentFilters", "Department"));
        availableValues.put(FilterType.ROLE, readSingleValueSheet(excelPath, "RoleFilters", "Role"));
        availableValues.put(FilterType.INDUSTRY, readSingleValueSheet(excelPath, "IndustryFilters", "Industry"));
        availableValues.put(FilterType.EMPLOYMENT_STATUS, readSingleValueSheet(excelPath, "EmploymentStatusFilters", "employment_status"));
        availableValues.put(FilterType.LANGUAGE, readSingleValueSheet(excelPath, "LanguageFilters", "Language"));
        availableValues.put(FilterType.SOCIALS, readSingleValueSheet(excelPath, "SocialsFilters", "Social"));

        // Remove empty lists so we don't select filters without data
        availableValues.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());

        if (availableValues.isEmpty()) {
            System.out.println("[MIXED] No filter values found in mixed Excel. Fill sheets first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        Random random = new Random();

        for (int run = 1; run <= MIXED_RUNS; run++) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[MIXED] Starting mixed filter run " + run);
            System.out.println("=".repeat(80));

            // reset section-open flags for this run
            personalOpened = false;
            professionalOpened = false;
            socialsOpened = false;
            educationOpened = false;
            languageOpened = false;

            List<FilterType> filterTypes = new ArrayList<>(availableValues.keySet());
            Collections.shuffle(filterTypes);
            int maxFiltersThisRun = filterTypes.size();
            int countThisRun = 1 + random.nextInt(maxFiltersThisRun);
            List<FilterType> chosenTypes = filterTypes.subList(0, countThisRun);

            Map<FilterType, String> chosenValues = new EnumMap<>(FilterType.class);
            for (FilterType type : chosenTypes) {
                List<String> vals = availableValues.get(type);
                if (vals == null || vals.isEmpty()) continue;
                String v = vals.get(random.nextInt(vals.size()));
                if (v != null && !v.trim().isEmpty()) {
                    chosenValues.put(type, v.trim());
                }
            }

            if (chosenValues.isEmpty()) {
                System.out.println("[MIXED] No concrete values selected for this run, skipping.");
                continue;
            }

            System.out.println("Mixed Filters applied:");
            int idx = 1;
            for (Map.Entry<FilterType, String> e : chosenValues.entrySet()) {
                System.out.println(idx + ") " + e.getKey().name() + " – " + e.getValue());
                idx++;
            }

            boolean firstFilterApplied = false;
            for (Map.Entry<FilterType, String> e : chosenValues.entrySet()) {
                FilterType type = e.getKey();
                String value = e.getValue();
                switch (type) {
                    case COMPANY:
                        // Randomly pick Company mode: Current+Past / Current / Past
                        String[] companyTypes = {"Current+Past", "Current", "Past"};
                        String companyType = companyTypes[random.nextInt(companyTypes.length)];
                        candidatesPage.setCompanyFilterWithType(value, companyType);
                        break;
                    case LOCATION:
                        // Randomly pick Location mode: Either / Current Location / Preferred Location / Both
                        String[] locModes = {"Either", "Current Location", "Preferred Location", "Both"};
                        String locMode = locModes[random.nextInt(locModes.length)];
                        String currentLoc = null;
                        String prefLoc = null;
                        if ("Both".equalsIgnoreCase(locMode)) {
                            currentLoc = value;
                            prefLoc = value;
                        } else if ("Current Location".equalsIgnoreCase(locMode)) {
                            currentLoc = value;
                        } else if ("Preferred Location".equalsIgnoreCase(locMode)) {
                            prefLoc = value;
                        } else { // Either (default)
                            currentLoc = value;
                        }
                        candidatesPage.setLocationFilterWithMode(locMode, currentLoc, prefLoc);
                        break;
                    case DESIGNATION:
                        candidatesPage.setDesignationFilter(value);
                        break;
                    case EXPERIENCE_RANGE: {
                        String[] parts = value.split(":", -1);
                        Integer minY = null, maxY = null;
                        try { if (!parts[0].trim().isEmpty()) minY = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                        try { if (parts.length > 1 && !parts[1].trim().isEmpty()) maxY = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                        candidatesPage.setExperienceFilter(minY, maxY);
                        break;
                    }
                    case CURRENT_CTC: {
                        String[] parts = value.split(":", -1);
                        Integer min = null, max = null;
                        try { if (!parts[0].trim().isEmpty()) min = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                        try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                        candidatesPage.setCurrentCTCFilter(min, max);
                        break;
                    }
                    case EXPECTED_CTC: {
                        String[] parts = value.split(":", -1);
                        Integer min = null, max = null;
                        try { if (!parts[0].trim().isEmpty()) min = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                        try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                        candidatesPage.setExpectedCTCFilter(min, max);
                        break;
                    }
                    case GENDER:
                        ensurePersonalDetailsOpen();
                        candidatesPage.setGenderFilter(value);
                        break;
                    case MARITAL_STATUS:
                        ensurePersonalDetailsOpen();
                        candidatesPage.setMaritalStatusFilter(value);
                        break;
                    case AGE: {
                        String[] parts = value.split(":", -1);
                        Integer min = null, max = null;
                        try { if (!parts[0].trim().isEmpty()) min = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                        try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                        ensurePersonalDetailsOpen();
                        candidatesPage.setAgeFilter(min, max);
                        break;
                    }
                    case PHYSICALLY_CHALLENGED:
                        ensurePersonalDetailsOpen();
                        candidatesPage.setPhysicallyChallengedFilter(value);
                        break;
                    case CATEGORY:
                        ensurePersonalDetailsOpen();
                        candidatesPage.setCategoryFilter(value);
                        break;
                    case DEPARTMENT:
                        ensureProfessionalDetailsOpen();
                        candidatesPage.setDepartmentFilter(value);
                        break;
                    case ROLE:
                        ensureProfessionalDetailsOpen();
                        candidatesPage.setRoleFilter(value);
                        break;
                    case INDUSTRY:
                        ensureProfessionalDetailsOpen();
                        candidatesPage.setIndustryFilter(value);
                        break;
                    case EMPLOYMENT_STATUS:
                        ensureProfessionalDetailsOpen();
                        candidatesPage.setEmploymentStatusFilter(value);
                        break;
                    case LANGUAGE:
                        ensureLanguageDetailsOpen();
                        try {
                            By searchInputBy = By.xpath(
                                "//input[@placeholder='Search Language' or contains(@placeholder,'Search Language') or contains(@aria-label,'Search Language')]"
                            );
                            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputBy));
                            ScrollHelper.scrollIntoView(driver, input);
                            input.clear();
                            input.sendKeys(value);
                            Thread.sleep(300);
                            input.sendKeys(org.openqa.selenium.Keys.ENTER);
                            Thread.sleep(600);
                            System.out.println("[MIXED][LANGUAGE] Added language card: " + value);
                        } catch (Exception e2) {
                            System.out.println("[MIXED][LANGUAGE] Search Language input/card creation failed for '" + value + "': " + e2.getMessage());
                        }
                        try {
                            By applyBtn = By.xpath("//button[contains(normalize-space(.),'Apply Filter') or contains(normalize-space(.),'Apply filter')]");
                            WebElement apply = wait.until(ExpectedConditions.elementToBeClickable(applyBtn));
                            ScrollHelper.scrollIntoView(driver, apply);
                            apply.click();
                            System.out.println("[MIXED][LANGUAGE] Clicked Apply Filter for Languages.");
                        } catch (Exception e2) {
                            System.out.println("[MIXED][LANGUAGE] Apply Filter button not found/clickable: " + e2.getMessage());
                        }
                        break;
                    case SOCIALS:
                        ensureSocialsOpen();
                        candidatesPage.setSocialsFilter(value);
                        break;
                    default:
                        break;
                }

                // After applying any filter, wait a bit so the UI can update
                candidatesPage.waitForLoaderGone();
                Thread.sleep(2000);
                // Try to close the active filter panel header (X button). If not present, ignore.
                tryCloseFilterHeader();

                // After applying the first filter, if there are 0 profiles, stop this mixed run early
                if (!firstFilterApplied) {
                    int countAfterFirst = candidatesPage.getResultCount();
                    System.out.println("[MIXED] Result count after first filter (" + type.name() + "): " + countAfterFirst);
                    firstFilterApplied = true;
                    if (countAfterFirst <= 0) {
                        System.out.println("[MIXED] No profiles after first filter. Skipping remaining filters and sampling for this run.");
                        candidatesPage.clickClearFiltersIfPresent();
                        Thread.sleep(800);
                        continue; // move to next filter in loop, but we will detect zero later and skip sampling
                    }
                }
            }

            candidatesPage.waitForLoaderGone();
            Thread.sleep(1000);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[MIXED] No results for this mixed filter combination. Skipping sampling.");
                candidatesPage.clickClearFiltersIfPresent();
                Thread.sleep(500);
                continue;
            }

            int totalPages = (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            if (totalPages < 1) totalPages = 1;

            System.out.println("[MIXED] Total candidates: " + totalCount + " | Pages: " + totalPages);

            String parentHandle = driver.getWindowHandle();
            List<MixedCandidateRow> summaries = new ArrayList<>();

            StringBuilder filtersAppliedSb = new StringBuilder();
            int fIdx = 1;
            for (Map.Entry<FilterType, String> e : chosenValues.entrySet()) {
                filtersAppliedSb.append(fIdx)
                    .append(") ")
                    .append(e.getKey().name())
                    .append(" - ")
                    .append(e.getValue())
                    .append(" | ");
                fIdx++;
            }
            String filtersAppliedStr = filtersAppliedSb.toString();

            for (int page = 1; page <= totalPages; page++) {
                if (page > 1) {
                    candidatesPage.scrollToBottom();
                    candidatesPage.selectPage(page);
                    candidatesPage.waitForLoaderGone();
                    Thread.sleep(800);
                }

                scrollListToTop(candidatesPage);

                List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (links.isEmpty()) continue;

                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < links.size(); i++) indices.add(i);
                Collections.shuffle(indices);
                int toOpen = Math.min(SAMPLES_PER_PAGE, indices.size());

                System.out.println("[MIXED] Page " + page + "/" + totalPages +
                    " – opening " + toOpen + " random candidates (of " + links.size() + " on page)");

                for (int i = 0; i < toOpen; i++) {
                    int idxOnPage = indices.get(i);
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

                        CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                        String detailsUrl = driver.getCurrentUrl();
                        String candidateName = detailsPage.getCandidateName();

                        StringBuilder candidateValuesSb = new StringBuilder();
                        boolean allMatched = true;
                        int num = 1;
                        for (Map.Entry<FilterType, String> e : chosenValues.entrySet()) {
                            FilterType type = e.getKey();
                            String filterVal = e.getValue();
                            String gotVal = "";
                            boolean thisMatch = false;

                            switch (type) {
                                case COMPANY:
                                    gotVal = detailsPage.getCurrentCompany();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case LOCATION: {
                                    String currentLoc = detailsPage.getCurrentLocation();
                                    List<String> prefs = detailsPage.getPrefLocationsAsList();
                                    gotVal = (currentLoc == null ? "" : currentLoc) +
                                        (prefs.isEmpty() ? "" : " | " + String.join(",", prefs));
                                    String f = filterVal.trim().toLowerCase();
                                    boolean inCurrent = currentLoc != null && currentLoc.toLowerCase().contains(f);
                                    boolean inPref = prefs.stream().anyMatch(p -> p.toLowerCase().contains(f));
                                    thisMatch = inCurrent || inPref;
                                    break;
                                }
                                case DESIGNATION:
                                    gotVal = detailsPage.getDesignationFromCurrent();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case EXPERIENCE_RANGE: {
                                    String[] parts = filterVal.split(":", -1);
                                    Double min = null, max = null;
                                    try { if (!parts[0].trim().isEmpty()) min = Double.parseDouble(parts[0].trim()); } catch (Exception ignored) {}
                                    try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Double.parseDouble(parts[1].trim()); } catch (Exception ignored) {}
                                    Double years = detailsPage.getExperienceYears();
                                    gotVal = years == null ? "" : years.toString();
                                    if (years != null) {
                                        boolean geMin = (min == null) || years >= min;
                                        boolean leMax = (max == null) || years <= max;
                                        thisMatch = geMin && leMax;
                                    }
                                    break;
                                }
                                case CURRENT_CTC: {
                                    String[] parts = filterVal.split(":", -1);
                                    Double min = null, max = null;
                                    try { if (!parts[0].trim().isEmpty()) min = Double.parseDouble(parts[0].trim()); } catch (Exception ignored) {}
                                    try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Double.parseDouble(parts[1].trim()); } catch (Exception ignored) {}
                                    Double ctc = detailsPage.getCurrentCTCLakhs();
                                    gotVal = ctc == null ? "" : ctc.toString();
                                    if (ctc != null) {
                                        boolean geMin = (min == null) || ctc >= min;
                                        boolean leMax = (max == null) || ctc <= max;
                                        thisMatch = geMin && leMax;
                                    }
                                    break;
                                }
                                case EXPECTED_CTC: {
                                    String[] parts = filterVal.split(":", -1);
                                    Double min = null, max = null;
                                    try { if (!parts[0].trim().isEmpty()) min = Double.parseDouble(parts[0].trim()); } catch (Exception ignored) {}
                                    try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Double.parseDouble(parts[1].trim()); } catch (Exception ignored) {}
                                    Double ctc = detailsPage.getExpectedCTCLakhs();
                                    gotVal = ctc == null ? "" : ctc.toString();
                                    if (ctc != null) {
                                        boolean geMin = (min == null) || ctc >= min;
                                        boolean leMax = (max == null) || ctc <= max;
                                        thisMatch = geMin && leMax;
                                    }
                                    break;
                                }
                                case GENDER:
                                    gotVal = detailsPage.getGender();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case MARITAL_STATUS:
                                    gotVal = detailsPage.getMaritalStatus();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case AGE: {
                                    String[] parts = filterVal.split(":", -1);
                                    Integer min = null, max = null;
                                    try { if (!parts[0].trim().isEmpty()) min = Integer.parseInt(parts[0].trim()); } catch (Exception ignored) {}
                                    try { if (parts.length > 1 && !parts[1].trim().isEmpty()) max = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                                    String ageStr = detailsPage.getAge();
                                    gotVal = ageStr;
                                    try {
                                        int age = Integer.parseInt(ageStr);
                                        boolean geMin = (min == null) || age >= min;
                                        boolean leMax = (max == null) || age <= max;
                                        thisMatch = geMin && leMax;
                                    } catch (Exception ignored) { }
                                    break;
                                }
                                case PHYSICALLY_CHALLENGED:
                                    gotVal = detailsPage.getPhysicallyChallenged();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case CATEGORY:
                                    gotVal = detailsPage.getCategory();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case DEPARTMENT:
                                    gotVal = detailsPage.getDepartment();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case ROLE:
                                    gotVal = detailsPage.getRole();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case INDUSTRY:
                                    gotVal = detailsPage.getIndustry();
                                    thisMatch = textContains(filterVal, gotVal);
                                    break;
                                case EMPLOYMENT_STATUS:
                                    gotVal = detailsPage.getEmploymentStatus();
                                    thisMatch = employmentStatusMatches(filterVal, gotVal);
                                    break;
                                case LANGUAGE: {
                                    // Parse "Languages known" section similar to LanguageFilterTest
                                    List<String> langLines = new ArrayList<>();
                                    try {
                                        By langSectionBy = By.xpath("//h3[normalize-space(text())='Languages known']/parent::div");
                                        WebElement section = driver.findElement(langSectionBy);
                                        ScrollHelper.scrollIntoView(driver, section);
                                        Thread.sleep(400);

                                        List<WebElement> rows = section.findElements(
                                            By.xpath(".//div[contains(@class,'text-sm') and contains(@class,'text-gray-700')]")
                                        );
                                        for (WebElement row : rows) {
                                            String rowText = row.getText() == null ? "" : row.getText().trim();
                                            if (!rowText.isEmpty()) {
                                                langLines.add(rowText);
                                            }
                                        }
                                    } catch (Exception ignoreLang) { }

                                    gotVal = langLines.isEmpty() ? "" : String.join(" || ", langLines);

                                    String normFilterLang = normalizeLanguage(filterVal);
                                    boolean anyMatch = false;
                                    for (String line : langLines) {
                                        String normLine = normalizeLanguage(line);
                                        if (!normLine.isEmpty() && (normLine.contains(normFilterLang) || normFilterLang.contains(normLine))) {
                                            anyMatch = true;
                                            break;
                                        }
                                    }
                                    thisMatch = anyMatch;
                                    break;
                                }
                                case SOCIALS:
                                    boolean present = detailsPage.hasSocial(filterVal);
                                    gotVal = present ? "Present" : "Not Present";
                                    thisMatch = present;
                                    break;
                                default:
                                    break;
                            }

                            candidateValuesSb.append(num)
                                .append(") ")
                                .append(type.name())
                                .append(" - ")
                                .append(gotVal == null ? "(not found)" : gotVal)
                                .append(" | ");

                            if (!thisMatch) {
                                allMatched = false;
                            }
                            num++;
                        }

                        String candidateValuesStr = candidateValuesSb.toString();

                        summaries.add(new MixedCandidateRow(
                            page,
                            idxOnPage + 1,
                            candidateName == null ? "" : candidateName.trim(),
                            filtersAppliedStr,
                            candidateValuesStr,
                            allMatched,
                            detailsUrl
                        ));

                        driver.close();
                        driver.switchTo().window(parentHandle);
                        Thread.sleep(500);
                    } catch (Exception e) {
                        try {
                            driver.switchTo().window(parentHandle);
                        } catch (Exception ignored) { }
                        System.out.println("[MIXED] Error while sampling candidate on page " + page + ": " + e.getMessage());
                    }
                }

                if (page < totalPages) {
                    scrollListToBottom(candidatesPage);
                }
            }

            System.out.println("\n[MIXED] Summary for mixed filter run " + run + ":");
            System.out.println("Mixed Filters applied:");
            System.out.println(filtersAppliedStr);
            System.out.printf("%-6s | %-10s | %-28s | %-60s | %-60s | %-10s | %-60s%n",
                "Page", "Index", "Candidate Name", "Filters Applied", "Candidate Values", "Result", "Candidate URL");
            System.out.println("-".repeat(260));

            for (MixedCandidateRow r : summaries) {
                System.out.printf("%-6d | %-10d | %-28s | %-60s | %-60s | %-10s | %-60s%n",
                    r.page,
                    r.indexOnPage,
                    truncate(r.candidateName, 28),
                    truncate(r.filtersApplied, 60),
                    truncate(r.candidateValues, 60),
                    r.allMatched ? "MATCH" : "UNMATCHED",
                    truncate(r.url, 60));
            }

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(800);
        }
    }

    // === PARALLEL MULTI-USER LOAD TEST INSIDE SAME CLASS ===

    @Test(dataProvider = "userEmails", threadPoolSize = 50)
    @Description("Mixed Advanced Filters – continuous multi-user load using users_id.xlsx (CPU spike test)")
    public void mixedAdvanceFilters_MultiUser_Load_FromUsersExcel(String email) throws Exception {
        String password = "Admin@123";

        System.out.println("\n" + "=".repeat(80));
        System.out.println("[MIXED-LOAD] Starting browser for user: " + email);
        System.out.println("=".repeat(80));

        // Measure login time per user
        Instant loginStart = Instant.now();
        try {
            loginPage lp = new loginPage(driver);
            lp.loginAs(email, password);
        } catch (TimeoutException te) {
            Instant loginEnd = Instant.now();
            long loginMs = Duration.between(loginStart, loginEnd).toMillis();
            System.out.println("[MIXED-LOAD][LOGIN-FAILED] User: " + email + " | Time: " + loginMs + " ms | Timeout: " + te.getMessage());
            throw te;
        } catch (Exception e) {
            Instant loginEnd = Instant.now();
            long loginMs = Duration.between(loginStart, loginEnd).toMillis();
            System.out.println("[MIXED-LOAD][LOGIN-FAILED] User: " + email + " | Time: " + loginMs + " ms | Error: " + e.getMessage());
            throw e;
        }
        Instant loginEnd = Instant.now();
        long loginMs = Duration.between(loginStart, loginEnd).toMillis();
        System.out.println("[MIXED-LOAD][LOGIN-SUCCESS] User: " + email + " | Login time: " + loginMs + " ms");

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        // Preload simple filter values from filters_mixed.xlsx
        String projectRoot = System.getProperty("user.dir");
        String mixedExcelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_mixed",
            "filters_mixed.xlsx"
        ).toString();

        List<String> companies = readSingleValueSheet(mixedExcelPath, "CompanyFilters", "CompanyName");
        List<String> locations = readSingleValueSheet(mixedExcelPath, "LocationFilters", "LocationName");
        List<String> departments = readSingleValueSheet(mixedExcelPath, "DepartmentFilters", "Department");
        List<String> roles = readSingleValueSheet(mixedExcelPath, "RoleFilters", "Role");
        List<String> industries = readSingleValueSheet(mixedExcelPath, "IndustryFilters", "Industry");

        Random rnd = new Random();
        int iteration = 0;

        while (true) {
            iteration++;
            System.out.println("\n[MIXED-LOAD][" + email + "] Mixed filter iteration " + iteration);

            List<SimpleFilterType> allTypes = new ArrayList<>();
            if (!companies.isEmpty()) allTypes.add(SimpleFilterType.COMPANY);
            if (!locations.isEmpty()) allTypes.add(SimpleFilterType.LOCATION);
            if (!departments.isEmpty()) allTypes.add(SimpleFilterType.DEPARTMENT);
            if (!roles.isEmpty()) allTypes.add(SimpleFilterType.ROLE);
            if (!industries.isEmpty()) allTypes.add(SimpleFilterType.INDUSTRY);

            if (allTypes.isEmpty()) {
                System.out.println("[MIXED-LOAD][" + email + "] No simple mixed filter values available. Ending loop.");
                break;
            }

            Collections.shuffle(allTypes, rnd);
            int howMany = 1 + rnd.nextInt(allTypes.size());
            List<SimpleFilterType> chosen = allTypes.subList(0, howMany);

            System.out.println("[MIXED-LOAD][" + email + "] Filters applied this iteration:");
            for (SimpleFilterType t : chosen) {
                String val;
                switch (t) {
                    case COMPANY:
                        val = companies.get(rnd.nextInt(companies.size()));
                        System.out.println("  - COMPANY = " + val);
                        candidatesPage.setCompanyFilter(val);
                        break;
                    case LOCATION:
                        val = locations.get(rnd.nextInt(locations.size()));
                        System.out.println("  - LOCATION = " + val);
                        candidatesPage.setLocationFilter(val);
                        break;
                    case DEPARTMENT:
                        val = departments.get(rnd.nextInt(departments.size()));
                        System.out.println("  - DEPARTMENT = " + val);
                        candidatesPage.setDepartmentFilter(val);
                        break;
                    case ROLE:
                        val = roles.get(rnd.nextInt(roles.size()));
                        System.out.println("  - ROLE = " + val);
                        candidatesPage.setRoleFilter(val);
                        break;
                    case INDUSTRY:
                        val = industries.get(rnd.nextInt(industries.size()));
                        System.out.println("  - INDUSTRY = " + val);
                        candidatesPage.setIndustryFilter(val);
                        break;
                    default:
                        break;
                }
            }

            candidatesPage.waitForLoaderGone();
            int count = candidatesPage.getResultCount();
            System.out.println("[MIXED-LOAD][" + email + "] Result count after iteration " + iteration + ": " + count);

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(1000);
        }
    }
}



