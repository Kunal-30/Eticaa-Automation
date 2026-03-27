package testPackage.filters.language_section;

import Pages.CandidatesPage;
import Pages.basePage;
import Manager.DatabaseCleanupManager;
import Utils.ScrollHelper;
import Utils.SSHTunnelManager;
import Utils.language_Util.language_filter_Util;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
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

@Feature("Advanced Filters - Languages")
public class LanguageFilterTest extends basePage {

    private static final int MAX_LANG_CARDS = 1;   // for now, create exactly 1 language card per run
    private static final int SAMPLES_PER_PAGE = 4; // open up to 4 candidates per page, per result page
    private static final int ROWS_PER_PAGE = 25;   // assumed rows per page (same as other filters)

    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] BeforeClass (LanguageFilter): Starting SSH tunnel");
        SSHTunnelManager.startTunnel();
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] AfterClass (LanguageFilter): Stopping SSH tunnel");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
    public void cleanupDatabase(ITestResult result) {
        DatabaseCleanupManager.runAfterMethodCleanup(result);
    }

    /** Normalize language strings so format differences don't break matching. */
    private static String normalizeLanguage(String src) {
        if (src == null) return "";
        String s = src.trim();
        // If we have "Marathi ( Read,Write,Speak )", keep only before '('
        int idxParen = s.indexOf('(');
        if (idxParen > 0) {
            s = s.substring(0, idxParen).trim();
        }
        // Lowercase and collapse multiple spaces
        s = s.toLowerCase().replaceAll("\\s+", " ");
        return s;
    }

    @Test
    @Description("Languages – manually add language cards (name + optional proficiency + abilities), scroll long filter panel if needed, apply, and sample candidates")
    public void languageFilter_ManualCards_SampleCandidates() throws Exception {
        LoginHelper.loginAsAdvancedFilterUser(driver, wait);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();
        Thread.sleep(500);

        // 1) Load languages from Excel once for this test
        String projectRoot = System.getProperty("user.dir");
        String excelPath = Paths.get(
            projectRoot,
            "src", "test", "resources",
            "filters_languages",
            "language_filter.xlsx"
        ).toString();

        List<String> languages = new ArrayList<>();
        if (Files.exists(Paths.get(excelPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(excelPath))) {
                languages = language_filter_Util.readLanguages(in);
            }
        }
        if (languages.isEmpty()) {
            System.out.println("[LANGUAGE] No rows in language_filter.xlsx. Fill Language column first.");
            return;
        }

        // Randomly select up to 3 distinct languages to run in a single test execution
        Random rnd = new Random();
        List<String> languagesToRun = language_filter_Util.getRandomLanguages(languages, 3);

        for (String primaryLanguage : languagesToRun) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[LANGUAGE] Starting run for language: " + primaryLanguage);
            System.out.println("=".repeat(80));

            // Ensure clean state before each language run
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.openAdvanceSearch();
            Thread.sleep(500);

            // 2) Click on "Language Details" tab/section header
            try {
                By languageDetailsTab = By.xpath("//span[text() = 'Language Details']");
                WebElement tab = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(languageDetailsTab));
                ScrollHelper.scrollIntoViewAtTop(driver, tab);
                tab.click();
                Thread.sleep(600);
            } catch (Exception e) {
                System.out.println("[LANGUAGE] Language Details tab not found/clickable: " + e.getMessage());
                return;
            }

            // 3) Directly use the Search Language input (no extra button click needed now)

            // 4) Create ONE language card using primaryLanguage
            for (int i = 0; i < MAX_LANG_CARDS; i++) {
                String lang = primaryLanguage;

            try {
                By searchInputBy = By.xpath(
                    "//input[@placeholder='Search Language' or contains(@placeholder,'Search Language') or contains(@aria-label,'Search Language')]"
                );
                WebElement input = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(searchInputBy));
                ScrollHelper.scrollIntoView(driver, input);
                input.clear();
                input.sendKeys(lang);
                Thread.sleep(400);
                input.sendKeys(Keys.ENTER);
                Thread.sleep(700);
                System.out.println("[LANGUAGE] Added language card: " + lang);
            } catch (Exception e) {
                System.out.println("[LANGUAGE] Search Language input/card creation failed for '" + lang + "': " + e.getMessage());
            }

            // 4) Optionally set proficiency on the latest card
            try {
                By lastCardProficiency = By.xpath(
                    "(//div[contains(@class,'language-card') or .//p[contains(normalize-space(.),'Language')]])" +
                    "[last()]//button[contains(@id,'proficiency') or contains(normalize-space(.),'Proficiency') or contains(normalize-space(.),'None')]"
                );
                List<WebElement> profTriggers = driver.findElements(lastCardProficiency);
                if (!profTriggers.isEmpty()) {
                    WebElement pTrig = profTriggers.get(0);
                    ScrollHelper.scrollIntoView(driver, pTrig);
                    pTrig.click();
                    Thread.sleep(400);

                    // Choose one random proficiency: None / Beginner / Proficient / Expert
                    String[] profs = {"None", "Beginner", "Proficient", "Expert"};
                    String prof = profs[rnd.nextInt(profs.length)];
                    By profOption = By.xpath("//*[contains(@role,'option') or self::li][normalize-space(text())='" + prof + "']");
                    List<WebElement> profOpts = driver.findElements(profOption);
                    if (!profOpts.isEmpty()) {
                        WebElement opt = profOpts.get(0);
                        ScrollHelper.scrollIntoView(driver, opt);
                        opt.click();
                        Thread.sleep(400);
                        System.out.println("[LANGUAGE] Set proficiency '" + prof + "' for language " + lang);
                    }
                }
            } catch (Exception e) {
                System.out.println("[LANGUAGE] Proficiency selection failed for language card '" + lang + "': " + e.getMessage());
            }

            // 5) Optionally select one or more abilities (Read / Write / Speak) on the latest card
            try {
                String[] abilities = {"Read", "Write", "Speak"};
                int abilitiesToSelect = 1 + rnd.nextInt(abilities.length); // 1–3
                for (int j = 0; j < abilitiesToSelect; j++) {
                    String ability = abilities[j];
                    By abilityCheckbox = By.xpath(
                        "(//div[contains(@class,'language-card') or .//p[contains(normalize-space(.),'Language')]])" +
                        "[last()]//label[.//span[normalize-space(text())='" + ability + "']]//input[@type='checkbox']"
                    );
                    List<WebElement> boxes = driver.findElements(abilityCheckbox);
                    if (!boxes.isEmpty()) {
                        WebElement box = boxes.get(0);
                        ScrollHelper.scrollIntoView(driver, box);
                        if (!box.isSelected()) {
                            box.click();
                            Thread.sleep(200);
                            System.out.println("[LANGUAGE] Selected ability '" + ability + "' for language " + lang);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[LANGUAGE] Ability selection failed for language card '" + lang + "': " + e.getMessage());
            }

            // If many language cards exist, the panel may need scrolling; scroll down a bit after each card
            try {
                org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                js.executeScript("arguments[0].scrollTop = arguments[0].scrollTop + 200;",
                    driver.findElement(By.xpath("//div[contains(@class,'overflow-y-auto') or contains(@class,'scroll')]")));
                Thread.sleep(400);
            } catch (Exception ignored) { }
            }

            // 5) Click Apply Filter to apply all language cards
        try {
            By applyBtn = By.xpath("//button[contains(normalize-space(.),'Apply Filter') or contains(normalize-space(.),'Apply filter')]");
            WebElement apply = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(applyBtn));
            ScrollHelper.scrollIntoView(driver, apply);
            apply.click();
            candidatesPage.waitForLoaderGone();
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("[LANGUAGE] Apply Filter button not found/clickable: " + e.getMessage());
            }

            // 6) After applying, some UIs show language tags with proficiency + abilities; we just log them if present
        try {
            List<WebElement> tags = driver.findElements(By.xpath(
                "//div[contains(@class,'tag') or contains(@class,'chip') or contains(@class,'badge')]" +
                "[.//span[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'language')]]"
            ));
            if (!tags.isEmpty()) {
                System.out.println("[LANGUAGE] Applied language filter tags:");
                for (WebElement tag : tags) {
                    ScrollHelper.scrollIntoView(driver, tag);
                    System.out.println("  TAG: " + tag.getText());
                }
            }
        } catch (Exception e) {
            System.out.println("[LANGUAGE] Logging language tags failed: " + e.getMessage());
            }

            // 7) Sample a few candidates from the results (across pages) and log their Languages known section,
            //    including which rows match the searched primary language.
        try {
            int totalCount = candidatesPage.getResultCount();
            System.out.println("[LANGUAGE] Total candidates after language filter: " + totalCount);
            int totalPages = (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            if (totalPages < 1) totalPages = 1;

            String parentHandle = driver.getWindowHandle();

            class CandidateLanguageSummary {
                final int page;
                final int indexOnPage;
                final String candidateName;
                final String url;
                final String languagesPresent;
                final boolean anyMatch;
                final String matchedValues;

                CandidateLanguageSummary(int page, int indexOnPage, String candidateName, String url,
                                         String languagesPresent, boolean anyMatch, String matchedValues) {
                    this.page = page;
                    this.indexOnPage = indexOnPage;
                    this.candidateName = candidateName;
                    this.url = url;
                    this.languagesPresent = languagesPresent;
                    this.anyMatch = anyMatch;
                    this.matchedValues = matchedValues;
                }
            }

            java.util.List<CandidateLanguageSummary> summaries = new java.util.ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {
                if (page > 1) {
                    // After sampling a page, scroll to bottom then change page (pattern from Socials filter tests)
                    try {
                        candidatesPage.scrollToBottom();
                    } catch (Exception ignored) { }
                    candidatesPage.selectPage(page);
                    Thread.sleep(600);
                }

                List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
                // Build a list of indices so both which candidates and their index positions are random
                java.util.List<Integer> indices = new java.util.ArrayList<>();
                for (int idx = 0; idx < links.size(); idx++) {
                    indices.add(idx);
                }
                java.util.Collections.shuffle(indices);
                int toOpen = Math.min(SAMPLES_PER_PAGE, indices.size());

                for (int i = 0; i < toOpen; i++) {
                    int idxOnPage = indices.get(i); // 0-based index in original listing order
                    WebElement link = links.get(idxOnPage);
                    try {
                        String nameOnList = link.getText();
                        candidatesPage.openCandidateProfile(link);
                        Thread.sleep(1500);
                        for (String h : driver.getWindowHandles()) {
                            if (!h.equals(parentHandle)) { driver.switchTo().window(h); break; }
                        }
                        String detailsUrl = driver.getCurrentUrl();
                        System.out.println("[LANGUAGE] Opened candidate from language filter (page " + page + ", index " + (idxOnPage + 1) + "): " + nameOnList +
                            " | Details URL: " + detailsUrl);

                        // Scroll to "Languages known" section and capture all values
                        try {
                            By langSectionBy = By.xpath("//h3[normalize-space(text())='Languages known']/parent::div");
                            WebElement section = driver.findElement(langSectionBy);
                            ScrollHelper.scrollIntoView(driver, section);
                            Thread.sleep(500);

                            List<WebElement> rows = section.findElements(
                                By.xpath(".//div[contains(@class,'text-sm') and contains(@class,'text-gray-700')]")
                            );
                            System.out.println("[LANGUAGE] Languages known on details page for candidate '" + nameOnList + "':");
                            java.util.List<String> allLangs = new java.util.ArrayList<>();
                            java.util.List<String> matchedLangs = new java.util.ArrayList<>();
                            String primaryNorm = normalizeLanguage(primaryLanguage);
                            boolean anyMatch = false;

                            for (WebElement row : rows) {
                                try {
                                    WebElement langSpan = row.findElement(By.xpath(".//span[contains(@class,'font-medium')]"));
                                    String langNameRaw = langSpan.getText() != null ? langSpan.getText().trim() : "";
                                    String langNameNorm = normalizeLanguage(langNameRaw);
                                    allLangs.add(langNameRaw);

                                    List<WebElement> spans = row.findElements(By.xpath(".//span[not(contains(@class,'font-medium'))]"));
                                    String prof = spans.size() > 0 && spans.get(0).getText() != null
                                        ? spans.get(0).getText().trim()
                                        : "";
                                    String abilities = "";
                                    for (WebElement s : spans) {
                                        String t = s.getText();
                                        if (t != null && t.contains("(") && t.contains(")")) {
                                            abilities = t.trim();
                                            break;
                                        }
                                    }
                                    boolean matchesPrimary = !langNameNorm.isEmpty()
                                        && !primaryNorm.isEmpty()
                                        && (langNameNorm.contains(primaryNorm) || primaryNorm.contains(langNameNorm));
                                    if (matchesPrimary) {
                                        anyMatch = true;
                                        matchedLangs.add(langNameRaw);
                                    }
                                    System.out.println("  LANG ROW: language='" + langNameRaw + "', proficiency='" + prof +
                                        "', abilities='" + abilities + "' | " + (matchesPrimary ? "MATCH" : "UNMATCHED"));
                                } catch (Exception rowEx) {
                                    System.out.println("[LANGUAGE] Failed to parse a language row: " + rowEx.getMessage());
                                }
                            }
                        String languagesPresent = String.join(" | ", allLangs);
                        String matchedValues = matchedLangs.isEmpty() ? "-" : String.join(" | ", matchedLangs);
                        summaries.add(new CandidateLanguageSummary(
                            page,
                            idxOnPage + 1,
                            nameOnList,
                            detailsUrl,
                            languagesPresent,
                            anyMatch,
                            matchedValues
                        ));
                        } catch (Exception secEx) {
                            System.out.println("[LANGUAGE] 'Languages known' section not found or not parsable: " + secEx.getMessage());
                        }
                        driver.close();
                        driver.switchTo().window(parentHandle);
                        Thread.sleep(500);
                    } catch (Exception e) {
                        try { driver.switchTo().window(parentHandle); } catch (Exception ignored) { }
                        System.out.println("[LANGUAGE] Error while sampling candidate: " + e.getMessage());
                    }
                }
            }

            // Summary table after sampling
            System.out.println("\n[LANGUAGE] Languages Applied: " + primaryLanguage);
            System.out.println("[LANGUAGE] Total profiles after filter (all pages): " + totalCount);
            System.out.println("[LANGUAGE] Summary table (per sampled candidate):");
            System.out.printf("%-8s | %-8s | %-30s | %-60s | %-40s | %-10s | %-40s%n",
                "Page", "Index", "Candidate Name", "Candidate URL", "Languages Present", "Matched?", "Matched Values");
            System.out.println("=".repeat(220));
            for (CandidateLanguageSummary s : summaries) {
                String matchedFlag = s.anyMatch ? "MATCH" : "UNMATCHED";
                System.out.printf("%-8d | %-8d | %-30s | %-60s | %-40s | %-10s | %-40s%n",
                    s.page,
                    s.indexOnPage,
                    s.candidateName,
                    s.url,
                    s.languagesPresent,
                    matchedFlag,
                    s.matchedValues);
            }
            } catch (Exception e) {
                        System.out.println("[LANGUAGE] Sampling candidates after language filter failed: " + e.getMessage());
            }

            // 8) After finishing this language run, clear filters so the next language starts clean
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
        }
    }
}
