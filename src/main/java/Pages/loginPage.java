package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.Reporter;

import io.qameta.allure.Step;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

import Utils.ScrollHelper;

public class loginPage {
    private WebDriver driver;
    private WebDriverWait wait;

    // Locators
    private By emailField = By.id("email");
    private By passwordField = By.id("password");
    private By loginButton = By.xpath("//button[@type='submit']");
    private By menuButton = By.xpath("(//button[@type='button'])[4]"); // Menu button to open logout menu
    private By logoutButton = By.xpath("//span[text()='Logout']");
    
    private final By loginFailedMessage = By.xpath("//div[text()='Account is Deactivated']");
    
    // Updated locator for the "Logout" button within the custom modal
    private By confirmLogoutButton = By.xpath("//button[text()='Logout']");
    
    // Session conflict locators
    private By sessionConflictMessage = By.xpath("//div[contains(text(),'Active session detected')]");
    private By confirmContinueLoginButton = By.xpath("//button[text() = 'Confirm & Continue Login']");
    
    // Post-login page readiness indicators (navigation menu or dashboard elements)
    private By navigationMenuIndicator = By.xpath("//a[@href='/customer-management/customers'] | //a[@href='/user-management/users'] | //a[@href='/customers'] | //a[@href='/users']");

    // Heading text that appears on the login page – used to start typing as soon as possible
    private By loginHeading = By.xpath("//*[normalize-space(text())='Log in']");

