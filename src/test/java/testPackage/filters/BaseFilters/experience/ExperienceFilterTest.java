package testPackage.filters.BaseFilters.experience;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.experience_filter_Util;
import Utils.SSHTunnelManager;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
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
 * Advanced Filter – Experience (min/max years) ranges from ExperienceMaster.xlsx.
 *
 * Logic is copied from superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_Experience_Filter
 * without functional changes, only relocated into its own test class.
 */
@Feature("Advanced Filters - Experience")
public class ExperienceFilterTest extends basePage {

    /** Default pagination size (candidates per page). */
    private static final int PAGE_SIZE = 25;

    /** Table row for Experience filter results. */
    private static class ExperienceFilterResultRow {
        final String candidateName;
        final int minYears;
        final int maxYears;
        final String experienceText;
        final Double experienceYears;
        final boolean inRange;

        ExperienceFilterResultRow(String candidateName, int minYears, int maxYears,
                                  String experienceText, Double experienceYears, boolean inRange) {
            this.candidateName = candidateName;
            this.minYears = minYears;
            this.maxYears = maxYears;
            this.experienceText = experienceText;
            this.experienceYears = experienceYears;
            this.inRange = inRange;
        }
    }

    /** Helper to truncate long strings so console tables stay aligned. */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /** Print summary table for one Experience range. */
    private void printExperienceFilterSummaryTable(int min,
                                                   int max,
                                                   int totalCount,
                                                   int sampledCount,
                                                   List<ExperienceFilterResultRow> rows) {
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
        long inRangeCount = rows.stream().filter(r -> r.inRange).count();
        long outOfRangeCount = rows.size() - inRangeCount;

        System.out.println("======================================================================");
        System.out.println("[ADV FILTER][EXPERIENCE] FILTER SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Filter Type      : Experience");
        System.out.println("Range (years)    : " + min + " - " + max);
        System.out.println("Total Results    : " + totalCount);
        System.out.println("Rows per Page    : " + PAGE_SIZE);
        System.out.println("Total Pages      : " + totalPages);
        System.out.println("Profiles Sampled : " + sampledCount);
        System.out.println("IN RANGE         : " + inRangeCount);
        System.out.println("OUT OF RANGE     : " + outOfRangeCount);
        System.out.println("======================================================================");

        String headerFormat = "| %-3s | %-30s | %-15s | %-10s | %-8s |%n";
        String rowFormat    = "| %-3d | %-30s | %-15s | %-10s | %-8s |%n";

        System.out.printf(headerFormat, "#", "Candidate Name", "Exp Text", "Exp Years", "Result");
        System.out.println("----------------------------------------------------------------------");

        int index = 1;
        for (ExperienceFilterResultRow r : rows) {
            String resultText = r.inRange ? "IN-RANGE" : "OUT";
            System.out.printf(
                rowFormat,
                index++,
                truncate(r.candidateName, 30),
                truncate(r.experienceText, 15),
                r.experienceYears != null ? r.experienceYears : "-",
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
        System.out.println("[FRAMEWORK] BeforeClass (ExperienceFilter): Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass (ExperienceFilter): Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Test No 7: Advanced Filter – Experience ranges (min/max years) from Excel, log counts")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Experience_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 7: Advanced Filter – Experience ranges (min/max years) from ExperienceMaster.xlsx");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String experienceMasterPath = Paths.get(projectRoot, "src", "test", "resources", "ExperienceMaster.xlsx").toString();
        experience_filter_Util.createEmptyTemplateIfMissing(experienceMasterPath);

        List<int[]> allRanges = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("ExperienceMaster.xlsx")) {
            if (excelIn != null) allRanges = experience_filter_Util.readExperienceRanges(excelIn);
        }
        if (allRanges.isEmpty() && Files.exists(Paths.get(experienceMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(experienceMasterPath))) {
                allRanges = experience_filter_Util.readExperienceRanges(in);
            }
        }
        if (allRanges.isEmpty()) {
            System.out.println("[ADVANCE FILTER][EXPERIENCE] No valid ranges in ExperienceMaster (empty rows skipped). Ensure MinYears, MaxYears are filled.");
            return;
        }
        List<int[]> ranges = experience_filter_Util.getRandomExperienceRanges(allRanges, 10);
        System.out.println("[ADVANCE FILTER][EXPERIENCE] Using " + ranges.size() + " experience range(s) from Excel (empty cells skipped):");
        for (int[] r : ranges) {
            System.out.println("  - " + r[0] + " to " + r[1] + " years");
        }

        // Use dedicated advanced-filter user (never deleted)
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            System.out.println("\n" + "=".repeat(60));
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setExperienceFilter(min, max);
            int totalCount = candidatesPage.getResultCount();
            System.out.println("[ADVANCE FILTER][EXPERIENCE] Filter: " + min + " to " + max + " years (from Excel) | Total results: " + totalCount);
            System.out.println("=".repeat(60));
            Assert.assertTrue(totalCount >= 0, "Result count should be displayed (>= 0) for Experience " + min + "–" + max + " years");
            String parentHandle = driver.getWindowHandle();
            List<String> openedCandidateNames = new ArrayList<>();
            List<ExperienceFilterResultRow> experienceRowsForThisRange = new ArrayList<>();

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
                    Assert.assertNotNull(candidateName, "Candidate name should be visible on details page");
                    Assert.assertFalse(candidateName.isEmpty(), "Candidate name should not be empty");
                    openedCandidateNames.add(candidateName);
                    String expText = detailsPage.getExperienceText();
                    Double expYears = detailsPage.getExperienceYears();
                    boolean inRange = expYears != null && expYears >= min && expYears <= max;
                    if (expYears != null) {
                        Assert.assertTrue(expYears >= min && expYears <= max,
                            "Experience " + expText + " (" + expYears + " y) should be between " + min + " and " + max + " years for " + candidateName);
                        System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate = " + candidateName + " (from Page " + pageNum + ") | Experience: " + expText + " (" + expYears + " y) in range [" + min + "–" + max + "]");
                    } else {
                        System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate = " + candidateName + " (from Page " + pageNum + ") | Experience text: " + (expText.isEmpty() ? "(not found)" : expText) + " | Filter: " + min + "–" + max + " years");
                    }
                    // Add row for detailed Experience summary table
                    experienceRowsForThisRange.add(new ExperienceFilterResultRow(
                        candidateName,
                        min,
                        max,
                        expText,
                        expYears,
                        inRange
                    ));
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][EXPERIENCE] REPORT for " + min + "–" + max + " years: Total = " + totalCount + ", Opened = " + openedCandidateNames.size() + ". " + openedCandidateNames);
            // Detailed table for this Experience range
            printExperienceFilterSummaryTable(
                min,
                max,
                totalCount,
                experienceRowsForThisRange.size(),
                experienceRowsForThisRange
            );
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 7 SUMMARY: Experience range filters (from Excel) completed. Same profile-open pattern as Location.");
        System.out.println("=".repeat(80));
    }
}

