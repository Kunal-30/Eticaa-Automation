package testPackage.filters.personal_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.personal_details_Util.age_filter_Util;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
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
import java.util.Set;

@Feature("Advanced Filters - Personal Details - Age")
public class AgeFilterTest extends basePage {

    private static final int PAGE_SIZE = 25;
    private static final int SAMPLES_PER_PAGE = 4;

    private static class AgeRow {
        final String candidateName;
        final String filterRange;
        final String valueOnDetails;
        final boolean inRange;

        AgeRow(String candidateName, String filterRange, String valueOnDetails, boolean inRange) {
            this.candidateName = candidateName;
            this.filterRange = filterRange;
            this.valueOnDetails = valueOnDetails;
            this.inRange = inRange;
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private static boolean ageInRange(int minAge, int maxAge, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(valueOnDetails.trim());
        if (!m.find()) return false;
        try {
            int age = Integer.parseInt(m.group(1));
            return age >= minAge && age <= maxAge;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (AgeFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (AgeFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Personal Details – Age filter from Excel; sample candidates and assert age is in range")
    public void ageFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_personal_details",
            "age_filter.xlsx"
        ).toString();

        age_filter_Util.createTemplateIfMissing(excelPath);

        List<int[]> ranges = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                ranges = age_filter_Util.readAgeRanges(in);
            }
        }
        if (ranges.isEmpty()) {
            System.out.println("[AGE] No rows in age_filter.xlsx. Fill MinAge/MaxAge columns first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (int[] range : ranges) {
            int minAge = range[0];
            int maxAge = range[1];
            String filterRange = minAge + " - " + maxAge;

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setAgeFilter(minAge, maxAge);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[AGE] No results for range " + filterRange + ". Skipping.");
                continue;
            }

            List<AgeRow> rows = new ArrayList<>();

            // Reuse existing sampling helper from Personal Details tests, or do a simple single-page sample:
            List<Pages.CandidatesPage> dummy = null; // replace with your sampling helper if needed

            List<org.openqa.selenium.WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            int toOpen = Math.min(SAMPLES_PER_PAGE, links.size());
            for (int i = 0; i < toOpen; i++) {
                org.openqa.selenium.WebElement link = links.get(i);
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    for (String h : driver.getWindowHandles()) {
                        if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    String valueOnDetails = detailsPage.getAge();
                    boolean inRange = ageInRange(minAge, maxAge, valueOnDetails);
                    rows.add(new AgeRow(
                        candidateName,
                        filterRange,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        inRange
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(inRange,
                            "Age on details [" + valueOnDetails + "] should be in range [" + filterRange + "]");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[AGE] Error for range " + filterRange + ": " + e.getMessage());
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("AGE FILTER: " + filterRange + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (AgeRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-11s | Details=%-10s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterRange, 11),
                    truncate(r.valueOnDetails, 10),
                    r.inRange ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}