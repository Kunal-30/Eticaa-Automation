package testPackage.helpers;

import Pages.loginPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared login helper for tests.
 * Provides reusable methods to log in as Super Admin and as the dedicated
 * Advanced Filter user without duplicating the popup handling logic.
 */
public class LoginHelper {

        /**
     * Logs in using the provided credentials.
     * loginPage.loginAs already handles waiting for fields, entering email/password,
     * conflict popup, and post-login readiness. We simply delegate to it to avoid
     * duplicate waits and extra sleeps that slow down typing.
     */
    private static void loginWithCredentials(WebDriver driver, WebDriverWait wait,
                                             String email, String password) throws InterruptedException {
        loginPage lp = new loginPage(driver);
        lp.loginAs(email, password);
    }

    /**
     * Login as Super Admin (admin10@gmail.com / Admin@123).
     */
    public static void loginAsSuperAdmin(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        loginWithCredentials(driver, wait, "admin10@gmail.com", "Admin@123");
    }

    /**
     * Login as dedicated Advanced Filter user (never deleted in cleanup).
        * Email: user0118@test.com
     * Pass : Admin@123
     */
    public static void loginAsAdvancedFilterUser(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        loginWithCredentials(driver, wait, "user0118@test.com", "Admin@123");
    }
}

