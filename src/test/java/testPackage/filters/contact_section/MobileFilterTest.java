package testPackage.filters.contact_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.contact_details_Util.mobile_filter_Util;
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

@Feature("Advanced Filters - Contact Details - Mobile")
public class MobileFilterTest extends basePage {

    private static final int SAMPLES_PER_FILTER = 4;

    private static class MobileRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        MobileRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean mobileMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).replaceAll("\\s+", "");
        String v = valueOnDetails.replaceAll("\\s+", "");
        return v.endsWith(f) || v.equals(f);
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (MobileFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (MobileFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Contact Details – Mobile filter from Excel; sample candidates and assert Mobile on details page")
    public void mobileFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_contact_details",
            "mobile_filter.xlsx"
        ).toString();

        mobile_filter_Util.createTemplateIfMissing(excelPath);

        List<String> mobiles = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                mobiles = mobile_filter_Util.readMobiles(in);
            }
        }
        if (mobiles.isEmpty()) {
            System.out.println("[MOBILE] No rows in mobile_filter.xlsx. Fill Mobile column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : mobiles) {
            if (filterValue == null || filterValue.trim().isEmpty()) continue;

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.applyContactMobileFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[MOBILE] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<MobileRow> rows = new ArrayList<>();
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
                    String valueOnDetails = detailsPage.getMobile();
                    boolean match = mobileMatches(filterValue, valueOnDetails);
                    rows.add(new MobileRow(
                        candidateName,
                        filterValue,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(match,
                            "Mobile on details [" + valueOnDetails + "] should match filter [" + filterValue + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                    System.out.println("[MOBILE] Error: " + e.getMessage());
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("MOBILE FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (MobileRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-20s | Details=%-20s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 20),
                    truncate(r.valueOnDetails, 20),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}

