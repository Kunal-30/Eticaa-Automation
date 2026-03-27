package testPackage.filters.BaseFilters.designation;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.designation_filter_Util;
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
 * Advanced Filter – Designation filters from Excel, assert on candidate details, mark and log candidate names.
 *
 * Logic is copied from superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_Designation_Filter
 * without any functional changes, only relocated into its own test class.
 */
@Feature("Advanced Filters - Designation")
public class DesignationFilterTest extends basePage {

    /** Default pagination size (candidates per page). */
    private static final int PAGE_SIZE = 25;

    /** Table row for Designation filter results. */
    private static class DesignationFilterResultRow {
        final String candidateName;
        final String filterDesignation;
        final String currentDesignation;
        final boolean match;

        DesignationFilterResultRow(String candidateName, String filterDesignation,
                                   String currentDesignation, boolean match) {
            this.candidateName = candidateName;
            this.filterDesignation = filterDesignation;
            this.currentDesignation = currentDesignation;
            this.match = match;
        }
    }

    /** Helper to truncate long strings so console tables stay aligned. */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /** Print summary table for one Designation filter value. */
    private void printDesignationFilterSummaryTable(String designation,
                                                    int totalCount,
                                                    int sampledCount,
                                                    List<DesignationFilterResultRow> rows) {
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
        long matchCount = rows.stream().filter(r -> r.match).count();
        long markedCount = rows.size() - matchCount;

        System.out.println("======================================================================");
        System.out.println("[ADV FILTER][DESIGNATION] FILTER SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Filter Type      : Designation");
        System.out.println("Filter Value     : " + designation);
        System.out.println("Total Results    : " + totalCount);
        System.out.println("Rows per Page    : " + PAGE_SIZE);
        System.out.println("Total Pages      : " + totalPages);
        System.out.println("Profiles Sampled : " + sampledCount);
        System.out.println("MATCH            : " + matchCount);
        System.out.println("MARKED           : " + markedCount);
        System.out.println("======================================================================");

        String headerFormat = "| %-3s | %-30s | %-25s | %-25s | %-8s |%n";
        String rowFormat    = "| %-3d | %-30s | %-25s | %-25s | %-8s |%n";

        System.out.printf(headerFormat, "#", "Candidate Name", "Filter Designation", "Current Designation", "Result");
        System.out.println("----------------------------------------------------------------------");

        int index = 1;
        for (DesignationFilterResultRow r : rows) {
            String resultText = r.match ? "MATCH" : "MARKED";
            System.out.printf(
                rowFormat,
                index++,
                truncate(r.candidateName, 30),
                truncate(r.filterDesignation, 25),
                truncate(r.currentDesignation, 25),
                resultText
            );
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
        System.out.println("[FRAMEWORK] BeforeClass (DesignationFilter): Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass (DesignationFilter): Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Test No 6: Advanced Filter – Designation filters from Excel, assert on candidate details, mark and log")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Designation_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 6: Advanced Filter (DesignationMaster.xlsx, random Designation filters, mark + log candidate name)");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String designationMasterPath = Paths.get(projectRoot, "src", "test", "resources", "DesignationMaster.xlsx").toString();
        // Ensure template exists; you have already filled DesignationMaster.xlsx
        designation_filter_Util.createEmptyTemplateIfMissing(designationMasterPath);

        List<String> allDesignations = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("DesignationMaster.xlsx")) {
            if (excelIn != null) allDesignations = designation_filter_Util.readDesignationNames(excelIn);
        }
        if (allDesignations.isEmpty() && Files.exists(Paths.get(designationMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(designationMasterPath))) {
                allDesignations = designation_filter_Util.readDesignationNames(in);
            }
        }
        if (allDesignations.isEmpty()) {
            System.out.println("[ADVANCE FILTER][DESIGNATION] No Designations in DesignationMaster. Ensure DesignationMaster.xlsx has data.");
            return;
        }
        List<FilterRow> rows = designation_filter_Util.getRandomDesignationFilterRows(allDesignations, 10);
        System.out.println("[ADVANCE FILTER][DESIGNATION] Using " + rows.size() + " random Designation criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue());
        }

        // Use dedicated advanced-filter user (never deleted)
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters before starting the loop
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            String designation = row.getFilterValue();
            if (designation == null || designation.trim().isEmpty()) continue;

            // Clear previous filters and directly add the new one; Advance Search is already open
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            candidatesPage.setFilterByType("Designation", designation);

            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][DESIGNATION] Filter: Designation = " + designation + " | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            List<DesignationFilterResultRow> designationRowsForThisFilter = new ArrayList<>();
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
                    String currentDesignation = detailsPage.getDesignationFromCurrent();
                    boolean found = detailsPage.isFilterValuePresentInSection("Designation", designation, row.getType(), row.getIncludeExclude(), row.getNotes());
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH | Designation = " + designation);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED | Designation not found for filter = " + designation);
                    }
                    // Add row for detailed Designation summary table
                    designationRowsForThisFilter.add(new DesignationFilterResultRow(
                        candidateName,
                        designation,
                        currentDesignation,
                        found
                    ));
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][DESIGNATION] REPORT for Designation = " + designation
                + ": Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);
            // Detailed table for this Designation filter value
            printDesignationFilterSummaryTable(
                designation,
                totalCount,
                designationRowsForThisFilter.size(),
                designationRowsForThisFilter
            );
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 6 SUMMARY: Designation filters (from Excel) run completed. See logs for marked candidate names.");
        System.out.println("=".repeat(80));
    }
}

