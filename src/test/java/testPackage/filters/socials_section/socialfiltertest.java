package testPackage.filters.socials_section;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.basePage;
import Utils.ScrollHelper;
import Utils.socials_Util.socials_filter_Util;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;
import testPackage.helpers.LoginHelper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Feature("Advanced Filters - Socials")
public class socialfiltertest extends basePage {

    private static final int PAGE_SIZE = 25;

    private static class SocialRow {
        final String candidateName;
        final String filterValue;
        final boolean present;
        final String socialValue; // e.g. LinkedIn URL from list row

        SocialRow(String candidateName, String filterValue, boolean present, String socialValue) {
            this.candidateName = candidateName;
            this.filterValue = filterValue;
            this.present = present;
            this.socialValue = socialValue != null ? socialValue : "";
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
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

    @Test
    @Description("Socials – filter from Excel; capture name, presence and social value (e.g. LinkedIn URL) from candidates list only; no details page; all 25 per page then scroll and next page")
    public void socialsFilter_FromExcel_CaptureFromListOnly() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_socials",
            "socials_filter.xlsx"
        ).toString();

        socials_filter_Util.createTemplateIfMissing(excelPath);

        List<String> socials = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                socials = socials_filter_Util.readSocials(in);
            }
        }
        if (socials.isEmpty()) {
            System.out.println("[SOCIALS] No rows in socials_filter.xlsx. Fill Social column first.");
            return;
        }

        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        int baselineTotal = candidatesPage.getResultCount();
        System.out.println("[SOCIALS] BASELINE: Total profiles without socials filter = " + baselineTotal);

        for (String filterValue : socials) {
            if (filterValue == null || filterValue.trim().isEmpty()) continue;
            String filterLower = filterValue.toLowerCase();

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setSocialsFilter(filterValue);

            Thread.sleep(1500);
            int totalCount = candidatesPage.getResultCount();
            if (totalCount <= 0) {
                System.out.println("[SOCIALS] No results for '" + filterValue + "'. Skipping.");
                continue;
            }

            int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            System.out.println("[SOCIALS] FILTER='" + filterValue + "' | Total profiles=" + totalCount
                + " | Page size=" + PAGE_SIZE + " | Total pages=" + totalPages);

            List<SocialRow> rows = new ArrayList<>();

            // LINKEDIN & WHATSAPP: read directly from list for ALL candidates per page (no details page)
            if (filterLower.contains("linkedin") || filterLower.contains("whatsapp") || filterLower.contains("whats")) {
                System.out.println("\n" + "=".repeat(90));
                System.out.printf("%-8s | %-10s | %-30s | %-8s | %-30s%n",
                        "Page No", "Cand No", "Full Name", "Present", "Social Profile");
                System.out.println("-".repeat(90));

                for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                    if (pageNum > 1) {
                        candidatesPage.selectPage(pageNum);
                        Thread.sleep(600);
                    }

                    scrollListToTop(candidatesPage);

                    List<WebElement> nameLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                    if (nameLinks.isEmpty()) continue;

                    int candidateNumberOnPage = 1;
                    for (WebElement nameLink : nameLinks) {
                        String name = nameLink.getText();
                        if (name == null) name = "";
                        WebElement row = candidatesPage.getRowFromNameLink(nameLink);
                        String socialUrl = row != null ? candidatesPage.getSocialUrlInRow(row, filterValue) : "";
                        boolean present = !socialUrl.isEmpty();

                        rows.add(new SocialRow(name, filterValue, present, socialUrl));

                        System.out.printf("%-8d | %-10d | %-30s | %-8s | %-30s%n",
                                pageNum,
                                candidateNumberOnPage,
                                truncate(name, 30),
                                present ? "YES" : "NO",
                                present ? truncate(socialUrl, 30) : "-");

                        Assert.assertTrue(
                            present,
                            "Expected social [" + filterValue + "] to be present on list row for candidate [" + name + "] (Page " + pageNum + ")"
                        );

                        candidateNumberOnPage++;
                    }

                    if (pageNum < totalPages) {
                        scrollListToBottom(candidatesPage);
                    }
                }

                System.out.println("=".repeat(90));
                System.out.println("SOCIALS FILTER: " + filterValue + " | Total results: " + totalCount + " | Captured: " + rows.size());
                System.out.println("=".repeat(90) + "\n");
                continue; // done for this filter
            }

            // OTHER SOCIALS (Github, Dribbble, WhatsApp): take 5 samples per page, open details page
            String parentHandle = driver.getWindowHandle();
            Random rnd = new Random();

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                if (pageNum > 1) {
                    candidatesPage.selectPage(pageNum);
                    Thread.sleep(600);
                }

                scrollListToTop(candidatesPage);

                List<WebElement> nameLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (nameLinks.isEmpty()) continue;

                // pick up to 5 random candidates on this page
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < nameLinks.size(); i++) indices.add(i);
                Collections.shuffle(indices, rnd);
                int sampleCount = Math.min(5, indices.size());

                System.out.println("\n" + "-".repeat(80));
                System.out.println("Page " + pageNum + " – Samples for social: " + filterValue);
                System.out.printf("%-8s | %-10s | %-30s | %-8s | %-30s%n",
                        "Page No", "Cand No", "Full Name", "Present", "Profile URL");
                System.out.println("-".repeat(80));

                for (int s = 0; s < sampleCount; s++) {
                    int idxSample = indices.get(s);
                    List<WebElement> freshLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                    if (idxSample >= freshLinks.size()) continue;

                    WebElement nameLink = freshLinks.get(idxSample);
                    String listName = nameLink.getText();
                    if (listName == null) listName = "";

                    candidatesPage.openCandidateProfile(nameLink);
                    Thread.sleep(1500);

                    for (String h : driver.getWindowHandles()) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }

                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    Thread.sleep(1000);
                    boolean present = detailsPage.hasSocial(filterValue);
                    String currentUrl = driver.getCurrentUrl();

                    rows.add(new SocialRow(candidateName, filterValue, present, currentUrl));

                    System.out.printf("%-8d | %-10d | %-30s | %-8s | %-30s%n",
                            pageNum,
                            (idxSample + 1),
                            truncate(candidateName, 30),
                            present ? "YES" : "NO",
                            truncate(currentUrl, 30));

                    Assert.assertTrue(
                        present,
                        "Expected social [" + filterValue + "] to be present on details page for candidate [" + candidateName + "] (Page " + pageNum + ")"
                    );

                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                }

                if (pageNum < totalPages) {
                    scrollListToBottom(candidatesPage);
                }
            }

            System.out.println("\n" + "=".repeat(90));
            System.out.println("SOCIALS FILTER: " + filterValue + " | Total results: " + totalCount + " | Sampled: " + rows.size());
            System.out.println("=".repeat(90) + "\n");
        }
    }
}
