package Pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import java.time.Duration;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Step;
import io.qameta.allure.Attachment;
import org.testng.ITestResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.util.Map;
import java.util.HashMap;

import org.openqa.selenium.chrome.ChromeOptions;

public class basePage {
    protected WebDriver driver;
    protected WebDriverWait wait;

    /**
     * Initializes the WebDriver and WebDriverWait before each test method.
     * Uses WebDriverManager for automatic browser driver setup, which is a best
     * practice.
     */
    @BeforeMethod
    @Step("Setup: Launching browser and navigating to URL")
    public void setUp() {
        // Use WebDriverManager to automatically download and set up the ChromeDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Disable password manager
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);

        options.setExperimentalOption("prefs", prefs);

        // Disable Chrome security popups
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-features=PasswordCheck");

        // (IMPORTANT) Start with fresh automation profile
        options.addArguments("--guest");

        // Run in visible UI mode (headless disabled for debugging/demo)
        // options.addArguments("--headless=new");
        // options.addArguments("--window-size=1920,1080");
        // options.addArguments("--disable-gpu");
        // options.addArguments("--disable-dev-shm-usage");
        // options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);

        driver.manage().window().maximize();
        // A longer wait time of 15 seconds is more reliable for web applications
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        // driver.get("https://dms.eticaatest.co.in/auth/login");
        driver.get("https://dms.eticaa.com/auth/login");
        System.out.println("INFO: Browser launched and navigated to the application URL.");
    }

    /**
     * Closes the WebDriver after each test method.
     * This method also checks for test failures and attaches a screenshot to the
     * Allure report.
     *
     * @param result The result of the test method execution.
     */
    @AfterMethod
    @Step("Teardown: Closing browser")
    public void tearDown(ITestResult result) {
        // Only capture a screenshot if the test failed
        if (result.getStatus() == ITestResult.FAILURE) {
            captureScreenshot();
        }

        if (driver != null) {
            driver.quit();
            System.out.println("INFO: Browser closed successfully.");
        }
    }

    /**
     * Attaches a screenshot to the Allure report.
     * This method is called from tearDown() only when a test has failed.
     * 
     * @return The captured screenshot as a byte array.
     */
    @Attachment(value = "Page Screenshot", type = "image/png")
    private byte[] captureScreenshot() {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to capture screenshot. " + e.getMessage());
            return new byte[0];
        }
    }
}
