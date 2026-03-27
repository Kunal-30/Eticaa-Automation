package testPackage.filters.BaseFilters.noticeperiod;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Feature("Advanced Filters - Notice Period")
public class NoticePeriodFilterTest extends basePage {

    private static final int PAGE_SIZE = 25;
    private static final int NOTICE_PERIOD_SAMPLES_PER_PAGE = 4;
    private static final List<String> NOTICE_PERIOD_OPTIONS =
        java.util.Arrays.asList("Immediate Joiner", "0 - 15 days", "1 month", "2 months", "3 months");

    // copy these helper methods exactly from superadmintest:

    private void forEachPageSampleCandidatesFixedCount(
            CandidatesPage candidatesPage, int totalCount, String parentHandle,
            int perPageCount, BiConsumer<WebElement, Integer> processOne) throws InterruptedException {

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages <= 0) return;
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            System.out.println("[NOTICE PERIOD] Opening page " + pageNum + " of " + totalPages + " (assert: sampling from page " + pageNum + ").");
            if (pageNum > 1) {
                candidatesPage.scrollToBottom();
                Thread.sleep(400);
                candidatesPage.selectPage(pageNum);
                Thread.sleep(800);
                candidatesPage.scrollToTop();
                Thread.sleep(400);
            }
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) continue;
            int toOpen = Math.min(perPageCount, links.size());
            System.out.println("[NOTICE PERIOD] Page " + pageNum + ": sampling " + toOpen + " random candidates of " + links.size() + ".");
            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            java.util.Collections.shuffle(indices);
            for (int j = 0; j < toOpen; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), pageNum);
                candidatesPage.scrollToTop();
                Thread.sleep(300);
            }
        }
    }

    private void sampleCandidatesForFilterFixedPerPage(
            CandidatesPage candidatesPage, int totalCount, String parentHandle,
            int perPageCount, BiConsumer<WebElement, Integer> processOne) throws InterruptedException {

        if (totalCount <= 0) return;
        if (totalCount > PAGE_SIZE) {
            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            System.out.println("[NOTICE PERIOD] Results = " + totalCount + " -> " + totalPages + " pages (25 per page). Sampling " + perPageCount + " per page.");
            forEachPageSampleCandidatesFixedCount(candidatesPage, totalCount, parentHandle, perPageCount, processOne);
        } else {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) return;
            int toOpen = Math.min(perPageCount, links.size());
            System.out.println("[NOTICE PERIOD] Results = " + totalCount + " (single page). Opening " + toOpen + " of " + links.size() + " candidates.");
            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            java.util.Collections.shuffle(indices);
            for (int j = 0; j < toOpen; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), 1);
            }
        }
    }

    private boolean noticePeriodMatchesFilter(String filterValue, String valueFoundOnDetails) {
        if (valueFoundOnDetails == null || valueFoundOnDetails.trim().isEmpty()) return false;
        String f = filterValue == null ? "" : filterValue.trim().toLowerCase().replaceAll("\\s+", " ");
        String v = valueFoundOnDetails.trim().toLowerCase().replaceAll("\\s+", " ");
        if (f.isEmpty()) return true;
        if (v.contains(f) || f.contains(v)) return true;
        if (f.contains("immediate") && (v.contains("immediate") || v.contains("0 day"))) return true;
        if (f.contains("0 - 15") && (v.contains("15") || v.contains("0") || v.contains("immediate"))) return true;
        if (f.contains("1 month") && (v.contains("1 month") || v.contains("30"))) return true;
        if (f.contains("2 month") && (v.contains("2 month") || v.contains("60"))) return true;
        if (f.contains("3 month") && (v.contains("3 month") || v.contains("90"))) return true;
        return false;
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (NoticePeriodFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (NoticePeriodFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Notice Period filter only: select value, open all pages (25 per page), 4 random candidates per page, log candidate names from details page")
    public void noticePeriodFilter_OpenAllPages_FourPerPage_LogCandidateNames() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("NOTICE PERIOD FILTER: Open all pages, 4 candidates per page, log names from details");
        System.out.println("=".repeat(80));

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (String noticePeriodValue : NOTICE_PERIOD_OPTIONS) {
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            candidatesPage.setNoticePeriodFilter(noticePeriodValue);
            int totalCount = candidatesPage.getResultCount();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("[NOTICE PERIOD] Filter: " + noticePeriodValue + " | Total results: " + totalCount);
            if (totalCount > PAGE_SIZE) {
                int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                System.out.println("[NOTICE PERIOD] Pages: " + totalPages + " (25 per page). Will open 4 candidates per page.");
            }
            System.out.println("=".repeat(60));

            if (totalCount <= 0) {
                System.out.println("[NOTICE PERIOD] No results for '" + noticePeriodValue + "'. Skipping.");
                continue;
            }

            String parentHandle = driver.getWindowHandle();
            List<String> loggedCandidateNames = new ArrayList<>();
            final String filterApplied = noticePeriodValue;

            sampleCandidatesForFilterFixedPerPage(candidatesPage, totalCount, parentHandle, NOTICE_PERIOD_SAMPLES_PER_PAGE, (link, pageNum) -> {
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
                    String valueFoundOnDetails = detailsPage.getNoticePeriod();
                    boolean hasValue = valueFoundOnDetails != null && !valueFoundOnDetails.isEmpty();
                    boolean matches = hasValue ? noticePeriodMatchesFilter(filterApplied, valueFoundOnDetails) : false;

                    loggedCandidateNames.add(candidateName);
                    System.out.println("[NOTICE PERIOD] ---");
                    System.out.println("[NOTICE PERIOD] Filter applied: " + filterApplied);
                    System.out.println("[NOTICE PERIOD] Page number (sampling from): " + pageNum);
                    System.out.println("[NOTICE PERIOD] Candidate name (from details): " + candidateName);
                    System.out.println("[NOTICE PERIOD] Value found on candidate details page: " + (hasValue ? valueFoundOnDetails : "(not found)"));
                    System.out.println("[NOTICE PERIOD] Assert: value within/applies to filter? " + (hasValue ? (matches ? "PASS" : "FAIL") : "SKIP (not found on page)"));
                    if (hasValue) {
                        Assert.assertTrue(matches, "Notice period on details page [" + valueFoundOnDetails + "] should match applied filter [" + filterApplied + "]");
                    }

                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[NOTICE PERIOD] Page " + pageNum + " | Error: " + e.getMessage());
                }
            });

            System.out.println("[NOTICE PERIOD] REPORT for '" + noticePeriodValue + "': Total results=" + totalCount + ", Opened & logged=" + loggedCandidateNames.size());
            System.out.println("[NOTICE PERIOD] Logged candidate names: " + loggedCandidateNames);
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("NOTICE PERIOD FILTER TEST SUMMARY: Completed for all notice period options.");
        System.out.println("=".repeat(80));
    }
}