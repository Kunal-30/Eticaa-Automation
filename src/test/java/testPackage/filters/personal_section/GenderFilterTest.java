package testPackage.filters.personal_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.personal_details_Util.gender_filter_Util;
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

@Feature("Advanced Filters - Personal Details - Gender")
public class GenderFilterTest extends basePage {

    private static final int PAGE_SIZE = 25;
    private static final int SAMPLES_PER_PAGE = 4;

    private static class GenderFilterResultRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        GenderFilterResultRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean genderMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return v.contains(f) || f.contains(v) || v.equals(f);
    }

    private void forEachPageSampleCandidatesFixedCount(
            CandidatesPage candidatesPage, int totalCount, String parentHandle,
            int perPageCount, BiConsumer<WebElement, Integer> processOne) throws InterruptedException {

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages <= 0) return;
        Random rnd = new Random();
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            System.out.println("[GENDER] Page " + pageNum + " of " + totalPages + ".");
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
            System.out.println("[GENDER] Results = " + totalCount + " -> " + totalPages + " pages. Sampling " + perPageCount + " per page.");
            forEachPageSampleCandidatesFixedCount(candidatesPage, totalCount, parentHandle, perPageCount, processOne);
        } else {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) return;
            int toOpen = Math.min(perPageCount, links.size());
            System.out.println("[GENDER] Results = " + totalCount + " (single page). Opening " + toOpen + " of " + links.size() + ".");
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
        System.out.println("\n[FRAMEWORK] BeforeClass (GenderFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (GenderFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Personal Details – Gender filter from Excel: Female/Male/Other; sample candidates and assert value on details page")
    public void genderFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_personal_details",
            "gender_filter.xlsx"
        ).toString();

        gender_filter_Util.createTemplateIfMissing(excelPath);

        List<String> genders = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                genders = gender_filter_Util.readGenders(in);
            }
        }
        if (genders.isEmpty()) {
            System.out.println("[GENDER] No rows in gender_filter.xlsx. Fill Gender column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : genders) {
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setGenderFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[GENDER] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<GenderFilterResultRow> rows = new ArrayList<>();
            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, SAMPLES_PER_PAGE, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    for (String h : driver.getWindowHandles()) {
                        if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    String valueOnDetails = detailsPage.getGender();
                    boolean match = genderMatches(filterValue, valueOnDetails);
                    rows.add(new GenderFilterResultRow(
                        candidateName,
                        filterValue,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(match,
                            "Gender on details [" + valueOnDetails + "] should match filter [" + filterValue + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[GENDER] Error page " + pageNum + ": " + e.getMessage());
                }
            });

            // Simple summary log
            System.out.println("\n" + "=".repeat(72));
            System.out.println("GENDER FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=" .repeat(72));
            int idx = 1;
            for (GenderFilterResultRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-10s | Details=%-15s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 10),
                    truncate(r.valueOnDetails, 15),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=" .repeat(72) + "\n");
        }
    }
}