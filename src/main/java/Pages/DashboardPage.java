package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Step;

/**
 * Page object for Super Admin Dashboard: assert heading and log values only.
 * No Candidates page, no quick filters. Dashboard only.
 *
 * Dashboard Time dropdown options: All Time, Today, Yesterday, Current Week, Last Week,
 * Current Month, Last Month, Current Quarter, Last Quarter, Current Year, Last Year.
 */
public class DashboardPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    /** Dashboard Time dropdown options (order as in UI). */
    public static final String[] DASHBOARD_TIME_OPTIONS = {
        "All Time", "Today", "Yesterday", "Current Week", "Last Week",
        "Current Month", "Last Month", "Current Quarter", "Last Quarter",
        "Current Year", "Last Year"
    };

    // ----- Dashboard only -----
    private final By dashboardHeading = By.xpath("//h1[text()='Dashboard']");
    private final By timeDropdownButton = By.xpath("/html/body/div[1]/div[2]/div[2]/div[3]/div/div/div[1]/div/div/div/div[1]/button");
    private final By totalProfilesValue = By.xpath("/html/body/div[1]/div[2]/div[2]/div[3]/div/div/div[2]/div[1]/div/p[2]");
    private final By linkedInProfilesCount = By.xpath("/html/body/div[1]/div[2]/div[2]/div[3]/div/div/div[2]/div[2]/div");
    private final By licenseInUse = By.xpath("/html/body/div[1]/div[2]/div[2]/div[3]/div/div/div[2]/div[3]/div");
    private final By loader = By.xpath("//div[@role='progressbar']");

    private final By loggedInUsersCard = By.xpath("//p[text()='Logged in users']/ancestor::div[contains(@class,'bg-white')]");
    private final By loggedInCustomersCard = By.xpath("//p[text()='Logged in customers']/ancestor::div[contains(@class,'bg-white')]");

    // ----- Customer dropdown (beside time dropdown) -----
    private final By customerDropdownButton = By.xpath("//div[contains(@class,'customer-dropdown-container')]//button");
    private final By customerDropdownPanel = By.xpath("//div[contains(@class,'customer-dropdown-container')]//div[contains(@class,'absolute') and contains(@class,'top-full')]");
    private final By customerOptionButtons = By.xpath("//div[contains(@class,'customer-dropdown-container')]//div[contains(@class,'overflow-y-auto')]//button");

    public DashboardPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    @Step("Open customer dropdown")
    public void openCustomerDropdown() {
        wait.until(ExpectedConditions.elementToBeClickable(customerDropdownButton)).click();
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        wait.until(ExpectedConditions.visibilityOfElementLocated(customerDropdownPanel));
        System.out.println("[DASHBOARD] Customer dropdown opened.");
    }

    /** Get display names of all options: "My Dashboard" plus each customer name. */
    public List<String> getCustomerDropdownOptionNames() {
        openCustomerDropdown();
        List<String> names = new ArrayList<>();
        List<WebElement> buttons = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(customerOptionButtons));
        for (WebElement btn : buttons) {
            try {
                List<WebElement> boldSpan = btn.findElements(By.xpath(".//span[contains(@class,'font-bold')]"));
                if (!boldSpan.isEmpty()) {
                    names.add(boldSpan.get(0).getText().trim());
                } else {
                    String text = btn.getText().trim();
                    if (!text.isEmpty()) names.add(text.contains("My Dashboard") ? "My Dashboard" : text.split("\\n")[0].trim());
                }
            } catch (Exception e) {
                // skip
            }
        }
        closeCustomerDropdownIfOpen();
        System.out.println("[DASHBOARD] Customer options: " + names);
        return names;
    }

    /** Select customer by name (e.g. "My Dashboard" or "Placon HR Services"). */
    @Step("Select customer: {customerName}")
    public void selectCustomerDropdown(String customerName) {
        openCustomerDropdown();
        String escaped = customerName.replace("'", "\\'");
        By option = By.xpath(
            "//div[contains(@class,'customer-dropdown-container')]//div[contains(@class,'overflow-y-auto')]//button[.//span[normalize-space()='" + escaped + "']] | " +
            "//div[contains(@class,'customer-dropdown-container')]//div[contains(@class,'overflow-y-auto')]//button[.//span[contains(@class,'font-bold') and normalize-space()='" + escaped + "']]"
        );
        try {
            wait.until(ExpectedConditions.elementToBeClickable(option)).click();
            waitForLoaderGone();
            System.out.println("[DASHBOARD] Selected customer: " + customerName);
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Customer option '" + customerName + "' not found: " + e.getMessage());
            closeCustomerDropdownIfOpen();
            throw e;
        }
    }

    private void closeCustomerDropdownIfOpen() {
        try {
            driver.findElement(dashboardHeading).click();
        } catch (Exception ignored) {}
    }

    @Step("Assert Dashboard heading is visible")
    public void assertDashboardHeading() {
        WebElement h1 = wait.until(ExpectedConditions.visibilityOfElementLocated(dashboardHeading));
        Assert.assertTrue(h1.isDisplayed(), "Dashboard heading should be visible");
        System.out.println("[DASHBOARD] ✅ Dashboard heading asserted.");
    }

    @Step("Select time range in dashboard dropdown: {optionText}")
    public void selectDashboardTimeDropdown(String optionText) {
        wait.until(ExpectedConditions.elementToBeClickable(timeDropdownButton)).click();
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String escaped = optionText.replace("'", "\\'");
        By option = By.xpath(
            "//*[@role='listbox']//*[contains(.,'" + escaped + "')] | " +
            "//*[contains(@role,'option')][contains(.,'" + escaped + "')] | " +
            "//li[contains(.,'" + escaped + "')] | " +
            "//span[normalize-space(text())='" + escaped + "'] | " +
            "//div[normalize-space(text())='" + escaped + "']"
        );
        wait.until(ExpectedConditions.elementToBeClickable(option)).click();
        System.out.println("[DASHBOARD] Selected Dashboard Time: " + optionText);
    }

    @Step("Get total profiles count from dashboard")
    public String getTotalProfilesCount() {
        waitForLoaderGone();
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(totalProfilesValue));
        String count = el.getText().trim();
        System.out.println("[DASHBOARD] Total profiles: " + count);
        return count;
    }

    @Step("Log LinkedIn profiles count")
    public void logLinkedInProfilesCount() {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(linkedInProfilesCount));
            System.out.println("[DASHBOARD] LinkedIn profiles count: " + el.getText().trim());
        } catch (Exception e) {
            System.out.println("[DASHBOARD] LinkedIn profiles count: element not found - " + e.getMessage());
        }
    }

    @Step("Log License in Use")
    public void logLicenseInUse() {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(licenseInUse));
            System.out.println("[DASHBOARD] License in Use: " + el.getText().trim());
        } catch (Exception e) {
            System.out.println("[DASHBOARD] License in Use: element not found - " + e.getMessage());
        }
    }

    @Step("Log Logged in users (Chrome Ext / Web)")
    public void logLoggedInUsers() {
        try {
            WebElement card = wait.until(ExpectedConditions.visibilityOfElementLocated(loggedInUsersCard));
            String full = card.getText();
            System.out.println("[DASHBOARD] Logged in users (card text): " + full.trim());
            List<WebElement> bigNumbers = card.findElements(By.xpath(".//p[contains(@class,'text-[30px') or contains(@class,'font-semibold')]"));
            for (WebElement n : bigNumbers) {
                String v = n.getText().trim();
                if (!v.isEmpty()) System.out.println("[DASHBOARD] Logged in users value: " + v);
            }
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Logged in users: element not found - " + e.getMessage());
        }
    }

    @Step("Log Logged in customers")
    public void logLoggedInCustomers() {
        try {
            WebElement card = wait.until(ExpectedConditions.visibilityOfElementLocated(loggedInCustomersCard));
            String full = card.getText();
            System.out.println("[DASHBOARD] Logged in customers (card text): " + full.trim());
            List<WebElement> bigNumbers = card.findElements(By.xpath(".//p[contains(@class,'text-[30px') or contains(@class,'font-semibold')]"));
            for (WebElement n : bigNumbers) {
                String v = n.getText().trim();
                if (!v.isEmpty()) System.out.println("[DASHBOARD] Logged in customers value: " + v);
            }
        } catch (Exception e) {
            System.out.println("[DASHBOARD] Logged in customers: element not found - " + e.getMessage());
        }
    }

    private void waitForLoaderGone() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        } catch (Exception ignored) {}
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Step("Log all dashboard metrics (LinkedIn, License, Logged in users/customers)")
    public void logAllDashboardMetrics() {
        logLinkedInProfilesCount();
        logLicenseInUse();
        logLoggedInUsers();
        logLoggedInCustomers();
    }
}
