package testPackage.filters.contact_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import Utils.contact_details_Util.full_name_filter_Util;
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
import java.util.Random;

@Feature("Advanced Filters - Contact Details - Full Name")
public class FullNameFilterTest extends basePage {

    private static final int SAMPLES_PER_FILTER = 4;
    private static final Random RANDOM = new Random();

    private static class FullNameRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        FullNameRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean fullNameMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return v.contains(f) || f.contains(v);
    }

    /**
     * From a full name string, pick one token (first/middle/last) of length ≥ 2
     * to use for searching. Returns null when no suitable token is found.
     */
    private static String pickNameToken(String fullName) {
        if (fullName == null) return null;
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) return null;
        String[] parts = trimmed.split("\\s+");
        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.length() >= 2) {
                validIndices.add(i);
            }
        }
        if (validIndices.isEmpty()) {
            return null;
        }
        int idx = validIndices.get(RANDOM.nextInt(validIndices.size()));
        return parts[idx];
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (FullNameFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (FullNameFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Contact Details – Full Name filter from Excel; sample candidates and assert Full Name on details page")
    public void fullNameFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_contact_details",
            "full_name_filter.xlsx"
        ).toString();

        full_name_filter_Util.createTemplateIfMissing(excelPath);

        List<String> names = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                names = full_name_filter_Util.readFullNames(in);
            }
        }
        if (names.isEmpty()) {
            System.out.println("[FULL NAME] No rows in full_name_filter.xlsx. Fill FullName column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String fullNameFromExcel : names) {
            if (fullNameFromExcel == null || fullNameFromExcel.trim().isEmpty()) continue;

            String token = pickNameToken(fullNameFromExcel);
            if (token == null) {
                System.out.println("[FULL NAME] Skipping '" + fullNameFromExcel + "' because no token with length >= 2 was found.");
                continue;
            }

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.applyContactFullNameFilter(token);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[FULL NAME] No results for token '" + token + "' (from '" + fullNameFromExcel + "'). Skipping.");
                continue;
            }

            List<FullNameRow> rows = new ArrayList<>();
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
                    String valueOnDetails = detailsPage.getCandidateName();
                    boolean match = fullNameMatches(token, valueOnDetails);
                    rows.add(new FullNameRow(
                        candidateName,
                        token,
                        valueOnDetails != null ? valueOnDetails : "(not found)",
                        match
                    ));
                    if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                        Assert.assertTrue(
                            match,
                            "Candidate name [" + valueOnDetails + "] should contain or match searched token [" + token + "]"
                        );
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                    System.out.println("[FULL NAME] Error: " + e.getMessage());
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("FULL NAME FILTER (token): " + token + " (from '" + fullNameFromExcel + "') | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (FullNameRow r : rows) {
                System.out.printf("#%02d %-28s | Filter=%-24s | Details=%-24s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 24),
                    truncate(r.valueOnDetails, 24),
                    r.match ? "PASS" : "FAIL");
            }
            System.out.println("=".repeat(72) + "\n");
        }
    }
}

