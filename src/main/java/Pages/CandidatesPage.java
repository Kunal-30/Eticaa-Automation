package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import Utils.ScrollHelper;
import io.qameta.allure.Step;

/**
 * Candidates list page: Advance Search (All Filters), apply filters, get result count,
 * open candidate profiles (NAME column), pagination via combobox at bottom.
 */
public class CandidatesPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By candidatesPageLink = By.xpath("//a[@href='/candidate-management/candidates']");
    private final By advanceSearchButton = By.xpath("//button[contains(.,'Advance Search')] | //*[contains(text(),'Advance Search')]/ancestor::button");
    private final By clearFiltersButton = By.xpath("//button[normalize-space(text())='Clear Filters'] | //span[normalize-space(text())='Clear Filters']");
    private final By loader = By.xpath("//div[@role='progressbar']");

    // All Filters panel: click "+ Filter by Company" button first, then search bar opens
    private final By filterByCompany = By.xpath("//button[contains(@class,'cursor-pointer') and contains(.,'Filter by Company') and .//span[text()='+']]");
    private final By filterByLocation = By.xpath("//button[contains(@class,'cursor-pointer') and contains(.,'Filter by Location')] | //*[contains(text(),'Filter by Location') or contains(.,'+ Filter by Location')]");
    private final By filterByDesignation = By.xpath("//button[contains(@class,'cursor-pointer') and contains(.,'Filter by Designation')] | //*[contains(text(),'Filter by Designation') or contains(.,'+ Filter by Designation')]");

    // Result count: captured after applying filter (MuiBox that shows count for current search)
    private final By resultCountBox = By.cssSelector(".MuiBox-root.css-1qocved");
    private final By resultCountFallback = By.xpath("//*[contains(text(),'candidates')]");

    // NAME column: click only the name link (opens profile). Don't rely on class names (e.g. css-1a3o6tk) as they may change.
    // Name link: <a> with visible text (candidate name) or href="#" or internal path; exclude LinkedIn/WhatsApp (external hrefs).
    private static final String NAME_LINK_PREDICATE =
        "[(contains(@href,'/candidate-management/') or contains(@href,'/candidates/') or contains(@href,'/contacts/') or @href='#' or normalize-space(text())!='')"
        + " and not(contains(@href,'linkedin')) and not(contains(@href,'whatsapp')) and not(contains(@href,'wa.me'))]";
    private final By candidateNameLinks = By.xpath(
        "//td[@data-index='1']//a" + NAME_LINK_PREDICATE
        + " | //*[@data-column-id='fullName']//a" + NAME_LINK_PREDICATE
        + " | //*[@data-field='fullName']//a" + NAME_LINK_PREDICATE
        + " | //td[@data-pinned='true']//a" + NAME_LINK_PREDICATE
    );

    // Pagination: appears after scroll to bottom
    private final By paginationCombobox = By.xpath("//div[@role='combobox']");

    // New UI: Location section & mode dropdown inside Advance Search
    private final By locationSectionButton = By.xpath("//button[text()='Location']");
    // Top tab label for Location in split view (used only for scrolling Split A into view)
    private final By locationTabLabelForScroll = By.xpath("(//span[text()='Location'])[3]");
    private final By locationModeTrigger = By.xpath("//span[@class='capitalize']");

    // New UI: Designation section (split view)
    private final By designationSectionButton = By.xpath("//button[text()='Designation']");
    private final By designationTabLabelForScroll = By.xpath("(//span[text()='Designation'])[3]");

    // New UI: Experience section (split view)
    private final By experienceTabLabelForScroll = By.xpath("//span[text()='Experience']");
    private final By experienceSectionButton = By.xpath("//button[text()='Experience']");

    // Current CTC, Expected CTC, Notice Period sections
    private final By currentCTCSectionButton = By.xpath("//button[contains(text(),'Current CTC') or text()='Current CTC']");
    private final By expectedCTCSectionButton = By.xpath("//button[contains(text(),'Expected CTC') or text()='Expected CTC']");
    private final By noticePeriodSectionButton = By.xpath("//button[contains(text(),'Notice period') or text()='Notice period']");

    // Personal Details section filters
    private final By genderSectionButton = By.xpath("//button[contains(text(),'Gender') or contains(.,'Gender')]");
    private final By maritalStatusSectionButton = By.xpath("//button[contains(text(),'Marital Status') or contains(.,'Marital Status') or contains(text(),'Marital')]");
    private final By ageSectionButton = By.xpath("//button[contains(text(),'Age') or contains(.,'Age')]");
    private final By physicallyChallengedSectionButton = By.xpath("//button[contains(text(),'Physically Challenged') or contains(.,'Physically Challenged') or contains(text(),'Physically')]");
    private final By categorySectionButton = By.xpath("//button[contains(text(),'Category') or contains(.,'Category')]");

    public CandidatesPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void candidatesPage_Link() {
        WebElement candidatesPageLinkElement = wait.until(ExpectedConditions.elementToBeClickable(candidatesPageLink));
        ScrollHelper.scrollIntoView(driver, candidatesPageLinkElement);
        candidatesPageLinkElement.click();
        waitForLoaderGone();
        System.out.println("[CANDIDATES] Candidates page opened.");
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("[CANDIDATES] Candidates page loaded.");
        waitForLoaderGone();
        System.out.println("[CANDIDATES] Candidates page loaded.");
        waitForLoaderGone();
        System.out.println("[CANDIDATES] Candidates page loaded.");
        waitForLoaderGone();
        System.out.println("[CANDIDATES] Candidates page loaded.");
        waitForLoaderGone();
        System.out.println("[CANDIDATES] Candidates page loaded.");
    }

    @Step("Open Advance Search (All Filters) panel")
    public void openAdvanceSearch() {
        waitForLoaderGone();
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(advanceSearchButton));
        ScrollHelper.scrollIntoView(driver, btn);
        btn.click();
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("[CANDIDATES] Advance Search opened.");
    }

    @Step("Click Clear Filters (only when visible)")
    public void clickClearFiltersIfPresent() {
        List<WebElement> btns = driver.findElements(clearFiltersButton);
        if (!btns.isEmpty()) {
            try {
                WebElement clearBtn = wait.until(ExpectedConditions.elementToBeClickable(clearFiltersButton));
                ScrollHelper.scrollIntoView(driver, clearBtn);
                clearBtn.click();
                waitForLoaderGone();
                System.out.println("[CANDIDATES] Cleared filters.");
            } catch (Exception e) {
                System.out.println("[CANDIDATES] Clear Filters not clickable: " + e.getMessage());
            }
        }
    }

    @Step("Add and set Company filter: {value}")
    public void setCompanyFilter(String value) {
        setCompanyFilterWithType(value, null);
    }

    /**
     * Company filter with type: open Company section, open type dropdown (Current+Past/Current/Past),
     * select type, then enter company name in the text input and apply.
     * XPaths: (//button[@type="button"])[5] = type dropdown; (//input[@type="text"])[4] = company name input.
     */
    @Step("Add and set Company filter: {companyName} (Type: {type})")
    public void setCompanyFilterWithType(String companyName, String type) {
        try {
            WebElement filterBtn = wait.until(ExpectedConditions.elementToBeClickable(filterByCompany));
            ScrollHelper.scrollIntoView(driver, filterBtn);
            filterBtn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Filter by Company not found: " + e.getMessage());
            return;
        }
        // Open type dropdown: (//button[@type="button"])[5] then select Current / Past / Current+Past
        if (type != null && !type.trim().isEmpty()) {
            try {
                By typeDropdownButton = By.xpath("(//button[@type=\"button\"])[5]");
                WebElement typeBtn = wait.until(ExpectedConditions.elementToBeClickable(typeDropdownButton));
                ScrollHelper.scrollIntoView(driver, typeBtn);
                typeBtn.click();
                Thread.sleep(400);
                // Map our Type values to the exact UI label text
                String trimmed = type.trim();
                String uiText;
                if (trimmed.equalsIgnoreCase("Current + Past") || trimmed.equalsIgnoreCase("Current+Past")) {
                    uiText = "Current+Past";
                } else if (trimmed.equalsIgnoreCase("Current")) {
                    uiText = "Current";
                } else if (trimmed.equalsIgnoreCase("Past")) {
                    uiText = "Past";
                } else {
                    uiText = trimmed;
                }
                // In the dropdown HTML, labels are: <span class="text-sm text-gray-700">Current</span> etc.
                By typeOption = By.xpath("//div[contains(@class,'company-type-dropdown-menu')]//span[normalize-space(text())='" + uiText + "']");
                WebElement optionSpan = wait.until(ExpectedConditions.elementToBeClickable(typeOption));
                ScrollHelper.scrollIntoView(driver, optionSpan);
                optionSpan.click();
                Thread.sleep(400);
            } catch (Exception e) {
                System.out.println("[CANDIDATES] Company type dropdown/option: " + e.getMessage());
            }
        }
        // Company name input: (//input[@type="text"])[4]
        By companyInput = By.xpath("(//input[@type='text'])[4]");
        try {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(companyInput));
            ScrollHelper.scrollIntoView(driver, input);
            input.clear();
            input.sendKeys(companyName);
            Thread.sleep(600);
            input.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            try {
                By searchInput = By.xpath("//input[@placeholder='Search Company']");
                WebElement fallback = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));
                ScrollHelper.scrollIntoView(driver, fallback);
                fallback.clear();
                fallback.sendKeys(companyName);
                Thread.sleep(600);
                fallback.sendKeys(Keys.ENTER);
            } catch (Exception e2) {
                System.out.println("[CANDIDATES] Company search input: " + e.getMessage());
            }
        }
        applyFilter();
    }

    @Step("Add and set Location filter: {value}")
    public void setLocationFilter(String value) {
        try {
            WebElement filterBtn = wait.until(ExpectedConditions.elementToBeClickable(filterByLocation));
            ScrollHelper.scrollIntoView(driver, filterBtn);
            filterBtn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Filter by Location not found: " + e.getMessage());
            return;
        }
        By searchInput = By.xpath("//input[contains(@placeholder,'location') or contains(@placeholder,'Location') or contains(@placeholder,'Search')]");
        try {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));
            ScrollHelper.scrollIntoView(driver, input);
            input.clear();
            input.sendKeys(value);
            Thread.sleep(600);
            input.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Location search input: " + e.getMessage());
        }
        applyFilter();
    }

    /**
     * New Location filter flow with mode dropdown:
     * - Click Location section button
     * - Click mode dropdown (//span[@class='capitalize']) and choose one of:
     *   Either (Current or Preferred) / Current Location / Preferred Location / Both (Current and Preferred)
     * - For Either, Current, Preferred: enter location in (//input[@type="text"])[4]
     * - For Both: enter current in (//input[@type="text"])[4] and preferred in (//input[@type="text"])[5]
     */
    @Step("Set Location filter with mode: {mode}, current={currentLocation}, preferred={preferredLocation}")
    public void setLocationFilterWithMode(String mode, String currentLocation, String preferredLocation) {
        try {
            // Ensure Split A is scrolled so the Location section is visible
            try {
                WebElement locTab = wait.until(ExpectedConditions.visibilityOfElementLocated(locationTabLabelForScroll));
                ScrollHelper.scrollIntoView(driver, locTab);
            } catch (Exception ignored) { }

            WebElement locBtn = wait.until(ExpectedConditions.elementToBeClickable(locationSectionButton));
            ScrollHelper.scrollIntoView(driver, locBtn);
            locBtn.click();
            Thread.sleep(400);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Location section button not found: " + e.getMessage());
            return;
        }
        try {
            WebElement modeBtn = wait.until(ExpectedConditions.elementToBeClickable(locationModeTrigger));
            ScrollHelper.scrollIntoView(driver, modeBtn);
            modeBtn.click();
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Location mode dropdown trigger not found: " + e.getMessage());
            return;
        }
        String m = mode == null ? "" : mode.trim();
        String uiText;
        if (m.equalsIgnoreCase("Either") || m.startsWith("Either")) {
            uiText = "Either (Current or Preferred)";
        } else if (m.equalsIgnoreCase("Current") || m.equalsIgnoreCase("Current Location")) {
            uiText = "Current Location";
        } else if (m.toLowerCase().startsWith("pref")) {
            uiText = "Preferred Location";
        } else if (m.equalsIgnoreCase("Both") || m.startsWith("Both")) {
            uiText = "Both (Current and Preferred)";
        } else {
            uiText = m;
        }
        try {
            // Be flexible for Both option: match any button containing the text (to avoid exact-text issues)
            By optionLocator = By.xpath(
                "//div[contains(@class,'location-mode-dropdown-menu')]//button[" +
                    "normalize-space(text())='" + uiText + "' or contains(normalize-space(.), '" + uiText + "') or contains(normalize-space(.), 'Both')" +
                "]"
            );
            WebElement option = wait.until(ExpectedConditions.elementToBeClickable(optionLocator));
            ScrollHelper.scrollIntoView(driver, option);
            option.click();
            Thread.sleep(400);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Location mode option not found for text '" + mode + "': " + e.getMessage());
        }
        try {
            // Current location search bar (input 4)
            By input4 = By.xpath("(//input[@type='text'])[4]");
            WebElement inputCur = wait.until(ExpectedConditions.visibilityOfElementLocated(input4));
            ScrollHelper.scrollIntoView(driver, inputCur);
            inputCur.clear();
            if (currentLocation != null && !currentLocation.trim().isEmpty()) {
                inputCur.sendKeys(currentLocation.trim());
                inputCur.sendKeys(Keys.ENTER);  // press Enter after entering current value
            }
            // For Both: two search bars – one for Current, one for Preferred. Enter value then Enter in each.
            if ("Both (Current and Preferred)".equals(uiText) && preferredLocation != null && !preferredLocation.trim().isEmpty()) {
                By input5 = By.xpath("(//input[@type='text'])[5]");
                WebElement inputPref = wait.until(ExpectedConditions.visibilityOfElementLocated(input5));
                ScrollHelper.scrollIntoView(driver, inputPref);
                inputPref.clear();
                inputPref.sendKeys(preferredLocation.trim());
                inputPref.sendKeys(Keys.ENTER);  // press Enter after entering preferred value
            }
            Thread.sleep(600);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Location mode input(s): " + e.getMessage());
        }
        applyFilter();
    }

    @Step("Add and set Designation filter: {value}")
    public void setDesignationFilter(String value) {
        try {
            WebElement filterBtn = wait.until(ExpectedConditions.presenceOfElementLocated(designationSectionButton));
            // Location expand above can cover Designation; scroll Designation to top of scroll container
            ScrollHelper.scrollIntoViewAtTop(driver, filterBtn);
            Thread.sleep(300);
            filterBtn = wait.until(ExpectedConditions.elementToBeClickable(designationSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, filterBtn);
            filterBtn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Filter by Designation not found: " + e.getMessage());
            return;
        }
        // In new UI, designation input is typically the 4th text input under Designation; fall back to placeholder when needed
        By searchInput = By.xpath("(//input[@type='text'])[4]");
        try {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));
            ScrollHelper.scrollIntoView(driver, input);
            input.clear();
            input.sendKeys(value);
            Thread.sleep(600);
            input.sendKeys(Keys.ENTER);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Designation search input: " + e.getMessage());
        }
        applyFilter();
    }

    /**
     * Experience filter: scroll to Experience tab in split A, click Experience button,
     * then set Min and Max years inputs and apply.
     */
    @Step("Set Experience filter: min={minYears}, max={maxYears}")
    public void setExperienceFilter(Integer minYears, Integer maxYears) {
        try {
            WebElement expBtn = wait.until(ExpectedConditions.presenceOfElementLocated(experienceSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, expBtn);
            Thread.sleep(300);
            expBtn = wait.until(ExpectedConditions.elementToBeClickable(experienceSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, expBtn);
            expBtn.click();
            Thread.sleep(400);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Experience section button not found: " + e.getMessage());
            return;
        }

        try {
            By minInputBy = By.xpath("//label[normalize-space(text())='Min']/following-sibling::input[@type='number' or @type='text']");
            By maxInputBy = By.xpath("//label[normalize-space(text())='Max']/following-sibling::input[@type='number' or @type='text']");
            WebElement minInput = wait.until(ExpectedConditions.visibilityOfElementLocated(minInputBy));
            WebElement maxInput = wait.until(ExpectedConditions.visibilityOfElementLocated(maxInputBy));

            ScrollHelper.scrollIntoView(driver, minInput);
            minInput.clear();
            if (minYears != null) {
                minInput.sendKeys(String.valueOf(minYears));
            }

            ScrollHelper.scrollIntoView(driver, maxInput);
            maxInput.clear();
            if (maxYears != null) {
                maxInput.sendKeys(String.valueOf(maxYears));
            }
            Thread.sleep(600);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Experience min/max inputs not found: " + e.getMessage());
        }

        applyFilter();
    }

    /**
     * Current CTC filter: min/max in lakhs (similar to Experience).
     */
    @Step("Set Current CTC filter: min={minLakhs}, max={maxLakhs} LPA")
    public void setCurrentCTCFilter(Integer minLakhs, Integer maxLakhs) {
        setCTCFilterSection(currentCTCSectionButton, "Current CTC", minLakhs, maxLakhs);
    }

    /**
     * Expected CTC filter: min/max in lakhs.
     */
    @Step("Set Expected CTC filter: min={minLakhs}, max={maxLakhs} LPA")
    public void setExpectedCTCFilter(Integer minLakhs, Integer maxLakhs) {
        setCTCFilterSection(expectedCTCSectionButton, "Expected CTC", minLakhs, maxLakhs);
    }

    private void setCTCFilterSection(By sectionButton, String label, Integer minLakhs, Integer maxLakhs) {
        try {
            WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(sectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            Thread.sleep(300);
            btn = wait.until(ExpectedConditions.elementToBeClickable(sectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            btn.click();
            Thread.sleep(400);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] " + label + " section button not found: " + e.getMessage());
            return;
        }
        try {
            By minInputBy = By.xpath("//label[normalize-space(text())='Min' or contains(normalize-space(text()),'Min')]/following-sibling::input[@type='number' or @type='text'] | //input[@placeholder='Min' or contains(@placeholder,'Min')]");
            By maxInputBy = By.xpath("//label[normalize-space(text())='Max' or contains(normalize-space(text()),'Max')]/following-sibling::input[@type='number' or @type='text'] | //input[@placeholder='Max' or contains(@placeholder,'Max')]");
            WebElement minInput = wait.until(ExpectedConditions.visibilityOfElementLocated(minInputBy));
            WebElement maxInput = wait.until(ExpectedConditions.visibilityOfElementLocated(maxInputBy));
            ScrollHelper.scrollIntoView(driver, minInput);
            minInput.clear();
            if (minLakhs != null) minInput.sendKeys(String.valueOf(minLakhs));
            ScrollHelper.scrollIntoView(driver, maxInput);
            maxInput.clear();
            if (maxLakhs != null) maxInput.sendKeys(String.valueOf(maxLakhs));
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] " + label + " min/max inputs: " + e.getMessage());
        }
        applyFilter();
    }

    /**
     * Notice Period filter: select from dropdown (e.g. "Immediate", "15 days", "30 days", "60 days", "90 days").
     * Clicks Notice Period section, then selects the option (dropdown or list).
     */
    @Step("Set Notice Period filter: {option}")
    public void setNoticePeriodFilter(String option) {
        if (option == null || option.trim().isEmpty()) return;
        try {
            WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(noticePeriodSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            Thread.sleep(300);
            btn = wait.until(ExpectedConditions.elementToBeClickable(noticePeriodSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            btn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Notice Period section button not found: " + e.getMessage());
            return;
        }
        try {
            String optText = option.trim();
            By optionLocator = By.xpath(
                "//*[@role='listbox']//*[contains(normalize-space(.), '" + optText + "')] | " +
                "//*[@role='option'][contains(normalize-space(.), '" + optText + "')] | " +
                "//li[contains(normalize-space(.), '" + optText + "')] | " +
                "//div[contains(@class,'menu')]//*[contains(normalize-space(.), '" + optText + "')] | " +
                "//button[contains(normalize-space(.), '" + optText + "')] | " +
                "//span[contains(normalize-space(.), '" + optText + "')]/ancestor::button | " +
                "//*[normalize-space(text())='" + optText + "']"
            );
            WebElement opt = wait.until(ExpectedConditions.elementToBeClickable(optionLocator));
            ScrollHelper.scrollIntoView(driver, opt);
            opt.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Notice Period dropdown/option '" + option + "': " + e.getMessage());
        }
        applyFilter();
    }

    /**
     * Personal Details – Gender filter. Options: Female, Male, Other.
     */
    @Step("Set Gender filter: {option}")
    public void setGenderFilter(String option) {
        setPersonalDetailsDropdownFilter(genderSectionButton, option, "Gender");
    }

    /**
     * Personal Details – Marital Status filter. Options: Divorced, Married, Separated, Single/Unmarried.
     */
    @Step("Set Marital Status filter: {option}")
    public void setMaritalStatusFilter(String option) {
        setPersonalDetailsDropdownFilter(maritalStatusSectionButton, option, "Marital Status");
    }

    /**
     * Personal Details – Age filter (min and max range).
     */
    @Step("Set Age filter: min={minAge}, max={maxAge}")
    public void setAgeFilter(Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) return;
        try {
            WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(ageSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            Thread.sleep(300);
            btn = wait.until(ExpectedConditions.elementToBeClickable(ageSectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            btn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Age section button not found: " + e.getMessage());
            return;
        }
        try {
            By minInputBy = By.xpath("//label[normalize-space(text())='Min' or contains(text(),'Min')]/following-sibling::input | //input[@placeholder='Min' or contains(@placeholder,'Min')]");
            By maxInputBy = By.xpath("//label[normalize-space(text())='Max' or contains(text(),'Max')]/following-sibling::input | //input[@placeholder='Max' or contains(@placeholder,'Max')]");
            List<WebElement> minInputs = driver.findElements(minInputBy);
            List<WebElement> maxInputs = driver.findElements(maxInputBy);
            if (!minInputs.isEmpty() && minAge != null) {
                WebElement minInput = minInputs.get(0);
                ScrollHelper.scrollIntoView(driver, minInput);
                minInput.clear();
                minInput.sendKeys(String.valueOf(minAge));
            }
            if (!maxInputs.isEmpty() && maxAge != null) {
                WebElement maxInput = maxInputs.get(0);
                ScrollHelper.scrollIntoView(driver, maxInput);
                maxInput.clear();
                maxInput.sendKeys(String.valueOf(maxAge));
            }
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Age min/max inputs: " + e.getMessage());
        }
        applyFilter();
    }

    /**
     * Personal Details – Physically Challenged filter. Options: No, Yes.
     */
    @Step("Set Physically Challenged filter: {option}")
    public void setPhysicallyChallengedFilter(String option) {
        setPersonalDetailsDropdownFilter(physicallyChallengedSectionButton, option, "Physically Challenged");
    }

    /**
     * Personal Details – Category filter. Options: General, OBC - Creamy, OBC - Non creamy, Other, Scheduled Caste (SC), Scheduled Tribe (ST).
     */
    @Step("Set Category filter: {option}")
    public void setCategoryFilter(String option) {
        setPersonalDetailsDropdownFilter(categorySectionButton, option, "Category");
    }

    /** Opens a Personal Details section, selects the option by visible text, then applies filter. */
    private void setPersonalDetailsDropdownFilter(By sectionButton, String option, String sectionName) {
        if (option == null || option.trim().isEmpty()) return;
        try {
            WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(sectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            Thread.sleep(300);
            btn = wait.until(ExpectedConditions.elementToBeClickable(sectionButton));
            ScrollHelper.scrollIntoViewAtTop(driver, btn);
            btn.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] " + sectionName + " section button not found: " + e.getMessage());
            return;
        }
        try {
            String optText = option.trim();
            By optionLocator = By.xpath(
                "//*[@role='listbox']//*[contains(normalize-space(.), '" + optText.replace("'", "''") + "')] | " +
                "//*[@role='option'][contains(normalize-space(.), '" + optText.replace("'", "''") + "')] | " +
                "//li[contains(normalize-space(.), '" + optText.replace("'", "''") + "')] | " +
                "//div[contains(@class,'menu')]//*[contains(normalize-space(.), '" + optText.replace("'", "''") + "')] | " +
                "//button[contains(normalize-space(.), '" + optText.replace("'", "''") + "')] | " +
                "//span[contains(normalize-space(.), '" + optText.replace("'", "''") + "')]/ancestor::button | " +
                "//*[normalize-space(text())='" + optText.replace("'", "''") + "']"
            );
            WebElement opt = wait.until(ExpectedConditions.elementToBeClickable(optionLocator));
            ScrollHelper.scrollIntoView(driver, opt);
            opt.click();
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("[CANDIDATES] " + sectionName + " dropdown/option '" + option + "': " + e.getMessage());
        }
        applyFilter();
    }

    /** Apply filter: click Apply button if present, else send Enter. */
    public void applyFilter() {
        try {
            By applyBtn = By.xpath("//button[contains(text(),'Apply')]");
            List<WebElement> btns = driver.findElements(applyBtn);
            if (!btns.isEmpty()) {
                WebElement apply = wait.until(ExpectedConditions.elementToBeClickable(applyBtn));
                ScrollHelper.scrollIntoView(driver, apply);
                apply.click();
            } else {
                driver.switchTo().activeElement().sendKeys(Keys.ENTER);
            }
        } catch (Exception e) {
            driver.switchTo().activeElement().sendKeys(Keys.ENTER);
        }
        waitForLoaderGone();
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Step("Capture candidate count for current search (from MuiBox-root css-1qocved), then proceed")
    public int getResultCount() {
        waitForLoaderGone();
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(resultCountBox));
            ScrollHelper.scrollIntoView(driver, el);
            String text = el.getText();
            int count = parseFirstNumberFromText(text);
            System.out.println("[CANDIDATES] Result count for this filter: " + count + " (from " + text.trim() + ")");
            return count;
        } catch (Exception e) {
            try {
                WebElement fallback = wait.until(ExpectedConditions.visibilityOfElementLocated(resultCountFallback));
                ScrollHelper.scrollIntoView(driver, fallback);
                String text = fallback.getText();
                int count = parseFirstNumberFromText(text);
                return count;
            } catch (Exception e2) {
                System.out.println("[CANDIDATES] Result count not found: " + e.getMessage());
                return 0;
            }
        }
    }

    /** Parse only the first number from text (e.g. "8648 candidates for NP 0 - 15 days" -> 8648, not 8648015). */
    private int parseFirstNumberFromText(String text) {
        if (text == null || text.isEmpty()) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /** Get all visible candidate name links on current page. */
    public List<WebElement> getCandidateNameLinksOnCurrentPage() {
        waitForLoaderGone();
        return driver.findElements(candidateNameLinks);
    }

    /** Scroll to bottom so the pagination combobox (page numbers) becomes visible. The locator appears only when you scroll till the last of the page. */
    @Step("Scroll to bottom to reveal pagination")
    public void scrollToBottom() {
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Scroll to top of page (e.g. after returning from candidate details tab). */
    public void scrollToTop() {
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, 0);");
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Go to a specific page: scroll till the last of the page so the pagination locator appears, click it to open page numbers, then click the next page.
     * Page does not change until you scroll to bottom, then click the combobox and select the page number.
     */
    @Step("Open pagination combobox and select page {pageNum}")
    public void selectPage(int pageNum) {
        System.out.println("[CANDIDATES] Changing to page " + pageNum + " ...");
        scrollToBottom();
        try {
            // Wait until combobox is visible (scroll may have brought it into view)
            WebElement combobox = wait.until(ExpectedConditions.visibilityOfElementLocated(paginationCombobox));
            ScrollHelper.scrollIntoView(driver, combobox);
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            combobox.click();
            Thread.sleep(500);
            By pageOption = By.xpath("//*[@role='listbox']//*[contains(.,'" + pageNum + "')] | //*[@role='option'][contains(.,'" + pageNum + "')] | //li[contains(.,'" + pageNum + "')]");
            WebElement option = wait.until(ExpectedConditions.elementToBeClickable(pageOption));
            ScrollHelper.scrollIntoView(driver, option);
            option.click();
            waitForLoaderGone();
            System.out.println("[CANDIDATES] Now on page " + pageNum + ".");
        } catch (Exception e) {
            System.out.println("[CANDIDATES] Pagination page " + pageNum + ": " + e.getMessage());
        }
    }

    /** Open candidate profile by clicking the name link; may open in new tab. */
    public void openCandidateProfile(WebElement nameLink) {
        ScrollHelper.scrollIntoView(driver, nameLink);
        String href = nameLink.getAttribute("href");
        if (href != null && (href.contains("candidate") || href.contains("contacts"))) {
            nameLink.click();
        } else {
            nameLink.click();
        }
        try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void waitForLoaderGone() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        } catch (Exception ignored) {}
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Set filter by type (Company, Location, Designation). For Company, type (Current+Past/Current/Past) defaults. */
    public void setFilterByType(String filterType, String filterValue) {
        setFilterByType(filterType, filterValue, null);
    }

    /** Set filter by type; for Company, pass type (Current + Past / Current / Past) to use type dropdown and input[4]. */
    public void setFilterByType(String filterType, String filterValue, String type) {
        String ft = filterType == null ? "" : filterType.trim();
        String value = filterValue == null ? "" : filterValue.trim();
        if (value.isEmpty()) return;
        if (ft.equalsIgnoreCase("Company")) {
            setCompanyFilterWithType(value, type != null && !type.trim().isEmpty() ? type : null);
        } else if (ft.equalsIgnoreCase("Location")) {
            setLocationFilter(value);
        } else if (ft.equalsIgnoreCase("Designation")) {
            setDesignationFilter(value);
        } else {
            System.out.println("[CANDIDATES] Unsupported filter type for now: " + filterType);
        }
    }
}
