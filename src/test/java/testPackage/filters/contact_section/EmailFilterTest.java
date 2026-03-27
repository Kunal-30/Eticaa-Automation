package testPackage.filters.contact_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.contact_details_Util.email_filter_Util;
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

@Feature("Advanced Filters - Contact Details - Email")
public class EmailFilterTest extends basePage {

    private static final int SAMPLES_PER_FILTER = 4;

    private static class EmailRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        EmailRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean emailMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return v.equals(f);
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (EmailFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (EmailFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Contact Details – Email filter from Excel; sample candidates and assert Email on details page")
    public void emailFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_contact_details",
            "email_filter.xlsx"
        ).toString();

        email_filter_Util.createTemplateIfMissing(excelPath);

        List<String> emails = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                emails = email_filter_Util.readEmails(in);
            }
        }
        if (emails.isEmpty()) {
            System.out.println("[EMAIL] No rows in email_filter.xlsx. Fill Email column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : emails) {
            if (filterValue == null || filterValue.trim().isEmpty()) continue;

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.applyContactEmailFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[EMAIL] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<EmailRow> rows = new ArrayList<>();
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
                    String valueOnDetails = detailsPage.getEmail();
                    boolean match = emailMatches(filterValue, valueOnDetails);
                    rows.add(new EmailRow(
                        candidateName,
                        filterValue,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(match,
                            "Email on details [" + valueOnDetails + "] should match filter [" + filterValue + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                    System.out.println("[EMAIL] Error: " + e.getMessage());
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("EMAIL FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (EmailRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-30s | Details=%-30s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 30),
                    truncate(r.valueOnDetails, 30),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}

