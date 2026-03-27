package testPackage.filters.BaseFilters.ctc;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.ctc_filter_Util;
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
 * Advanced Filter – Expected CTC ranges from CTCMaster.xlsx.
 *
 * Logic is copied from superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_ExpectedCTC_Filter
 * without functional changes, only relocated into its own test class.
 */
@Feature("Advanced Filters - Expected CTC")
public class ExpectedCtcFilterTest extends basePage {

    /** Default pagination size (candidates per page). */
    private static final int PAGE_SIZE = 25;

    /** Table row for Expected CTC filter results. */
    private static class ExpectedCTCFilterResultRow {
        final String candidateName;
        final int minLakhs;
        final int maxLakhs;
        final Double ctcLakhs;
        final boolean inRange;

        ExpectedCTCFilterResultRow(String candidateName, int minLakhs, int maxLakhs,
                                   Double ctcLakhs, boolean inRange) {
            this.candidateName = candidateName;
            this.minLakhs = minLakhs;
            this.maxLakhs = maxLakhs;
            this.ctcLakhs = ctcLakhs;
            this.inRange = inRange;
        }
    }

    /** Helper to truncate long strings so console tables stay aligned. */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /** Print summary table for one Expected CTC range. */
    private void printExpectedCTCFilterSummaryTable(int min,
                                                    int max,
                                                    int totalCount,
                                                    int sampledCount,
                                                    List<ExpectedCTCFilterResultRow> rows) {
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
        long inRangeCount = rows.stream().filter(r -> r.inRange).count();
        long outOfRangeCount = rows.size() - inRangeCount;

        System.out.println("======================================================================");
        System.out.println("[ADV FILTER][EXPECTED CTC] FILTER SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Filter Type      : Expected CTC");
        System.out.println("Range (Lakhs)    : " + min + " - " + max);
        System.out.println("Total Results    : " + totalCount);
        System.out.println("Rows per Page    : " + PAGE_SIZE);
        System.out.println("Total Pages      : " + totalPages);
        System.out.println("Profiles Sampled : " + sampledCount);
        System.out.println("IN RANGE         : " + inRangeCount);
        System.out.println("OUT OF RANGE     : " + outOfRangeCount);
        System.out.println("======================================================================");

        String headerFormat = "| %-3s | %-30s | %-10s | %-8s |%n";
        String rowFormat    = "| %-3d | %-30s | %-10s | %-8s |%n";

        System.out.printf(headerFormat, "#", "Candidate Name", "CTC (L)", "Result");
        System.out.println("----------------------------------------------------------------------");

        int index = 1;
        for (ExpectedCTCFilterResultRow r : rows) {
            String resultText = r.inRange ? "IN-RANGE" : "OUT";
            System.out.printf(
                rowFormat,
                index++,
                truncate(r.candidateName, 30),
                r.ctcLakhs != null ? r.ctcLakhs + "L" : "-",
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
        System.out.println("[FRAMEWORK] BeforeClass (ExpectedCtcFilter): Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass (ExpectedCtcFilter): Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Test No 9: Advanced Filter – Expected CTC ranges from Excel, sample 2–3 per page, open profiles, log and assert")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_ExpectedCTC_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8b: Advanced Filter – Expected CTC ranges from CTCMaster.xlsx (sample 2–3 per page, open profiles)");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String ctcMasterPath = Paths.get(projectRoot, "src", "test", "resources", "CTCMaster.xlsx").toString();
        ctc_filter_Util.createEmptyTemplateIfMissing(ctcMasterPath);

        List<int[]> allRanges = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("CTCMaster.xlsx")) {
            if (excelIn != null) allRanges = ctc_filter_Util.readCTCRanges(excelIn);
        }
        if (allRanges.isEmpty() && Files.exists(Paths.get(ctcMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(ctcMasterPath))) {
                allRanges = ctc_filter_Util.readCTCRanges(in);
            }
        }
        if (allRanges.isEmpty()) {
            System.out.println("[ADVANCE FILTER][EXPECTED CTC] No valid ranges in CTCMaster. Ensure MinLakhs, MaxLakhs are filled.");
            return;
        }
        List<int[]> ranges = ctc_filter_Util.getRandomCTCRanges(allRanges, 10);
        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Using " + ranges.size() + " range(s) from Excel:");
        for (int[] r : ranges) System.out.println("  - " + r[0] + " to " + r[1] + " Lakhs");

        // Use dedicated advanced-filter user (never deleted)
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setExpectedCTCFilter(min, max);
            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][EXPECTED CTC] Filter: " + min + " to " + max + " Lakhs | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            List<ExpectedCTCFilterResultRow> expectedCTCRowsForThisRange = new ArrayList<>();
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
                    Double ctcLakhs = detailsPage.getExpectedCTCLakhs();
                    boolean inRange = ctcLakhs != null && ctcLakhs >= min && ctcLakhs <= max;
                    if (inRange) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MATCH | ECTC=" + ctcLakhs + "L in [" + min + "-" + max + "]");
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MARKED | ECTC=" + (ctcLakhs != null ? ctcLakhs + "L" : "not found") + " | Filter: " + min + "-" + max + "L");
                    }
                    // Add row for detailed Expected CTC summary table
                    expectedCTCRowsForThisRange.add(new ExpectedCTCFilterResultRow(
                        candidateName,
                        min,
                        max,
                        ctcLakhs,
                        inRange
                    ));
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate (Page " + pageNum + ") | Error: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][EXPECTED CTC] REPORT for " + min + "-" + max + "L: Total=" + totalCount + ", Matched=" + matchedCandidateNames.size() + ", Marked=" + markedCandidateNames.size());
            // Detailed table for this Expected CTC range
            printExpectedCTCFilterSummaryTable(
                min,
                max,
                totalCount,
                expectedCTCRowsForThisRange.size(),
                expectedCTCRowsForThisRange
            );
            System.out.println("=".repeat(60) + "\n");
        }
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8b SUMMARY: Expected CTC filter (from Excel) completed.");
        System.out.println("=".repeat(80));
    }
}

