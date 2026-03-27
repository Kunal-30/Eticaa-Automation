package testPackage.helpers;

import Pages.loginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared login helper for tests.
 * Provides reusable methods to log in as Super Admin and as the dedicated
 * Advanced Filter user without duplicating the popup handling logic.
 */
public class LoginHelper {

    private static void loginWithCredentials(WebDriver driver, WebDriverWait wait,
                                             String email, String password) throws InterruptedException {
        loginPage lp = new loginPage(driver);
        lp.loginAs(email, password);
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Continue Login')]")
                )).click();
            }
        } catch (TimeoutException e) {
            // No conflict popup – normal case, ignore
        }
        Thread.sleep(1500);
    }

    /**
     * Login as Super Admin (admin10@gmail.com / Admin@123).
     */
    public static void loginAsSuperAdmin(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        loginWithCredentials(driver, wait, "admin10@gmail.com", "Admin@123");
    }

    /**
     * Login as dedicated Advanced Filter user (never deleted in cleanup).
     * Email: kunal@placonhr.com
     * Pass : User@123
     */
    public static void loginAsAdvancedFilterUser(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        loginWithCredentials(driver, wait, "kunal@placonhr.com", "User@123");
    }
}

