package testPackage.filters.professional_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.professional_details_Util.industry_filter_Util;
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
import java.util.List;

@Feature("Advanced Filters - Professional Details - Industry")
public class IndustryFilterTest extends basePage {

    private static final int SAMPLES_PER_FILTER = 4;

    private static class IndustryRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        IndustryRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean industryMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return v.contains(f) || f.contains(v);
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (IndustryFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (IndustryFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Professional Details – Industry filter from Excel; sample candidates and assert Industry on details page")
    public void industryFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_professional_details",
            "industry_filter.xlsx"
        ).toString();

        industry_filter_Util.createTemplateIfMissing(excelPath);

        List<String> industries = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                industries = industry_filter_Util.readIndustries(in);
            }
        }
        if (industries.isEmpty()) {
            System.out.println("[INDUSTRY] No rows in industry_filter.xlsx. Fill Industry column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : industries) {
            if (filterValue == null || filterValue.trim().isEmpty()) continue;

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setIndustryFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[INDUSTRY] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<IndustryRow> rows = new ArrayList<>();
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            int toOpen = Math.min(SAMPLES_PER_FILTER, links.size());
            for (int i = 0; i < toOpen; i++) {
                WebElement link = links.get(i);
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    for (String h : driver.getWindowHandles()) {
                        if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    String valueOnDetails = detailsPage.getIndustry();
                    boolean match = industryMatches(filterValue, valueOnDetails);
                    rows.add(new IndustryRow(
                        candidateName,
                        filterValue,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(match,
                            "Industry on details [" + valueOnDetails + "] should match filter [" + filterValue + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                    System.out.println("[INDUSTRY] Error: " + e.getMessage());
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("INDUSTRY FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (IndustryRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-28s | Details=%-28s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 28),
                    truncate(r.valueOnDetails, 28),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}

