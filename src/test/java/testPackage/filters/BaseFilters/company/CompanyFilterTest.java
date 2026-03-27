package testPackage.filters.BaseFilters.company;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.company_filter_Util;
import Utils.FilterRow;
import Utils.SSHTunnelManager;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Advanced Filter – Company filter only (moved out of superadmintest).
 *
 * Logic is copied from superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_Company_Filter
 * without any functional changes, only relocated into its own test class.
 */
@Feature("Advanced Filters - Company")
public class CompanyFilterTest extends basePage {

    /** Default pagination size (candidates per page). */
    private static final int PAGE_SIZE = 25;

    /** Table row for Company filter results (one row per sampled candidate). */
    private static class CompanyFilterResultRow {
        final String candidateName;
        final String filterValue;
        final String filterType; // Current / Past / Current+Past
        final boolean match;     // true = MATCH, false = MARKED

        CompanyFilterResultRow(String candidateName, String filterValue, String filterType, boolean match) {
            this.candidateName = candidateName;
            this.filterValue = filterValue;
            this.filterType = filterType;
            this.match = match;
        }
    }

    /** Helper to truncate long strings so console tables stay aligned. */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /** Print a detailed console table summarizing one Company filter value. */
    private void printCompanyFilterSummaryTable(String filterValue,
                                                String filterType,
                                                int totalCount,
                                                int sampledCount,
                                                List<CompanyFilterResultRow> rows) {
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
        long matchCount = rows.stream().filter(r -> r.match).count();
        long markedCount = rows.size() - matchCount;

        System.out.println("======================================================================");
        System.out.println("[ADV FILTER][COMPANY] FILTER SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Filter Type      : Company");
        System.out.println("Filter Value     : " + filterValue);
        System.out.println("Filter Mode      : " + filterType);
        System.out.println("Total Results    : " + totalCount);
        System.out.println("Rows per Page    : " + PAGE_SIZE);
        System.out.println("Total Pages      : " + totalPages);
        System.out.println("Profiles Sampled : " + sampledCount);
        System.out.println("MATCH            : " + matchCount);
        System.out.println("MARKED           : " + markedCount);
        System.out.println("======================================================================");

        String headerFormat = "| %-3s | %-30s | %-15s | %-8s |%n";
        String rowFormat    = "| %-3d | %-30s | %-15s | %-8s |%n";

        System.out.printf(headerFormat, "#", "Candidate Name", "Filter Mode", "Result");
        System.out.println("----------------------------------------------------------------------");

        int index = 1;
        for (CompanyFilterResultRow r : rows) {
            String resultText = r.match ? "MATCH" : "MARKED";
            System.out.printf(rowFormat, index++, truncate(r.candidateName, 30), r.filterType, resultText);
        }
        System.out.println("----------------------------------------------------------------------");
    }