    public loginPage(WebDriver driver) {
        this.driver = driver;
        // 10 seconds was causing long waits before typing; 5 seconds is enough for fields to appear
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(1));
    }

    public void loginAs(String email, String password) {
        System.out.println("\n[LOGIN] =========================================");
        System.out.println("[LOGIN] Starting login process...");
        System.out.println("[LOGIN] Email: " + email);
        System.out.println("[LOGIN] =========================================");
        Reporter.log("INFO: Attempting to log in with email: " + email);
        
        try {
            // Wait for "Log in" text to appear, then give the page 1 second and start typing immediately
            System.out.println("[LOGIN] Step 0: Waiting for 'Log in' heading...");
            wait.until(ExpectedConditions.presenceOfElementLocated(loginHeading));
            Thread.sleep(1000);
            
            // Step 1: Find email field and type without extra waiting
            System.out.println("[LOGIN] Step 1: Locating email field...");
            WebElement emailElement = driver.findElement(emailField);
            System.out.println("[LOGIN] ✅ Email field found, entering email");
            emailElement.click();
            emailElement.clear();
            emailElement.sendKeys(email);
            System.out.println("[LOGIN] ✅ Email entered successfully");
            
            // Step 2: Find password field and type without extra waiting
            System.out.println("[LOGIN] Step 2: Locating password field...");
            WebElement passwordElement = driver.findElement(passwordField);
            System.out.println("[LOGIN] ✅ Password field found, entering password");
            passwordElement.click();
            passwordElement.clear();
            passwordElement.sendKeys(password);
            System.out.println("[LOGIN] ✅ Password entered successfully");
            
            System.out.println("[LOGIN] Step 3: Waiting for login button to be ready...");
            WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(loginButton));
            System.out.println("[LOGIN] ✅ Login button found and ready");
            ScrollHelper.scrollIntoView(driver, loginBtn);
            loginBtn.click();
            System.out.println("[LOGIN] ✅ Login button clicked successfully");
            
            // Step 4: Handle session conflict if it appears
            System.out.println("[LOGIN] Step 4: Checking for session conflict...");
            handleSessionConflict();
            
            // Step 5: Wait for post-login page to be ready
            System.out.println("[LOGIN] Step 5: Waiting for post-login page to be ready...");
            waitForPostLoginPageReady();
            
            System.out.println("[LOGIN] =========================================");
            System.out.println("[LOGIN] Login process completed");
            System.out.println("[LOGIN] =========================================");
            Reporter.log("SUCCESS: Login button clicked.");
            
        } catch (TimeoutException e) {
            System.out.println("[LOGIN] ❌ ERROR: Timeout waiting for login elements");
            System.out.println("[LOGIN] Current URL: " + driver.getCurrentUrl());
            System.out.println("[LOGIN] Error: " + e.getMessage());
            Reporter.log("ERROR: Timeout waiting for login elements. " + e.getMessage());
            throw new RuntimeException("Login failed due to timeout: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("[LOGIN] ❌ ERROR: Unexpected error during login");
            System.out.println("[LOGIN] Current URL: " + driver.getCurrentUrl());
            System.out.println("[LOGIN] Error: " + e.getMessage());
            Reporter.log("ERROR: Unexpected error during login. " + e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
    
//    public void customerLogin(String email,String password) {
//	
//    	Reporter.log("INFO: Attempting to log in with email: " + email);
//        driver.findElement(emailField).clear();
//        driver.findElement(emailField).sendKeys(email);
//
//        driver.findElement(passwordField).clear();
//        driver.findElement(passwordField).sendKeys(password);
//
//        driver.findElement(loginButton).click();
//        Reporter.log("SUCCESS: Login button clicked.");
//    	
//	}
    
    /**
     * Clicks the menu button to open the menu, then clicks the logout button and handles a custom confirmation modal.
     * This method assumes the menu button is visible after a successful login.
     */
    public void logout() {
        System.out.println("\n[LOGOUT] =========================================");
        System.out.println("[LOGOUT] Starting logout process...");
        System.out.println("[LOGOUT] =========================================");
        Reporter.log("INFO: Starting logout process.");
        
        try {
            // Step 1: Click the menu button to open the menu
            System.out.println("[LOGOUT] Step 1: Looking for menu button...");
            System.out.println("[LOGOUT] Menu button locator: (//button[@type='button'])[4]");
            Reporter.log("INFO: Looking for menu button to open logout menu.");
            
            WebElement menuBtn = wait.until(ExpectedConditions.elementToBeClickable(menuButton));
            System.out.println("[LOGOUT] ✅ Menu button found and is clickable");
            ScrollHelper.scrollIntoView(driver, menuBtn);
            menuBtn.click();
            System.out.println("[LOGOUT] ✅ Menu button clicked successfully");
            Reporter.log("SUCCESS: Menu button clicked - menu opened.");
            
            // Step 2: Wait for menu to fully open
            System.out.println("[LOGOUT] Step 2: Waiting for menu to fully open...");
            Thread.sleep(500);
            System.out.println("[LOGOUT] ✅ Menu should be open now");
            
            // Step 3: Click the logout button
            System.out.println("[LOGOUT] Step 3: Looking for logout button in menu...");
            System.out.println("[LOGOUT] Logout button locator: //span[text()='Logout']");
            Reporter.log("INFO: Looking for logout button in the opened menu.");
            
            WebElement logoutBtn = wait.until(ExpectedConditions.elementToBeClickable(logoutButton));
            System.out.println("[LOGOUT] ✅ Logout button found and is clickable");
            ScrollHelper.scrollIntoView(driver, logoutBtn);
            logoutBtn.click();
            System.out.println("[LOGOUT] ✅ Logout button clicked successfully");
            Reporter.log("SUCCESS: Logout button clicked - confirmation modal should appear.");
            
            // Step 4: Handle confirmation modal
            System.out.println("[LOGOUT] Step 4: Looking for confirmation logout button in modal...");
            System.out.println("[LOGOUT] Confirm logout button locator: //button[text()='Logout']");
            Reporter.log("INFO: Looking for confirmation button in logout modal.");
            
            WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(confirmLogoutButton));
            System.out.println("[LOGOUT] ✅ Confirmation logout button found and is clickable");
            ScrollHelper.scrollIntoView(driver, confirmBtn);
            confirmBtn.click();
            System.out.println("[LOGOUT] ✅ Confirmation logout button clicked successfully");
            Reporter.log("SUCCESS: Custom confirmation pop-up accepted - logout completed.");
            
            // Step 5: Logout completed
            System.out.println("[LOGOUT] =========================================");
            System.out.println("[LOGOUT] ✅ Logout process completed successfully!");
            System.out.println("[LOGOUT] =========================================");
            Reporter.log("SUCCESS: Logout process completed successfully.");
            
        } catch (TimeoutException e) {
            System.out.println("[LOGOUT] ❌ ERROR: Timeout waiting for element during logout");
            System.out.println("[LOGOUT] Error message: " + e.getMessage());
            System.out.println("[LOGOUT] Current URL: " + driver.getCurrentUrl());
            Reporter.log("ERROR: Timeout occurred during logout. " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Logout failed due to timeout: " + e.getMessage(), e);
            
        } catch (Exception e) {
            System.out.println("[LOGOUT] ❌ ERROR: An unexpected issue occurred during logout");
            System.out.println("[LOGOUT] Error type: " + e.getClass().getSimpleName());
            System.out.println("[LOGOUT] Error message: " + e.getMessage());
            System.out.println("[LOGOUT] Current URL: " + driver.getCurrentUrl());
            Reporter.log("ERROR: An issue occurred during logout or pop-up handling. " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles session conflict screen that appears when an active session is detected.
     * Scrolls the "Confirm & Continue Login" button into view and clicks it.
     */
    private void handleSessionConflict() {
        try {
            // Check if session conflict message is present (with short timeout)
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement conflictMsg = shortWait.until(ExpectedConditions.presenceOfElementLocated(sessionConflictMessage));
                if (conflictMsg.isDisplayed()) {
                    System.out.println("[LOGIN] ⚠️ Active session detected - session conflict screen appeared");
                    System.out.println("[LOGIN] Message: " + conflictMsg.getText());
                    
                    // Find the "Confirm & Continue Login" button
                    System.out.println("[LOGIN] Looking for 'Confirm & Continue Login' button...");
                    WebElement confirmButton = wait.until(ExpectedConditions.presenceOfElementLocated(confirmContinueLoginButton));
                    ScrollHelper.scrollIntoView(driver, confirmButton);
                    confirmButton = wait.until(ExpectedConditions.elementToBeClickable(confirmContinueLoginButton));
                    confirmButton.click();
                    System.out.println("[LOGIN] ✅ 'Confirm & Continue Login' button clicked successfully");
                    Reporter.log("SUCCESS: Session conflict handled - clicked 'Confirm & Continue Login' button.");
                }
            } catch (TimeoutException e) {
                // No session conflict message found - this is normal, continue
                System.out.println("[LOGIN] ✅ No session conflict detected - proceeding with normal login");
            }
        } catch (Exception e) {
            System.out.println("[LOGIN] ⚠️ WARNING: Error handling session conflict: " + e.getMessage());
            System.out.println("[LOGIN] Continuing with login process...");
            // Don't throw exception - allow login to continue even if conflict handling fails
        }
    }
    
    /**
     * Waits for the post-login page to be ready by checking for navigation menu elements.
     * This ensures the dashboard/main page has fully loaded before attempting navigation.
     */
    private void waitForPostLoginPageReady() {
        try {
            System.out.println("[LOGIN] Waiting for navigation menu to appear...");
            // Wait for any navigation link to be visible (indicates page is loaded)
            wait.until(ExpectedConditions.presenceOfElementLocated(navigationMenuIndicator));
            System.out.println("[LOGIN] ✅ Post-login page is ready - navigation menu visible");
        } catch (TimeoutException e) {
            System.out.println("[LOGIN] ⚠️ WARNING: Navigation menu not found, but continuing...");
            // Don't fail - page might still be usable
        }
    }
    
    @Step("Verifying that the login attempt failed")
    public void verifyLoginFailure() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(loginFailedMessage));
            System.out.println("SUCCESS: Verified that the login failed with the expected message.");
        } catch (TimeoutException e) {
            Assert.fail("Login failure message was not displayed within the timeout.");
        }
    }
}
