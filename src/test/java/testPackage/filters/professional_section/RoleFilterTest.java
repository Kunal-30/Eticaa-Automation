package testPackage.filters.professional_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.ScrollHelper;
import Utils.SSHTunnelManager;
import Utils.professional_details_Util.role_filter_Util;
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

@Feature("Advanced Filters - Professional Details - Role")
public class RoleFilterTest extends basePage {

    private static final int SAMPLES_PER_PAGE = 4;     // open up to 4 samples per page
    private static final int ROWS_PER_PAGE = 25;       // rows shown per page in UI
    private static final int MAX_FILTERS_PER_RUN = 5;  // test up to 5 random role values per run

    private static class RoleRow {
        final String candidateName;
        final String filterValue;
        final String valueOnDetails;
        final boolean match;

        RoleRow(String candidateName, String filterValue, String valueOnDetails, boolean match) {
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

    private static boolean roleMatches(String filterValue, String valueOnDetails) {
        if (valueOnDetails == null || valueOnDetails.trim().isEmpty()) return false;
        String f = (filterValue == null ? "" : filterValue).trim().toLowerCase();
        String v = valueOnDetails.trim().toLowerCase();
        return v.contains(f) || f.contains(v);
    }

    /** Scroll candidate list to top by scrolling the first name link into view. */
    private void scrollListToTop(CandidatesPage candidatesPage) {
        try {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (!links.isEmpty()) {
                ScrollHelper.scrollIntoView(driver, links.get(0));
                Thread.sleep(400);
            }
        } catch (Exception ignored) { }
    }

    /** Scroll candidate list to bottom by scrolling the last name link into view. */
    private void scrollListToBottom(CandidatesPage candidatesPage) {
        try {
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (!links.isEmpty()) {
                ScrollHelper.scrollIntoView(driver, links.get(links.size() - 1));
                Thread.sleep(600);
            }
        } catch (Exception ignored) { }
    }

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (RoleFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (RoleFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    @Test
    @Description("Professional Details – Role filter from Excel; sample candidates and assert Role on details page")
    public void roleFilter_FromExcel_SampleAndAssert() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_professional_details",
            "filters_role.xlsx"
        ).toString();

        List<String> roles = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                roles = role_filter_Util.readRoles(in);
            }
        }
        if (roles.isEmpty()) {
            System.out.println("[ROLE] No rows in filters_role.xlsx. Fill Role column first.");
            return;
        }

        // Pick a random subset of role values so test does not always use the same fixed order
        roles = role_filter_Util.getRandomRoles(roles, MAX_FILTERS_PER_RUN);
        System.out.println("[ROLE] Roles loaded from Excel (random subset, max " + MAX_FILTERS_PER_RUN + "): " + roles);

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        String parentHandle = driver.getWindowHandle();

        for (String filterValue : roles) {
            if (filterValue == null || filterValue.trim().isEmpty()) continue;

            candidatesPage.setRoleFilter(filterValue);

            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[ROLE] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            List<RoleRow> rows = new ArrayList<>();
            int totalPages = (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            if (totalPages < 1) totalPages = 1;

            for (int page = 1; page <= totalPages; page++) {
                if (page > 1) {
                    candidatesPage.selectPage(page);
                    Thread.sleep(600);
                }

                // Align scroll behavior with Socials test: scroll list to top before sampling
                scrollListToTop(candidatesPage);

                List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
                // Randomize the order so we don't always pick the first rows
                List<WebElement> randomLinks = new ArrayList<>(links);
                Collections.shuffle(randomLinks);
                int toOpen = Math.min(SAMPLES_PER_PAGE, randomLinks.size());
                System.out.println("[ROLE] Filter '" + filterValue + "' - Page " + page +
                    " of " + totalPages + " | opening " + toOpen + " sample profiles (total results: " + totalCount + ")");

                for (int i = 0; i < toOpen; i++) {
                    WebElement link = randomLinks.get(i);
                    try {
                        candidatesPage.openCandidateProfile(link);
                        Thread.sleep(1500);
                        for (String h : driver.getWindowHandles()) {
                            if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                        }
                        CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                        String candidateName = detailsPage.getCandidateName();
                        String valueOnDetails = detailsPage.getRole();
                        boolean match = roleMatches(filterValue, valueOnDetails);
                        rows.add(new RoleRow(
                            candidateName,
                            filterValue,
                            valueOnDetails != null ? valueOnDetails : "(not found)",
                            match
                        ));
                        System.out.println("[ROLE] Candidate '" + candidateName + "' | Filter='" +
                            filterValue + "' | RoleOnDetails='" + valueOnDetails + "' | " +
                            (match ? "MATCH" : "UNMATCHED"));
                        if (valueOnDetails != null && !valueOnDetails.isEmpty()) {
                            Assert.assertTrue(
                                match,
                                "Role on details [" + valueOnDetails + "] should match filter [" + filterValue + "]"
                            );
                        }
                        driver.close();
                        driver.switchTo().window(parentHandle);
                        Thread.sleep(500);
                    } catch (Exception e) {
                        try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                        System.out.println("[ROLE] Error while sampling candidate on page " + page + ": " + e.getMessage());
                    }
                }
                // After finishing samples on this page, scroll to bottom before changing page (except after last page)
                if (page < totalPages) {
                    scrollListToBottom(candidatesPage);
                }
            }

            System.out.println("\n" + "=".repeat(72));
            System.out.println("ROLE FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(72));
            int idx = 1;
            for (RoleRow r : rows) {
                String status = r.match ? "MATCH" : "";
                System.out.printf("#%02d %-28s | Filter=%-28s | Details=%-28s | %s%n",
                    idx++,
                    truncate(r.candidateName, 28),
                    truncate(r.filterValue, 28),
                    truncate(r.valueOnDetails, 28),
                    status);
            }
            System.out.println("=".repeat(72) + "\n");

            // After this filter's sampling is over: clear filters, then reliably click Professional Details -> Role
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            // Re-open Advance Search panel in case Clear Filters closed it
            try {
                candidatesPage.openAdvanceSearch();
                Thread.sleep(500);
            } catch (Exception e) {
                System.out.println("[ROLE] openAdvanceSearch after clear failed (may already be open): " + e.getMessage());
            }
            try {
                org.openqa.selenium.By professionalDetailsTab = org.openqa.selenium.By.xpath("//span[text() = 'Professional Details']");
                org.openqa.selenium.WebElement tab = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(professionalDetailsTab));
                ScrollHelper.scrollIntoView(driver, tab);
                tab.click();
                Thread.sleep(400);
            } catch (Exception e) {
                System.out.println("[ROLE] Professional Details tab click after clear failed: " + e.getMessage());
            }
            try {
                org.openqa.selenium.By roleButton = org.openqa.selenium.By.xpath("//button[contains(text(),'Role') or contains(.,'Role')]");
                org.openqa.selenium.WebElement roleBtn = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(roleButton));
                ScrollHelper.scrollIntoView(driver, roleBtn);
                roleBtn.click();
                Thread.sleep(400);
            } catch (Exception e) {
                System.out.println("[ROLE] Role button click after clear failed: " + e.getMessage());
            }
        }
    }
}