    /**
     * For each page 1..totalPages (based on totalCount and PAGE_SIZE), navigates to that page,
     * picks 2–4 random candidate links, and calls processOne(link, pageNum) for each.
     * Only used when totalCount > 25 (pagination is visible).
     */
    private void forEachPageSampleCandidates(CandidatesPage candidatesPage, int totalCount, String parentHandle,
                                             BiConsumer<WebElement, Integer> processOne) throws InterruptedException {
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages <= 0) return;
        Random rnd = new Random();
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            System.out.println("[CANDIDATES] Sampling candidates from page " + pageNum + " of " + totalPages + ".");
            if (pageNum > 1) {
                candidatesPage.selectPage(pageNum);
                Thread.sleep(600);
            }
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) continue;
            int sampleCount = Math.min(2 + rnd.nextInt(3), links.size()); // 2, 3 or 4 per page
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int j = 0; j < sampleCount; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), pageNum);
            }
        }
    }

    /**
     * Sample candidates for one applied filter. Pagination is only visible when results > 25.
     * - If totalCount > 25: use pagination, 2–4 random per page.
     * - If totalCount <= 25: no pagination; open 10–12 randomly (or all if fewer).
     */
    private void sampleCandidatesForFilter(CandidatesPage candidatesPage, int totalCount, String parentHandle,
                                           BiConsumer<WebElement, Integer> processOne) throws InterruptedException {
        if (totalCount <= 0) return;
        Random rnd = new Random();
        if (totalCount > PAGE_SIZE) {
            System.out.println("[CANDIDATES] Results = " + totalCount + " (> " + PAGE_SIZE + "): pagination visible. Sampling 2–4 per page.");
            forEachPageSampleCandidates(candidatesPage, totalCount, parentHandle, processOne);
        } else {
            System.out.println("[CANDIDATES] Results = " + totalCount + " (≤ " + PAGE_SIZE + "): no pagination. Sampling from single page.");
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) return;
            int toOpen;
            if (links.size() >= 12) {
                toOpen = 10 + rnd.nextInt(3); // 10, 11, or 12
            } else {
                toOpen = links.size(); // open all
            }
            toOpen = Math.min(toOpen, links.size());
            System.out.println("[CANDIDATES] Opening " + toOpen + " of " + links.size() + " candidates (from Page 1).");
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int j = 0; j < toOpen; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), 1);
            }
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] BeforeClass (CompanyFilter): Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass (CompanyFilter): Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    /**
     * Test No 4: Advanced Filter on Candidates – data from Excel template.
     * Creates CompanyMaster.xlsx from company_names.txt if missing.
     * For each Excel row: apply single Company filter → get result count → open each candidate in new tab →
     * assert filter value in details page; if not found, mark and log candidate name (Option 2 – test does not fail).
     *
     * Logic is identical to superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_Company_Filter.
     */
    @Test
    @Description("Test No 4: Advanced Filter – 10 random company criteria (Past/Current/Current+Past) from CompanyMaster Excel, assert on details, mark and log")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Company_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 4: Advanced Filter – 10 random company searches (Past / Current / Current+Past) from CompanyMaster");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String companyMasterPath = Paths.get(projectRoot, "src", "test", "resources", "CompanyMaster.xlsx").toString();
        try (InputStream namesIn = getClass().getClassLoader().getResourceAsStream("company_names.txt")) {
            company_filter_Util.createCompanyMasterFromTextIfMissing(companyMasterPath, namesIn);
        }
        List<String> allCompanies = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("CompanyMaster.xlsx")) {
            if (excelIn != null) allCompanies = company_filter_Util.readCompanyNames(excelIn);
        }
        if (allCompanies.isEmpty() && Files.exists(Paths.get(companyMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(companyMasterPath))) {
                allCompanies = company_filter_Util.readCompanyNames(in);
            }
        }
        if (allCompanies.isEmpty()) {
            System.out.println("[ADVANCE FILTER] No companies in CompanyMaster. Ensure company_names.txt or CompanyMaster.xlsx exists.");
            return;
        }
        List<FilterRow> rows = company_filter_Util.getRandomCompanyFilterRows(allCompanies, 10);
        System.out.println("[ADVANCE FILTER] Using " + rows.size() + " random company criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue() + " (Type: " + r.getType() + ")");
        }

        // Use dedicated advanced-filter user (never deleted)
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters before starting the loop
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            if (row.isEmpty()) continue;
            String filterType = row.getFilterType();
            String filterValue = row.getFilterValue();
            String type = row.getType();

            // Clear previous filter(s) and directly add the new one; DO NOT click Advance Search again
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            // Advance Search panel is already open; just set the new filter
            candidatesPage.setFilterByType(filterType, filterValue, type);

            int totalCount = candidatesPage.getResultCount();
            int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][COMPANY] Filter applied: " + filterType + " = " + filterValue + " (Type: " + type + ")");
            System.out.println("[ADVANCE FILTER][COMPANY] Total results: " + totalCount + " | Rows per page: " + PAGE_SIZE + " | Pages: " + totalPages);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            List<CompanyFilterResultRow> companyRowsForThisFilter = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    boolean found = detailsPage.isFilterValuePresentInSection(
                        filterType, filterValue, row.getType(), row.getIncludeExclude(), row.getNotes()
                    );
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH (value found) | Filter: " + filterType + " = " + filterValue);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED (value not found) | Filter: " + filterType + " = " + filterValue);
                    }
                    // Add row for detailed summary table (per sampled candidate)
                    companyRowsForThisFilter.add(new CompanyFilterResultRow(candidateName, filterValue, row.getType(), found));
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER] REPORT for " + filterType + " = " + filterValue + " (Type: " + row.getType() + "): Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);

            // Detailed table for this Company filter value
            printCompanyFilterSummaryTable(
                filterValue,
                row.getType(),
                totalCount,
                companyRowsForThisFilter.size(),
                companyRowsForThisFilter
            );
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 4 SUMMARY: 10 random company filters completed. See logs for marked candidate names.");
        System.out.println("=".repeat(80));
    }
}

