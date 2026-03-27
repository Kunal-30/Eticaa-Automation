package testPackage;

import Pages.CandidatesPage;
import Pages.CustomerPage;
import Pages.DashboardPage;
import Pages.UserPage;
import Pages.basePage;
import Pages.loginPage;
import Pages.superAdminPage;
import Manager.DatabaseCleanupManager;
import Utils.SSHTunnelManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.ITestResult;
import io.qameta.allure.Feature;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

@Feature("Super Admin Functionality")
public class superadmintest extends basePage {

    // Hardcoded Data (Variables for easy access)
    String customerOneName = "Customer One Company";
    String customerOneEmail = "customer@customerone.com";
    String customerOneDomain = "customerone.com";
    String user1Customer1FullName = "User1 Customer1";
    String user1Customer1Email = "user1";
    String user1Customer1EmailLogin = "user1@customerone.com";
    String user2Customer1FullName = "User2 Customer1";
    String user2Customer1Email = "user2@customerone.com";
    String user2Customer1EmailLogin = "user2@customerone.com";
    
    String customerTwoName = "Customer Two Company";
    String customerTwoEmail = "customer@customertwo.com";
    String customerTwoDomain = "customertwo.com";
    String user1Customer2FullName = "User1 Customer2";
    String user1Customer2Email = "user1@customertwo.com";
    String user1Customer2EmailLogin = "user1@customertwo.com";
    String user2Customer2FullName = "User2 Customer2";
    String user2Customer2Email = "user2@customertwo.com";
    String user2Customer2EmailLogin = "user2@customertwo.com";


