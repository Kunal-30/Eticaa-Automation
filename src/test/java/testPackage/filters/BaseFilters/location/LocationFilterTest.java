package testPackage.filters.BaseFilters.location;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.FilterRow;
import Utils.location_filter_Util;
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
 * Advanced Filter – Location filter with different modes (Either/Current/Preferred/Both).
 *
 * Logic is copied from superadmintest.advancedFilter_FromExcel_AssertAndMarkCandidates_for_Location_Filter
 * without any functional changes, only relocated into its own test class.
 */
@Feature("Advanced Filters - Location")
public class LocationFilterTest extends basePage {

    /** Default pagination size (candidates per page). */
    private static final int PAGE_SIZE = 25;

    /** Table row for Location filter results. */
    private static class LocationFilterResultRow {
        final String candidateName;
        final String city;
        final String mode;
        final String currentLocation;
        final String preferredLocations;
        final boolean match;

        LocationFilterResultRow(String candidateName, String city, String mode,
                                String currentLocation, String preferredLocations, boolean match) {
            this.candidateName = candidateName;
            this.city = city;
            this.mode = mode;
            this.currentLocation = currentLocation;
            this.preferredLocations = preferredLocations;
            this.match = match;
        }
    }

    /** Helper to truncate long strings so console tables stay aligned. */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /** Print summary table for one Location filter value. */
    private void printLocationFilterSummaryTable(String city,
                                                 String mode,
                                                 int totalCount,
                                                 int sampledCount,
                                                 List<LocationFilterResultRow> rows) {
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 0;
        long matchCount = rows.stream().filter(r -> r.match).count();
        long markedCount = rows.size() - matchCount;

        System.out.println("======================================================================");
        System.out.println("[ADV FILTER][LOCATION] FILTER SUMMARY");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Filter Type      : Location");
        System.out.println("City             : " + city);
        System.out.println("Mode             : " + mode);
        System.out.println("Total Results    : " + totalCount);
        System.out.println("Rows per Page    : " + PAGE_SIZE);
        System.out.println("Total Pages      : " + totalPages);
        System.out.println("Profiles Sampled : " + sampledCount);
        System.out.println("MATCH            : " + matchCount);
        System.out.println("MARKED           : " + markedCount);
        System.out.println("======================================================================");

        String headerFormat = "| %-3s | %-30s | %-12s | %-20s | %-20s | %-8s |%n";
        String rowFormat    = "| %-3d | %-30s | %-12s | %-20s | %-20s | %-8s |%n";

        System.out.printf(headerFormat, "#", "Candidate Name", "Mode", "Current Loc", "Preferred Locs", "Result");
        System.out.println("----------------------------------------------------------------------");

        int index = 1;
        for (LocationFilterResultRow r : rows) {
            String resultText = r.match ? "MATCH" : "MARKED";
            System.out.printf(
                rowFormat,
                index++,
                truncate(r.candidateName, 30),
                truncate(r.mode, 12),
                truncate(r.currentLocation, 20),
                truncate(r.preferredLocations, 20),
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
        System.out.println("[FRAMEWORK] BeforeClass (LocationFilter): Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass (LocationFilter): Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Test No. 5 : Advanced Filter – Location filter with different modes (Either/Current/Preferred/Both)")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Location_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ADVANCED FILTER: Location filter with mode dropdown (Either/Current/Preferred/Both) – random from Excel, assert on details");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String locationMasterPath = Paths.get(projectRoot, "src", "test", "resources", "LocationMaster.xlsx").toString();
        try (InputStream namesIn = getClass().getClassLoader().getResourceAsStream("location_names.txt")) {
            location_filter_Util.createLocationMasterFromTextIfMissing(locationMasterPath, namesIn);
        }
        List<String> allLocations = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("LocationMaster.xlsx")) {
            if (excelIn != null) allLocations = location_filter_Util.readLocationNames(excelIn);
        }
        if (allLocations.isEmpty() && Files.exists(Paths.get(locationMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(locationMasterPath))) {
                allLocations = location_filter_Util.readLocationNames(in);
            }
        }
        if (allLocations.isEmpty()) {
            System.out.println("[ADVANCE FILTER][LOCATION] No locations in LocationMaster. Ensure location_names.txt or LocationMaster.xlsx exists.");
            return;
        }
        List<FilterRow> rows = location_filter_Util.getRandomLocationFilterRows(allLocations, 10);
        System.out.println("[ADVANCE FILTER][LOCATION] Using " + rows.size() + " random location criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue() + " (Mode: " + r.getType() + ")");
        }

        // Use dedicated advanced-filter user (never deleted)
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            if (row.isEmpty()) continue;
            String mode = row.getType();
            String city = row.getFilterValue();

            // Clear previous filters and directly add the new one; Advance Search is already open
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            // For Both, use same city for current and preferred; for others, preferred is null
            boolean isBoth = mode != null && mode.toLowerCase().startsWith("both");
            String preferredCity = isBoth ? city : null;
            candidatesPage.setLocationFilterWithMode(mode, city, preferredCity);

            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][LOCATION] Filter: City = " + city + ", Mode = " + mode + " | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            List<LocationFilterResultRow> locationRowsForThisFilter = new ArrayList<>();
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
                    String currentLocationOnPage = detailsPage.getCurrentLocation();
                    String preferredLocationsOnPage = detailsPage.getPrefLocations();
                    boolean found = detailsPage.isFilterValuePresentInSection("Location", city, mode, "Include", "");
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][LOCATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH | City = " + city);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][LOCATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED | City = " + city);
                    }
                    // Add row for detailed Location summary table
                    locationRowsForThisFilter.add(new LocationFilterResultRow(
                        candidateName,
                        city,
                        mode,
                        currentLocationOnPage,
                        preferredLocationsOnPage,
                        found
                    ));
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][LOCATION] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][LOCATION] REPORT for City=" + city + ", Mode=" + mode
                + ": Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);
            // Detailed table for this Location filter value
            printLocationFilterSummaryTable(
                city,
                mode,
                totalCount,
                locationRowsForThisFilter.size(),
                locationRowsForThisFilter
            );
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ADVANCED FILTER (Location modes from Excel) SUMMARY: Completed.");
        System.out.println("=".repeat(80));
    }
}

