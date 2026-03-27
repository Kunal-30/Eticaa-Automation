package testPackage.filters.personal_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.personal_details_Util.marital_status_filter_Util;
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

@Feature("Advanced Filters - Personal Details - Marital Status")
public class MaritalStatusFilterTest extends basePage {

    private static final int PAGE_SIZE = 25;
    private static final int SAMPLES_PER_PAGE = 4;

    private static class MaritalStatusRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        MaritalStatusRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
            this.candidateName = candidateName;
            this.filterValue = filterValue;
            this.valueOnDetails = valueOnDetails;
            this.match = match;
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private static boolean maritalStatusMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue)
            .trim().toLowerCase().replace("/", " ").replaceAll("\\s+", " ");
        String v = valueOnDetails.trim().toLowerCase().replaceAll("\\s+", " ");
        if (f.contains("single") && (v.contains("single") || v.contains("unmarried"))) return true;
        if (f.contains("unmarried") && (v.contains("single") || v.contains("unmarried"))) return true;
        return v.contains(f) || f.contains(v);
    }

    private void forEachPageSampleCandidatesFixedCount(
            CandidatesPage candidatesPage, int totalCount, String parentHandle,
            int perPageCount, BiConsumer<WebElement, Integer> processOne) throws InterruptedException {

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages <= 0) return;
        Random rnd = new Random();
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            System.out.println("[MARITAL] Page " + pageNum + " of " + totalPages + ".");
            if (pageNum > 1) {
                candidatesPage.selectPage(pageNum);
                Thread.sleep(600);
            }
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) continue;
            int toOpen = Math.min(perPageCount, links.size());
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int j = 0; j < toOpen; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), pageNum);
            }
        }
    }

    private void sampleCandidatesForFilter(
            CandidatesPage candidatesPage, int totalCount, String parentHandle,
            int perPageCount, BiConsumer<WebElement, Integer> processOne) throws InterruptedException {

        if (totalCount <= 0) return;
        if (totalCount > PAGE_SIZE) {
            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            System.out.println("[MARITAL] Results = " + totalCount + " -> " + totalPages + " pages. Sampling " + perPageCount + " per page.");
            forEachPageSampleCandidatesFixedCount(candidatesPage, totalCount, parentHandle, perPageCount, processOne);
        } else {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) return;
            int toOpen = Math.min(perPageCount, links.size());
            System.out.println("[MARITAL] Results = " + totalCount + " (single page). Opening " + toOpen + " of " + links.size() + ".");
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
        System.out.println("\n[FRAMEWORK] BeforeClass (MaritalStatusFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (MaritalStatusFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Personal Details – Marital Status filter from Excel; sample candidates and assert value on details page")
    public void maritalStatusFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_personal_details",
            "marital_status_filter.xlsx"
        ).toString();

        marital_status_filter_Util.createTemplateIfMissing(excelPath);

        List<String> statuses = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                statuses = marital_status_filter_Util.readMaritalStatuses(in);
            }
        }
        if (statuses.isEmpty()) {
            System.out.println("[MARITAL] No rows in marital_status_filter.xlsx. Fill MaritalStatus column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : statuses) {
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setMaritalStatusFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[MARITAL] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<MaritalStatusRow> rows = new ArrayList<>();
            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, SAMPLES_PER_PAGE, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    for (String h : driver.getWindowHandles()) {
                        if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    String valueOnDetails = detailsPage.getMaritalStatus();
                    boolean match = maritalStatusMatches(filterValue, valueOnDetails);
                    rows.add(new MaritalStatusRow(
                        candidateName,
                        filterValue,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(match,
                            "Marital Status on details [" + valueOnDetails + "] should match filter [" + filterValue + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[MARITAL] Error page " + pageNum + ": " + e.getMessage());
                }
            });

            System.out.println("\n" + "=".repeat(72));
            System.out.println("MARITAL STATUS FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (MaritalStatusRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-18s | Details=%-18s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 18),
                    truncate(r.valueOnDetails, 18),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}