    @BeforeClass(alwaysRun = true)
    public void setUpSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] BeforeClass: Starting SSH tunnel for DB access");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.startTunnel();
    }


    @AfterClass(alwaysRun = true)
    public void tearDownSshTunnel() {
        System.out.println("\n[FRAMEWORK] =========================================");
        System.out.println("[FRAMEWORK] AfterClass: Stopping SSH tunnel");
        System.out.println("[FRAMEWORK] =========================================");
        SSHTunnelManager.stopTunnel();
    }

    @AfterMethod
     @Step("Database Cleanup: Delete test users from database")
     public void cleanupDatabase(ITestResult result) {
         DatabaseCleanupManager.runAfterMethodCleanup(result);
     }
    
    @Test
    @Description("Test No 1: Performs end-to-end test for creating and activation, deactivation of customers and their users as a Super Admin.")
    public void create_And_Verify_CustomersAndUsers_ActivationAndDeactivation() throws Exception {
        
    	 loginPage loginPage = new loginPage(driver);
         superAdminPage superAdmin = new superAdminPage(driver);
         CustomerPage customerPage = new CustomerPage(driver, wait);
         UserPage userPage = new UserPage(driver, wait);
        System.out.println("INFO: Logging in as Super Admin...");
        loginPage.loginAs("admin10@gmail.com", "Admin@123");
        try {
            // Case 1: Conflict message shown on page
            By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
            
            if (conflictMsg.isDisplayed()) {
                System.out.println("WARNING: Session conflict message appeared: " + conflictMsg.getText());

                // Now check button text
                By continueBtnLocator = By.xpath("//button[contains(text(),'Continue Login')]");
                WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(continueBtnLocator));
                continueBtn.click();

                System.out.println("INFO: Clicked on 'Continue Login' to proceed.");
            }
        } catch (TimeoutException e) {
            // Case 2: No conflict message, normal login flow
            System.out.println("INFO: No session conflict message appeared, logged in directly.");
        }

        System.out.println("--- Starting Customer and User Creation ---");
        System.out.println("INFO: Navigating to Customers page to create Customer One.");
        
        superAdmin.openCustomers();
        customerPage.createCustomer(customerOneName, customerOneEmail, customerOneDomain);
        
        Thread.sleep(2000);

        System.out.println("INFO: Navigating to Users page to create users for Customer One.");
        // driver.findElement(By.xpath("//a[@href='/user-management/users']")).click();
        superAdmin.openUsers();
        userPage.create_User_by_Super_Admin_View(user1Customer1FullName, user1Customer1Email, customerOneName);
        superAdmin.openUsers();
        userPage.create_User_by_Super_Admin_View(user2Customer1FullName, user2Customer1Email, customerOneName);
        
        
        System.out.println("INFO: Navigating to Customers page to create Customer Two.");
        superAdmin.openCustomers();
        customerPage.createCustomer(customerTwoName, customerTwoEmail, customerTwoDomain);
      
        
        System.out.println("INFO: Navigating to Users page to create users for Customer Two.");
        driver.findElement(By.xpath("//a[@href='/user-management/users']")).click();
        superAdmin.openUsers();
        userPage.create_User_by_Super_Admin_View(user1Customer2FullName, user1Customer2Email, customerTwoName);
        superAdmin.openUsers();
        userPage.create_User_by_Super_Admin_View(user2Customer2FullName, user2Customer2Email, customerTwoName);
        System.out.println("SUCCESS: All Customers and Users Created Successfully.");
        
        // ====================================================================
        // PHASE 2: Customer Status Verification and User Status Check
        // ====================================================================
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 2: Checking Customer2 Profile and Verifying User Status Changes");
        System.out.println("=".repeat(80));
        
        // Navigate to customers section and check Customer2 profile
        System.out.println("\n[MAIN] Starting Customer2 profile verification process...");
        System.out.println("[MAIN] Customer2 Name: " + customerTwoName);
        System.out.println("[MAIN] Customer2 Email: " + customerTwoEmail);
        System.out.println("[MAIN] Customer2 Domain: " + customerTwoDomain);
        
        System.out.println("\n[MAIN] Navigating to Customers section...");
        superAdmin.openCustomers();
        System.out.println("[DEBUG] On Customers page");
        
        // Open Customer2 profile in new tab
        System.out.println("\n[MAIN] Opening Customer2 profile in new tab...");
        String parentWindow = customerPage.openCustomerProfileInNewTab(customerTwoName);
        
        // Verify customer details that were written during creation
        System.out.println("\n[MAIN] Verifying customer details match creation data...");
        customerPage.verifyCustomerDetails(customerTwoName, customerTwoEmail, customerTwoDomain);
        
        // Check and toggle customer status
        System.out.println("\n[MAIN] Checking and toggling customer status...");
        String newCustomerStatus = customerPage.checkAndToggleCustomerStatus(); // Active/Inactive
        System.out.println("[MAIN] Customer status after toggle: " + newCustomerStatus);
        
        // Close customer tab and switch back to parent
        System.out.println("\n[MAIN] Closing customer profile tab and returning to main window...");
        customerPage.closeTabAndSwitchBack(parentWindow);
        Thread.sleep(2000);
        
        // Navigate to users section and filter by company name
        System.out.println("\n[MAIN] Navigating to Users section to verify user status changes...");
        superAdmin.openUsers();
        Thread.sleep(2000);
        System.out.println("[DEBUG] On Users page");
        
        // Filter users by Customer2 company name
        System.out.println("\n[MAIN] Filtering users by Customer2 company name: " + customerTwoName);
        userPage.filterUsersByCompany(customerTwoName);
        
        // Verify ALL users under Customer2 match the customer status (dynamic check)
        System.out.println("\n[MAIN] Verifying ALL users under Customer2 match customer status...");
        System.out.println("[MAIN] Expected status for all users: " + newCustomerStatus);
        
        // This method dynamically checks ALL users in the filtered table
        boolean allUsersMatch = userPage.verifyAllUsersStatusMatchCustomer(newCustomerStatus);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL VERIFICATION RESULT:");
        System.out.println("=".repeat(80));
        System.out.println("Customer2 Status: " + newCustomerStatus);
        
        if (allUsersMatch) {
            System.out.println("\n[SUCCESS] ✅ YES: ALL users status changed according to Customer2 status.");
            System.out.println("[SUCCESS] ✅ User status synchronization verified successfully!");
        } else {
            System.out.println("\n[FAILURE] ❌ NO: Some users status did NOT match Customer2 status.");
        }
        System.out.println("\n" + "=".repeat(80));
        
        // ====================================================================
        // PHASE 3: Reactivation Process - Toggle Customer Status Again
        // ====================================================================
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PHASE 3: Reactivating Customer2 and Verifying User Reactivation");
        System.out.println("=".repeat(80));
        
        // Navigate to customers section again
        System.out.println("\n[MAIN] Starting Customer2 reactivation process...");
        System.out.println("[MAIN] Current Customer2 Status: " + newCustomerStatus);
        System.out.println("[MAIN] Will toggle to opposite status (Reactivate if Inactive, Deactivate if Active)...");
        Thread.sleep(2000);
        
        System.out.println("\n[MAIN] Navigating to Customers section for reactivation...");
        superAdmin.openCustomers();
        System.out.println("[DEBUG] On Customers page");
        
        // Open Customer2 profile in new tab again
        System.out.println("\n[MAIN] Opening Customer2 profile in new tab for reactivation...");
        String parentWindow2 = customerPage.openCustomerProfileInNewTab(customerTwoName);
        
        // Toggle customer status again (this will reactivate if it was deactivated, or deactivate if it was reactivated)
        System.out.println("\n[MAIN] Toggling customer status again (reactivation/deactivation)...");
        String reactivatedCustomerStatus = customerPage.checkAndToggleCustomerStatus();
        System.out.println("[MAIN] Customer status after second toggle: " + reactivatedCustomerStatus);
        
        // Close customer tab and switch back to parent
        System.out.println("\n[MAIN] Closing customer profile tab and returning to main window...");
        customerPage.closeTabAndSwitchBack(parentWindow2);
        Thread.sleep(2000);
        
        // Navigate to users section and filter by company name again
        System.out.println("\n[MAIN] Navigating to Users section to verify user reactivation...");
        superAdmin.openUsers();
        Thread.sleep(2000);
        System.out.println("[DEBUG] On Users page");
        
        // Filter users by Customer2 company name again
        System.out.println("\n[MAIN] Filtering users by Customer2 company name: " + customerTwoName);
        userPage.filterUsersByCompany(customerTwoName);
        
        // Verify ALL users under Customer2 match the reactivated customer status
        System.out.println("\n[MAIN] Verifying ALL users under Customer2 match reactivated customer status...");
        System.out.println("[MAIN] Expected status for all users after reactivation: " + reactivatedCustomerStatus);
        
        // This method dynamically checks ALL users in the filtered table
        boolean allUsersReactivated = userPage.verifyAllUsersStatusMatchCustomer(reactivatedCustomerStatus);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("REACTIVATION VERIFICATION RESULT:");
        System.out.println("=".repeat(80));
        System.out.println("Customer2 Status After Reactivation: " + reactivatedCustomerStatus);
        
        if (allUsersReactivated) {
            System.out.println("\n[SUCCESS] ✅ YES: ALL users status changed according to Customer2 reactivation.");
            System.out.println("[SUCCESS] ✅ User reactivation synchronization verified successfully!");
        } else {
            System.out.println("\n[FAILURE] ❌ NO: Some users status did NOT match Customer2 reactivated status.");
        }
        System.out.println("\n" + "=".repeat(80));
        
        loginPage.logout();
    	
        // driver.close();

        driver.quit();
    }
    
    /**
     * Cleanup method that runs after each test method completes.
     * This ensures database cleanup happens even if the test fails.
     */

    
    /**
     * Test No 2: Customer creates users and verifies user login
     * Flow: Super Admin creates customer → Logout → Customer logs in → Customer creates users → 
     * Verify users created → Logout → Test user logins → Database cleanup
     */
    @Test
    @Description("Test No 2: Verify customer can create users and users can login successfully")
    public void customer_Creates_Users_And_Verify_Login() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 2: Customer Creates Users and Verifies Login");
        System.out.println("=".repeat(80));
        
        loginPage loginPage = new loginPage(driver);
        superAdminPage superAdmin = new superAdminPage(driver);
        CustomerPage customerPage = new CustomerPage(driver, wait);
        UserPage userPage = new UserPage(driver, wait);
        
        // ====================================================================
        // PHASE 1: Super Admin creates customer
        // ====================================================================
        System.out.println("\n[PHASE 1] Super Admin: Creating customer...");
        System.out.println("[INFO] Logging in as Super Admin...");
        loginPage.loginAs("admin10@gmail.com", "Admin@123");
        
        try {
    By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
    WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
    if (conflictMsg.isDisplayed()) {
        System.out.println("WARNING: Session conflict message appeared: " + conflictMsg.getText());
        By continueBtnLocator = By.xpath("//button[contains(text(),'Continue Login')]");
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(continueBtnLocator));
        continueBtn.click();
        System.out.println("INFO: Clicked on 'Continue Login' to proceed.");
    }
} catch (TimeoutException e) {
    System.out.println("INFO: No session conflict message appeared, logged in directly.");
}

        System.out.println("[INFO] Navigating to Customers page to create customer...");
        superAdmin.openCustomers();
        customerPage.createCustomer(customerOneName, customerOneEmail, customerOneDomain);
        System.out.println("[SUCCESS] ✅ Customer created: " + customerOneName);
        
        // Logout from Super Admin
        System.out.println("\n[INFO] Logging out from Super Admin...");
        loginPage.logout();
        // Wait for logout to complete (login page should be visible)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
        System.out.println("[SUCCESS] ✅ Logged out from Super Admin");
        
        // ====================================================================
        // PHASE 2: Customer logs in and creates users
        // ====================================================================
        System.out.println("\n[PHASE 2] Customer: Logging in and creating users...");
        System.out.println("[INFO] Logging in as Customer: " + customerOneEmail);
        loginPage.loginAs(customerOneEmail, "Lazy@351383");
        
        try {
    By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
    WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
    if (conflictMsg.isDisplayed()) {
        System.out.println("WARNING: Session conflict message appeared: " + conflictMsg.getText());
        By continueBtnLocator = By.xpath("//button[contains(text(),'Continue Login')]");
        WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(continueBtnLocator));
        continueBtn.click();
        System.out.println("INFO: Clicked on 'Continue Login' to proceed.");
    }
} catch (TimeoutException e) {
    System.out.println("INFO: No session conflict message appeared, logged in directly.");
}

        Thread.sleep(1000);
        System.out.println("[SUCCESS] ✅ Customer logged in successfully");

        // Navigate to Users section (customer view)
        System.out.println("\n[INFO] Navigating to Users section as Customer...");
        
        // Create users as customer
        System.out.println("\n[INFO] Creating User 1: " + user1Customer1FullName);
        superAdmin.openUsers();
        userPage.create_User_by_Customer_View(user1Customer1FullName, user1Customer1Email, customerOneName);
        System.out.println("[DEBUG] On Users page as Customer");
        System.out.println("[SUCCESS] ✅ User 1 created: " + user1Customer1FullName);
        
        System.out.println("\n[INFO] Creating User 2: " + user2Customer1FullName);
        Thread.sleep(1000);
        userPage.create_User_by_Customer_View(user2Customer1FullName, user2Customer1Email, customerOneName);
        superAdmin.openUsers();
        System.out.println("[SUCCESS] ✅ User 2 created: " + user2Customer1FullName);
        
        // ====================================================================
        // PHASE 3: Verify users are created by navigating to users section
        // ====================================================================
        System.out.println("\n[PHASE 3] Verifying users are created...");
        System.out.println("[INFO] Navigating to Users section to verify created users...");
        superAdmin.openUsers();
        Thread.sleep(1000);

        // Wait for table to load
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='progressbar']")));
        Thread.sleep(1000);

        // Verify users exist in the table (name is in second column)
        System.out.println("[INFO] Checking if User 1 exists in the table...");
        By user1Locator = By.xpath("//td[2][contains(.,'" + user1Customer1FullName + "')]");
        WebElement user1Element = wait.until(ExpectedConditions.presenceOfElementLocated(user1Locator));
        Assert.assertNotNull(user1Element, "User 1 not found in the table!");
        System.out.println("[SUCCESS] ✅ User 1 found in table: " + user1Customer1FullName);

        System.out.println("[INFO] Checking if User 2 exists in the table...");
        By user2Locator = By.xpath("//td[2][contains(.,'" + user2Customer1FullName + "')]");
        WebElement user2Element = wait.until(ExpectedConditions.presenceOfElementLocated(user2Locator));
        Assert.assertNotNull(user2Element, "User 2 not found in the table!");
        System.out.println("[SUCCESS] ✅ User 2 found in table: " + user2Customer1FullName);
        
        System.out.println("[SUCCESS] ✅ Both users verified in Users section");
        
        // Logout from Customer
        System.out.println("\n[INFO] Logging out from Customer...");
        loginPage.logout();
         Thread.sleep(2000);
        System.out.println("[SUCCESS] ✅ Logged out from Customer");
        
        // ====================================================================
        // PHASE 4: Test user logins one by one
        // ====================================================================
        System.out.println("\n[PHASE 4] Testing user logins...");
        
        // Test User 1 login
        System.out.println("\n[INFO] Testing User 1 login: " + user1Customer1EmailLogin);
        loginPage.loginAs(user1Customer1EmailLogin, "Lazy@351383");
        
        try {
            By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
            if (conflictMsg.isDisplayed()) {
                By continueBtnLocator = By.xpath("//button[contains(text(),'Continue Login')]");
                WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(continueBtnLocator));
                continueBtn.click();
            }
        } catch (TimeoutException e) {
            // No conflict message
        }
        
        Thread.sleep(1000);
        String currentUrl1 = driver.getCurrentUrl();
        System.out.println("[DEBUG] User 1 login - Current URL: " + currentUrl1);
        
        if (currentUrl1.toLowerCase().contains("dashboard") || currentUrl1.toLowerCase().contains("home")) {
            System.out.println("[SUCCESS] ✅ User 1 login successful - redirected to: " + currentUrl1);
        } else {
            Assert.fail("❌ User 1 login failed. URL: " + currentUrl1);
        }
        
        loginPage.logout();
        Thread.sleep(1000);
        
        // Test User 2 login
        System.out.println("\n[INFO] Testing User 2 login: " + user2Customer1EmailLogin);
        loginPage.loginAs(user2Customer1EmailLogin, "Lazy@351383");
        
        try {
            By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
            if (conflictMsg.isDisplayed()) {
                By continueBtnLocator = By.xpath("//button[contains(text(),'Continue Login')]");
                WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(continueBtnLocator));
                continueBtn.click();
            }
        } catch (TimeoutException e) {
            // No conflict message
        }
        
        Thread.sleep(1000);
        String currentUrl2 = driver.getCurrentUrl();
        System.out.println("[DEBUG] User 2 login - Current URL: " + currentUrl2);
        
        if (currentUrl2.toLowerCase().contains("dashboard") || currentUrl2.toLowerCase().contains("home")) {
            System.out.println("[SUCCESS] ✅ User 2 login successful - redirected to: " + currentUrl2);
        } else {
            Assert.fail("❌ User 2 login failed. URL: " + currentUrl2);
        }
        
        loginPage.logout();
        Thread.sleep(1000);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 2 SUMMARY:");
        System.out.println("=".repeat(80));
        System.out.println("✅ Customer created by Super Admin");
        System.out.println("✅ Customer logged in successfully");
        System.out.println("✅ Customer created 2 users");
        System.out.println("✅ Users verified in Users section");
        System.out.println("✅ User 1 login successful");
        System.out.println("✅ User 2 login successful");
        System.out.println("=".repeat(80));
        System.out.println("[INFO] Database cleanup will run automatically after test completion");
        System.out.println("=".repeat(80));
        
        driver.quit();
    }

    /**
     * Test No 3: Dashboard only – log values on dashboard. No Candidates page, no quick filters.
     * Login as Super Admin -> assert Dashboard heading -> log LinkedIn, License, Logged in users/customers ->
     * for each Dashboard Time option, select it and log Total profiles.
     */
    @Test
    @Description("Test No 3: Super Admin dashboard – assert heading, log dashboard values only (no Candidates)")
    public void dashboard_Verify_TotalProfiles_And_CandidatesCount() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 3: Dashboard – log values only (Super Admin)");
        System.out.println("=".repeat(80));

        loginPage loginPage = new loginPage(driver);
        DashboardPage dashboardPage = new DashboardPage(driver, wait);

        System.out.println("\n[PHASE 1] Login as Super Admin and land on dashboard...");
        loginPage.loginAs("admin10@gmail.com", "Admin@123");
        try {
            By conflictMsgLocator = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            WebElement conflictMsg = wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsgLocator));
            if (conflictMsg.isDisplayed()) {
                WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]")));
                continueBtn.click();
            }
        } catch (TimeoutException e) {
            // no conflict
        }
        Thread.sleep(1500);

        dashboardPage.assertDashboardHeading();

        List<String> customerNames = dashboardPage.getCustomerDropdownOptionNames();
        if (customerNames.isEmpty()) {
            customerNames = Arrays.asList("My Dashboard");
        }

        for (String customerName : customerNames) {
            System.out.println("\n[DASHBOARD] ========== Customer: " + customerName + " ==========");
            try {
                dashboardPage.selectCustomerDropdown(customerName);
            } catch (Exception e) {
                System.out.println("[DASHBOARD] Could not select customer '" + customerName + "', skipping: " + e.getMessage());
                continue;
            }
            Thread.sleep(800);

            for (String timeOption : DashboardPage.DASHBOARD_TIME_OPTIONS) {
                System.out.println("\n[DASHBOARD] --- " + customerName + " | Time: " + timeOption + " ---");
                try {
                    dashboardPage.selectDashboardTimeDropdown(timeOption);
                    Thread.sleep(800);
                    dashboardPage.getTotalProfilesCount();
                    dashboardPage.logLinkedInProfilesCount();
                    dashboardPage.logLicenseInUse();
                    dashboardPage.logLoggedInUsers();
                    dashboardPage.logLoggedInCustomers();
                } catch (Exception e) {
                    System.out.println("[DASHBOARD] Time option '" + timeOption + "' skipped: " + e.getMessage());
                }
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 3 SUMMARY: Dashboard values logged for all customers × all time options.");
        System.out.println("=".repeat(80));
    }


}

