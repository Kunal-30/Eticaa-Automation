package testPackage;

import Pages.CandidateDetailsPage;
import Pages.CandidatesPage;
import Pages.CustomerPage;
import Pages.DashboardPage;
import Pages.UserPage;
import Pages.basePage;
import Pages.loginPage;
import Pages.superAdminPage;
import Manager.DatabaseCleanupManager;
import Utils.AdvancedFilterExcelUtil;
import Utils.CompanyMasterExcelUtil;
import Utils.LocationMasterExcelUtil;
import Utils.DesignationMasterExcelUtil;
import Utils.ExperienceMasterExcelUtil;
import Utils.CTCMasterExcelUtil;
import Utils.FilterRow;
import Utils.MixedFilterCombination;
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

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

    /** Default pagination size (candidates per page). Used to compute total pages and sample 2–3 per page. */
    private static final int PAGE_SIZE = 25;

    /**
     * For each page 1..totalPages (based on totalCount and PAGE_SIZE), navigates to that page,
     * picks 2–3 random candidate links, and calls processOne(link, pageNum) for each.
     * Only used when totalCount > 25 (pagination is visible).
     */
    private void forEachPageSampleCandidates(CandidatesPage candidatesPage, int totalCount, String parentHandle,
            BiConsumer<WebElement, Integer> processOne) throws InterruptedException {
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages <= 0) return;
        Random rnd = new Random();
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            System.out.println("[CANDIDATES] Sampling candidates from page " + pageNum + " of " + totalPages + ".");
            if (pageNum > 1) {
                candidatesPage.selectPage(pageNum);
                Thread.sleep(600);
            }
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) continue;
            int sampleCount = Math.min(2 + rnd.nextInt(2), links.size()); // 2 or 3 per page
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int j = 0; j < sampleCount; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), pageNum);
            }
        }
    }

    /**
     * Sample candidates for one applied filter. Pagination is only visible when results > 25.
     * - If totalCount > 25: use pagination, 2–3 random per page.
     * - If totalCount <= 25: no pagination; open 10–12 randomly (or all if fewer).
     */
    private void sampleCandidatesForFilter(CandidatesPage candidatesPage, int totalCount, String parentHandle,
            BiConsumer<WebElement, Integer> processOne) throws InterruptedException {
        if (totalCount <= 0) return;
        Random rnd = new Random();
        if (totalCount > PAGE_SIZE) {
            System.out.println("[CANDIDATES] Results = " + totalCount + " (> " + PAGE_SIZE + "): pagination visible. Sampling 2–3 per page.");
            forEachPageSampleCandidates(candidatesPage, totalCount, parentHandle, processOne);
        } else {
            System.out.println("[CANDIDATES] Results = " + totalCount + " (≤ " + PAGE_SIZE + "): no pagination. Sampling from single page.");
            List<WebElement> links = candidatesPage.getCandidateNameLinksOnCurrentPage();
            if (links.isEmpty()) return;
            int toOpen;
            if (links.size() >= 12) {
                toOpen = 10 + rnd.nextInt(3); // 10, 11, or 12
            } else {
                toOpen = links.size(); // open all
            }
            toOpen = Math.min(toOpen, links.size());
            System.out.println("[CANDIDATES] Opening " + toOpen + " of " + links.size() + " candidates (from Page 1).");
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int j = 0; j < toOpen; j++) {
                int idx = indices.get(j);
                List<WebElement> currentLinks = candidatesPage.getCandidateNameLinksOnCurrentPage();
                if (idx >= currentLinks.size()) continue;
                processOne.accept(currentLinks.get(idx), 1);
            }
        }
    }

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
        loginPage.loginAs("test@gmail.com", "Test@123");
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
        loginPage.loginAs("test@gmail.com", "Test@123");
        
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
        loginPage.loginAs("test@gmail.com", "Test@123");
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

    /**
     * Test No 4: Advanced Filter on Candidates – data from Excel template.
     * Creates AdvancedFilterTestData.xlsx template (columns + sample values) if missing.
     * For each Excel row: apply single filter → get result count → open each candidate in new tab →
     * assert filter value in details page; if not found, mark and log candidate name (Option 2 – test does not fail).
     */
    @Test
    @Description("Test No 4: Advanced Filter – 10 random company criteria (Past/Current/Current+Past) from CompanyMaster Excel, assert on details, mark and log")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Company_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 4: Advanced Filter – 10 random company searches (Past / Current / Current+Past) from CompanyMaster");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String companyMasterPath = Paths.get(projectRoot, "src", "test", "resources", "CompanyMaster.xlsx").toString();
        try (InputStream namesIn = getClass().getClassLoader().getResourceAsStream("company_names.txt")) {
            CompanyMasterExcelUtil.createCompanyMasterFromTextIfMissing(companyMasterPath, namesIn);
        }
        List<String> allCompanies = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("CompanyMaster.xlsx")) {
            if (excelIn != null) allCompanies = CompanyMasterExcelUtil.readCompanyNames(excelIn);
        }
        if (allCompanies.isEmpty() && Files.exists(Paths.get(companyMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(companyMasterPath))) {
                allCompanies = CompanyMasterExcelUtil.readCompanyNames(in);
            }
        }
        if (allCompanies.isEmpty()) {
            System.out.println("[ADVANCE FILTER] No companies in CompanyMaster. Ensure company_names.txt or CompanyMaster.xlsx exists.");
            return;
        }
        List<FilterRow> rows = CompanyMasterExcelUtil.getRandomCompanyFilterRows(allCompanies, 10);
        System.out.println("[ADVANCE FILTER] Using " + rows.size() + " random company criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue() + " (Type: " + r.getType() + ")");
        }

        loginPage loginPage = new loginPage(driver);
        loginPage.loginAs("test@gmail.com", "Test@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters before starting the loop
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            if (row.isEmpty()) continue;
            String filterType = row.getFilterType();
            String filterValue = row.getFilterValue();
            String type = row.getType();

            // Clear previous filter(s) and directly add the new one; DO NOT click Advance Search again
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            // Advance Search panel is already open; just set the new filter
            candidatesPage.setFilterByType(filterType, filterValue, type);

            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER] Filter: " + filterType + " = " + filterValue + " (Type: " + type + ") | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    boolean found = detailsPage.isFilterValuePresentInSection(filterType, filterValue, row.getType(), row.getIncludeExclude(), row.getNotes());
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH (value found) | Filter: " + filterType + " = " + filterValue);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED (value not found) | Filter: " + filterType + " = " + filterValue);
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER] REPORT for " + filterType + " = " + filterValue + " (Type: " + row.getType() + "): Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 4 SUMMARY: 10 random company filters completed. See logs for marked candidate names.");
        System.out.println("=".repeat(80));
    }


    @Test
    @Description("Test No. 5 : Advanced Filter – Location filter with different modes (Either/Current/Preferred/Both)")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Location_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ADVANCED FILTER: Location filter with mode dropdown (Either/Current/Preferred/Both) – random from Excel, assert on details");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String locationMasterPath = Paths.get(projectRoot, "src", "test", "resources", "LocationMaster.xlsx").toString();
        try (InputStream namesIn = getClass().getClassLoader().getResourceAsStream("location_names.txt")) {
            LocationMasterExcelUtil.createLocationMasterFromTextIfMissing(locationMasterPath, namesIn);
        }
        List<String> allLocations = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("LocationMaster.xlsx")) {
            if (excelIn != null) allLocations = LocationMasterExcelUtil.readLocationNames(excelIn);
        }
        if (allLocations.isEmpty() && Files.exists(Paths.get(locationMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(locationMasterPath))) {
                allLocations = LocationMasterExcelUtil.readLocationNames(in);
            }
        }
        if (allLocations.isEmpty()) {
            System.out.println("[ADVANCE FILTER][LOCATION] No locations in LocationMaster. Ensure location_names.txt or LocationMaster.xlsx exists.");
            return;
        }
        List<FilterRow> rows = LocationMasterExcelUtil.getRandomLocationFilterRows(allLocations, 10);
        System.out.println("[ADVANCE FILTER][LOCATION] Using " + rows.size() + " random location criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue() + " (Mode: " + r.getType() + ")");
        }

        loginPage loginPage = new loginPage(driver);
        loginPage.loginAs("test@gmail.com", "Test@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            if (row.isEmpty()) continue;
            String mode = row.getType();
            String city = row.getFilterValue();

            // Clear previous filters and directly add the new one; Advance Search is already open
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            // For Both, use same city for current and preferred; for others, preferred is null
            boolean isBoth = mode != null && mode.toLowerCase().startsWith("both");
            String preferredCity = isBoth ? city : null;
            candidatesPage.setLocationFilterWithMode(mode, city, preferredCity);

            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][LOCATION] Filter: City = " + city + ", Mode = " + mode + " | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    boolean found = detailsPage.isFilterValuePresentInSection("Location", city, mode, "Include", "");
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][LOCATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH | City = " + city);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][LOCATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED | City = " + city);
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][LOCATION] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][LOCATION] REPORT for City=" + city + ", Mode=" + mode
                + ": Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ADVANCED FILTER (Location modes from Excel) SUMMARY: Completed.");
        System.out.println("=".repeat(80));
    }


    /**
     * TEST NO 6:
     * Advanced Filter – Designation filters from Excel, assert on candidate details, mark and log candidate names.
     * For each Excel row (FilterType=Designation): apply filter → get result count → open every candidate on current page in new tab →
     * assert designation (text before " at " in current row); if not found, mark and log.
     */
    @Test
    @Description("Test No 6: Advanced Filter – Designation filters from Excel, assert on candidate details, mark and log")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Designation_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 6: Advanced Filter (DesignationMaster.xlsx, random Designation filters, mark + log candidate name)");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String designationMasterPath = Paths.get(projectRoot, "src", "test", "resources", "DesignationMaster.xlsx").toString();
        // Ensure template exists; you have already filled DesignationMaster.xlsx
        DesignationMasterExcelUtil.createEmptyTemplateIfMissing(designationMasterPath);

        List<String> allDesignations = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("DesignationMaster.xlsx")) {
            if (excelIn != null) allDesignations = DesignationMasterExcelUtil.readDesignationNames(excelIn);
        }
        if (allDesignations.isEmpty() && Files.exists(Paths.get(designationMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(designationMasterPath))) {
                allDesignations = DesignationMasterExcelUtil.readDesignationNames(in);
            }
        }
        if (allDesignations.isEmpty()) {
            System.out.println("[ADVANCE FILTER][DESIGNATION] No Designations in DesignationMaster. Ensure DesignationMaster.xlsx has data.");
            return;
        }
        List<FilterRow> rows = DesignationMasterExcelUtil.getRandomDesignationFilterRows(allDesignations, 10);
        System.out.println("[ADVANCE FILTER][DESIGNATION] Using " + rows.size() + " random Designation criteria:");
        for (FilterRow r : rows) {
            System.out.println("  - " + r.getFilterValue());
        }

        loginPage loginPage = new loginPage(driver);
        // loginPage.loginAs("test@gmail.com", "Test@123");
        loginPage.loginAs("admin@eticaa.com", "Admin@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        // Open Advance Search once and clear any existing filters before starting the loop
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (FilterRow row : rows) {
            String designation = row.getFilterValue();
            if (designation == null || designation.trim().isEmpty()) continue;

            // Clear previous filters and directly add the new one; Advance Search is already open
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            candidatesPage.setFilterByType("Designation", designation);

            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][DESIGNATION] Filter: Designation = " + designation + " | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    boolean found = detailsPage.isFilterValuePresentInSection("Designation", designation, row.getType(), row.getIncludeExclude(), row.getNotes());
                    if (found) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MATCH | Designation = " + designation);
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate = " + candidateName + " (from Page " + pageNum + ") | MARKED | Designation not found for filter = " + designation);
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][DESIGNATION] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][DESIGNATION] REPORT for Designation = " + designation
                + ": Total profiles = " + totalCount
                + ", Matched = " + matchedCandidateNames.size()
                + ", Marked = " + markedCandidateNames.size()
                + ". Matched: " + matchedCandidateNames
                + ", Marked: " + markedCandidateNames);
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 6 SUMMARY: Designation filters (from Excel) run completed. See logs for marked candidate names.");
        System.out.println("=".repeat(80));
    }

    /**
     * TEST NO 7:
     * Advanced Filter – Experience (min/max years) ranges from ExperienceMaster.xlsx, log candidate count per range.
     */
    @Test
    @Description("Test No 7: Advanced Filter – Experience ranges (min/max years) from Excel, log counts")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_Experience_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 7: Advanced Filter – Experience ranges (min/max years) from ExperienceMaster.xlsx");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String experienceMasterPath = Paths.get(projectRoot, "src", "test", "resources", "ExperienceMaster.xlsx").toString();
        ExperienceMasterExcelUtil.createEmptyTemplateIfMissing(experienceMasterPath);

        List<int[]> allRanges = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("ExperienceMaster.xlsx")) {
            if (excelIn != null) allRanges = ExperienceMasterExcelUtil.readExperienceRanges(excelIn);
        }
        if (allRanges.isEmpty() && Files.exists(Paths.get(experienceMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(experienceMasterPath))) {
                allRanges = ExperienceMasterExcelUtil.readExperienceRanges(in);
            }
        }
        if (allRanges.isEmpty()) {
            System.out.println("[ADVANCE FILTER][EXPERIENCE] No valid ranges in ExperienceMaster (empty rows skipped). Ensure MinYears, MaxYears are filled.");
            return;
        }
        List<int[]> ranges = ExperienceMasterExcelUtil.getRandomExperienceRanges(allRanges, 10);
        System.out.println("[ADVANCE FILTER][EXPERIENCE] Using " + ranges.size() + " experience range(s) from Excel (empty cells skipped):");
        for (int[] r : ranges) {
            System.out.println("  - " + r[0] + " to " + r[1] + " years");
        }

        loginPage loginPage = new loginPage(driver);
        loginPage.loginAs("test@gmail.com", "Test@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();

        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            System.out.println("\n" + "=".repeat(60));
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setExperienceFilter(min, max);
            int totalCount = candidatesPage.getResultCount();
            System.out.println("[ADVANCE FILTER][EXPERIENCE] Filter: " + min + " to " + max + " years (from Excel) | Total results: " + totalCount);
            System.out.println("=".repeat(60));
            Assert.assertTrue(totalCount >= 0, "Result count should be displayed (>= 0) for Experience " + min + "–" + max + " years");
            String parentHandle = driver.getWindowHandle();
            List<String> openedCandidateNames = new ArrayList<>();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    Assert.assertNotNull(candidateName, "Candidate name should be visible on details page");
                    Assert.assertFalse(candidateName.isEmpty(), "Candidate name should not be empty");
                    openedCandidateNames.add(candidateName);
                    String expText = detailsPage.getExperienceText();
                    Double expYears = detailsPage.getExperienceYears();
                    if (expYears != null) {
                        Assert.assertTrue(expYears >= min && expYears <= max,
                            "Experience " + expText + " (" + expYears + " y) should be between " + min + " and " + max + " years for " + candidateName);
                        System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate = " + candidateName + " (from Page " + pageNum + ") | Experience: " + expText + " (" + expYears + " y) in range [" + min + "–" + max + "]");
                    } else {
                        System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate = " + candidateName + " (from Page " + pageNum + ") | Experience text: " + (expText.isEmpty() ? "(not found)" : expText) + " | Filter: " + min + "–" + max + " years");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][EXPERIENCE] Candidate (from Page " + pageNum + ") | Error opening/asserting: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][EXPERIENCE] REPORT for " + min + "–" + max + " years: Total = " + totalCount + ", Opened = " + openedCandidateNames.size() + ". " + openedCandidateNames);
            System.out.println("=".repeat(60) + "\n");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 7 SUMMARY: Experience range filters (from Excel) completed. Same profile-open pattern as Location.");
        System.out.println("=".repeat(80));
    }

    /**
     * Test No 8: Add MIXED filters (Company + Location + Designation + Experience + Current CTC + Expected CTC + Notice Period)
     * and clear in a loop. NO opening of candidates.
     * Runs until you manually stop the test (Ctrl+F2 in Eclipse or stop button).
     */
    private static final int FILTER_LOOP_MAX_ITERATIONS = 1000;

    @Test
    @Description("Test No 8: Mixed filters (all up to Notice Period) add + clear loop (no candidate opening); stop manually when done")
    public void filterAddAndClearLoop_NoCandidateOpening() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8: MIXED filter (Company + Location + Designation + Experience + Current CTC + Expected CTC + Notice Period) add + clear loop.");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        List<String> allCompanies = loadCompanyNames(projectRoot);
        List<String> allLocations = loadLocationNames(projectRoot);
        List<String> allDesignations = loadDesignationNames(projectRoot);
        List<int[]> allExperienceRanges = loadExperienceRanges(projectRoot);

        if (allCompanies.isEmpty()) allCompanies = Arrays.asList("Google", "Microsoft", "Amazon", "TCS", "Infosys");
        if (allLocations.isEmpty()) allLocations = Arrays.asList("Bangalore", "Mumbai", "Delhi", "Chennai", "Hyderabad");
        if (allDesignations.isEmpty()) allDesignations = Arrays.asList("Software Engineer", "Manager", "Developer", "Lead");
        if (allExperienceRanges.isEmpty()) allExperienceRanges = Arrays.asList(new int[]{0, 2}, new int[]{2, 5}, new int[]{5, 10}, new int[]{10, 15});

        List<int[]> ctcRanges = Arrays.asList(new int[]{0, 5}, new int[]{5, 10}, new int[]{10, 20}, new int[]{20, 40}, new int[]{40, 80});
        List<String> noticePeriodOptions = Arrays.asList("Immediate Joiner", "0 - 15 days", "1 month", "2 months", "3 months");

        loginPage loginPage = new loginPage(driver);
        // loginPage.loginAs("test@gmail.com", "Test@123");
        loginPage.loginAs("admin10@gmail.com", "Admin@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (org.openqa.selenium.TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        Random rnd = new Random();
        String[] companyTypes = {"Current+Past", "Current", "Past"};
        String[] locationModes = {"Either (Current or Preferred)", "Current Location", "Preferred Location", "Both (Current and Preferred)"};

        int iteration = 0;
        while (iteration < FILTER_LOOP_MAX_ITERATIONS) {
            iteration++;
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            StringBuilder applied = new StringBuilder();
            try {
                // 1. Company
                String company = allCompanies.get(rnd.nextInt(allCompanies.size()));
                String type = companyTypes[rnd.nextInt(companyTypes.length)];
                candidatesPage.setFilterByType("Company", company, type);
                applied.append("Company=").append(company).append("(").append(type).append(") ");

                // 2. Location
                String location = allLocations.get(rnd.nextInt(allLocations.size()));
                String mode = locationModes[rnd.nextInt(locationModes.length)];
                boolean isBoth = mode.toLowerCase().contains("both");
                candidatesPage.setLocationFilterWithMode(mode, location, isBoth ? location : null);
                applied.append("Location=").append(location).append(" ");

                // 3. Designation
                String designation = allDesignations.get(rnd.nextInt(allDesignations.size()));
                candidatesPage.setFilterByType("Designation", designation);
                applied.append("Designation=").append(designation).append(" ");

                // 4. Experience
                int[] expRange = allExperienceRanges.get(rnd.nextInt(allExperienceRanges.size()));
                candidatesPage.setExperienceFilter(expRange[0], expRange[1]);
                applied.append("Exp=").append(expRange[0]).append("-").append(expRange[1]).append("y ");

                // 5. Current CTC
                int[] ctcRange = ctcRanges.get(rnd.nextInt(ctcRanges.size()));
                candidatesPage.setCurrentCTCFilter(ctcRange[0], ctcRange[1]);
                applied.append("CurCTC=").append(ctcRange[0]).append("-").append(ctcRange[1]).append("L ");

                // 6. Expected CTC
                int[] expCtcRange = ctcRanges.get(rnd.nextInt(ctcRanges.size()));
                candidatesPage.setExpectedCTCFilter(expCtcRange[0], expCtcRange[1]);
                applied.append("ExpCTC=").append(expCtcRange[0]).append("-").append(expCtcRange[1]).append("L ");

                // 7. Notice Period
                String noticePeriod = noticePeriodOptions.get(rnd.nextInt(noticePeriodOptions.size()));
                candidatesPage.setNoticePeriodFilter(noticePeriod);
                applied.append("Notice=").append(noticePeriod);

                int count = candidatesPage.getResultCount();
                System.out.println("[FILTER LOOP #" + iteration + "] MIXED | " + applied + " | Results=" + count);
            } catch (Exception e) {
                System.out.println("[FILTER LOOP #" + iteration + "] Error applying mixed filter: " + e.getMessage());
            }

            Thread.sleep(800);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("FILTER LOOP ended after " + iteration + " iterations (or manually stopped).");
        System.out.println("=".repeat(80));
    }

    /**
     * Test No 8a: Current CTC filter – ranges from CTCMaster.xlsx.
     * For each range: apply filter → get result count → open 2–3 profiles per page across all pages → log/assert CTC in range.
     */
    @Test
    @Description("Test No 9: Advanced Filter – Current CTC ranges from Excel, sample 2–3 per page, open profiles, log and assert")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_CurrentCTC_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8a: Advanced Filter – Current CTC ranges from CTCMaster.xlsx (sample 2–3 per page, open profiles)");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String ctcMasterPath = Paths.get(projectRoot, "src", "test", "resources", "CTCMaster.xlsx").toString();
        CTCMasterExcelUtil.createEmptyTemplateIfMissing(ctcMasterPath);

        List<int[]> allRanges = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("CTCMaster.xlsx")) {
            if (excelIn != null) allRanges = CTCMasterExcelUtil.readCTCRanges(excelIn);
        }
        if (allRanges.isEmpty() && Files.exists(Paths.get(ctcMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(ctcMasterPath))) {
                allRanges = CTCMasterExcelUtil.readCTCRanges(in);
            }
        }
        if (allRanges.isEmpty()) {
            System.out.println("[ADVANCE FILTER][CURRENT CTC] No valid ranges in CTCMaster. Ensure MinLakhs, MaxLakhs are filled.");
            return;
        }
        List<int[]> ranges = CTCMasterExcelUtil.getRandomCTCRanges(allRanges, 10);
        System.out.println("[ADVANCE FILTER][CURRENT CTC] Using " + ranges.size() + " range(s) from Excel:");
        for (int[] r : ranges) System.out.println("  - " + r[0] + " to " + r[1] + " Lakhs");

        loginPage loginPage = new loginPage(driver);
        loginPage.loginAs("test@gmail.com", "Test@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setCurrentCTCFilter(min, max);
            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][CURRENT CTC] Filter: " + min + " to " + max + " Lakhs | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    Double ctcLakhs = detailsPage.getCurrentCTCLakhs();
                    if (ctcLakhs != null && ctcLakhs >= min && ctcLakhs <= max) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][CURRENT CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MATCH | CTC=" + ctcLakhs + "L in [" + min + "-" + max + "]");
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][CURRENT CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MARKED | CTC=" + (ctcLakhs != null ? ctcLakhs + "L" : "not found") + " | Filter: " + min + "-" + max + "L");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][CURRENT CTC] Candidate (Page " + pageNum + ") | Error: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][CURRENT CTC] REPORT for " + min + "-" + max + "L: Total=" + totalCount + ", Matched=" + matchedCandidateNames.size() + ", Marked=" + markedCandidateNames.size());
            System.out.println("=".repeat(60) + "\n");
        }
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8a SUMMARY: Current CTC filter (from Excel) completed.");
        System.out.println("=".repeat(80));
    }

    /**
     * Test No 8b: Expected CTC filter – ranges from CTCMaster.xlsx.
     * For each range: apply filter → get result count → open 2–3 profiles per page across all pages → log/assert ECTC in range.
     */
    @Test
    @Description("Test No 10: Advanced Filter – Expected CTC ranges from Excel, sample 2–3 per page, open profiles, log and assert")
    public void advancedFilter_FromExcel_AssertAndMarkCandidates_for_ExpectedCTC_Filter() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8b: Advanced Filter – Expected CTC ranges from CTCMaster.xlsx (sample 2–3 per page, open profiles)");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String ctcMasterPath = Paths.get(projectRoot, "src", "test", "resources", "CTCMaster.xlsx").toString();
        CTCMasterExcelUtil.createEmptyTemplateIfMissing(ctcMasterPath);

        List<int[]> allRanges = new ArrayList<>();
        try (InputStream excelIn = getClass().getClassLoader().getResourceAsStream("CTCMaster.xlsx")) {
            if (excelIn != null) allRanges = CTCMasterExcelUtil.readCTCRanges(excelIn);
        }
        if (allRanges.isEmpty() && Files.exists(Paths.get(ctcMasterPath))) {
            try (InputStream in = Files.newInputStream(Paths.get(ctcMasterPath))) {
                allRanges = CTCMasterExcelUtil.readCTCRanges(in);
            }
        }
        if (allRanges.isEmpty()) {
            System.out.println("[ADVANCE FILTER][EXPECTED CTC] No valid ranges in CTCMaster. Ensure MinLakhs, MaxLakhs are filled.");
            return;
        }
        List<int[]> ranges = CTCMasterExcelUtil.getRandomCTCRanges(allRanges, 10);
        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Using " + ranges.size() + " range(s) from Excel:");
        for (int[] r : ranges) System.out.println("  - " + r[0] + " to " + r[1] + " Lakhs");

        loginPage loginPage = new loginPage(driver);
        loginPage.loginAs("test@gmail.com", "Test@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();
        candidatesPage.clickClearFiltersIfPresent();

        for (int[] range : ranges) {
            int min = range[0];
            int max = range[1];
            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);
            candidatesPage.setExpectedCTCFilter(min, max);
            int totalCount = candidatesPage.getResultCount();
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[ADVANCE FILTER][EXPECTED CTC] Filter: " + min + " to " + max + " Lakhs | Total results: " + totalCount);
            System.out.println("=".repeat(60));

            List<String> matchedCandidateNames = new ArrayList<>();
            List<String> markedCandidateNames = new ArrayList<>();
            String parentHandle = driver.getWindowHandle();

            sampleCandidatesForFilter(candidatesPage, totalCount, parentHandle, (link, pageNum) -> {
                try {
                    candidatesPage.openCandidateProfile(link);
                    Thread.sleep(1500);
                    Set<String> handles = driver.getWindowHandles();
                    for (String h : handles) {
                        if (!h.equals(parentHandle)) {
                            driver.switchTo().window(h);
                            break;
                        }
                    }
                    CandidateDetailsPage detailsPage = new CandidateDetailsPage(driver, wait);
                    String candidateName = detailsPage.getCandidateName();
                    Double ctcLakhs = detailsPage.getExpectedCTCLakhs();
                    if (ctcLakhs != null && ctcLakhs >= min && ctcLakhs <= max) {
                        matchedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MATCH | ECTC=" + ctcLakhs + "L in [" + min + "-" + max + "]");
                    } else {
                        markedCandidateNames.add(candidateName);
                        System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate = " + candidateName + " (Page " + pageNum + ") | MARKED | ECTC=" + (ctcLakhs != null ? ctcLakhs + "L" : "not found") + " | Filter: " + min + "-" + max + "L");
                    }
                    driver.close();
                    driver.switchTo().window(parentHandle);
                    Thread.sleep(500);
                } catch (Exception e) {
                    try { driver.switchTo().window(parentHandle); } catch (Exception e2) { }
                    System.out.println("[ADVANCE FILTER][EXPECTED CTC] Candidate (Page " + pageNum + ") | Error: " + e.getMessage());
                }
            });

            System.out.println("[ADVANCE FILTER][EXPECTED CTC] REPORT for " + min + "-" + max + "L: Total=" + totalCount + ", Matched=" + matchedCandidateNames.size() + ", Marked=" + markedCandidateNames.size());
            System.out.println("=".repeat(60) + "\n");
        }
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 8b SUMMARY: Expected CTC filter (from Excel) completed.");
        System.out.println("=".repeat(80));
    }

    /**
     * Test No 9: Variable filter combinations from Excel – 2, 3, or 4 filter types.
     * Sometimes 1 company, sometimes 2 companies; sometimes 1 designation, sometimes 2 designations.
     * Location, Experience – 1 each when included. Data from CompanyMaster, DesignationMaster, LocationMaster, ExperienceMaster.
     */
    @Test
    @Description("Test No 11: Variable filter combinations from Excel – 2–4 filter types, sometimes 2 companies or 2 designations")
    public void advancedFilter_FromExcel_VariableFilterCombinations() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 9: Variable filter combinations (2–4 types) from Excel – Company, Designation, Location, Experience");
        System.out.println("=".repeat(80));

        String projectRoot = System.getProperty("user.dir");
        String resourcesDir = Paths.get(projectRoot, "src", "test", "resources").toString();

        try (InputStream companyIn = getClass().getClassLoader().getResourceAsStream("company_names.txt")) {
            CompanyMasterExcelUtil.createCompanyMasterFromTextIfMissing(
                Paths.get(resourcesDir, "CompanyMaster.xlsx").toString(), companyIn);
        }
        try (InputStream locIn = getClass().getClassLoader().getResourceAsStream("location_names.txt")) {
            LocationMasterExcelUtil.createLocationMasterFromTextIfMissing(
                Paths.get(resourcesDir, "LocationMaster.xlsx").toString(), locIn);
        }
        DesignationMasterExcelUtil.createEmptyTemplateIfMissing(Paths.get(resourcesDir, "DesignationMaster.xlsx").toString());
        ExperienceMasterExcelUtil.createEmptyTemplateIfMissing(Paths.get(resourcesDir, "ExperienceMaster.xlsx").toString());

        List<String> allCompanies = loadCompanyNames(projectRoot);
        List<String> allDesignations = loadDesignationNames(projectRoot);
        List<String> allLocations = loadLocationNames(projectRoot);
        List<int[]> allExperienceRanges = loadExperienceRanges(projectRoot);

        if (allCompanies.isEmpty() && allDesignations.isEmpty() && allLocations.isEmpty() && allExperienceRanges.isEmpty()) {
            System.out.println("[FILTER COMBO] No data in any Excel sheet (CompanyMaster, DesignationMaster, LocationMaster, ExperienceMaster). Skipping test.");
            return;
        }

        loginPage loginPage = new loginPage(driver);
        // loginPage.loginAs("test@gmail.com", "Test@123");
        loginPage.loginAs("admin10@gmail.com", "Admin@123");
        try {
            By conflictMsg = By.xpath("//div[contains(text(),'You have logged in on another device')]");
            if (wait.until(ExpectedConditions.presenceOfElementLocated(conflictMsg)).isDisplayed()) {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Continue Login')]"))).click();
            }
        } catch (TimeoutException e) { }
        Thread.sleep(1500);

        CandidatesPage candidatesPage = new CandidatesPage(driver, wait);
        candidatesPage.candidatesPage_Link();
        candidatesPage.openAdvanceSearch();

        int numIterations = 15;

        for (int i = 1; i <= numIterations; i++) {
            MixedFilterCombination combo = MixedFilterCombination.build(allCompanies, allDesignations, allLocations, allExperienceRanges);
            if (combo.totalFilterCount() == 0) continue;

            candidatesPage.clickClearFiltersIfPresent();
            Thread.sleep(500);

            StringBuilder applied = new StringBuilder();
            try {
                for (FilterRow row : combo.companyRows) {
                    candidatesPage.setFilterByType("Company", row.getFilterValue(), row.getType());
                    applied.append("Company=").append(row.getFilterValue()).append("(").append(row.getType()).append(") ");
                    Thread.sleep(400);
                }
                for (FilterRow row : combo.designationRows) {
                    candidatesPage.setFilterByType("Designation", row.getFilterValue());
                    applied.append("Designation=").append(row.getFilterValue()).append(" ");
                    Thread.sleep(400);
                }
                if (combo.hasLocation && combo.locationRow != null) {
                    String mode = combo.locationRow.getType();
                    boolean isBoth = mode != null && mode.toLowerCase().startsWith("both");
                    String preferred = isBoth ? combo.locationRow.getFilterValue() : null;
                    candidatesPage.setLocationFilterWithMode(mode, combo.locationRow.getFilterValue(), preferred);
                    applied.append("Location=").append(combo.locationRow.getFilterValue()).append(" ");
                }
                if (combo.hasExperience) {
                    candidatesPage.setExperienceFilter(combo.expMin, combo.expMax);
                    applied.append("Exp=").append(combo.expMin).append("-").append(combo.expMax).append("y");
                }

                int count = candidatesPage.getResultCount();
                System.out.println("[FILTER COMBO #" + i + "] " + combo.totalFilterCount() + " filters | " + applied + " | Results=" + count);
            } catch (Exception e) {
                System.out.println("[FILTER COMBO #" + i + "] Error: " + e.getMessage());
            }
            Thread.sleep(600);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST NO 9 SUMMARY: " + numIterations + " variable filter combinations completed.");
        System.out.println("=".repeat(80));
    }

    private List<String> loadCompanyNames(String projectRoot) {
        List<String> list = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("CompanyMaster.xlsx")) {
            if (in != null) list = CompanyMasterExcelUtil.readCompanyNames(in);
        } catch (Exception e) { }
        if (list.isEmpty() && Files.exists(Paths.get(projectRoot, "src", "test", "resources", "CompanyMaster.xlsx"))) {
            try (InputStream in = Files.newInputStream(Paths.get(projectRoot, "src", "test", "resources", "CompanyMaster.xlsx"))) {
                list = CompanyMasterExcelUtil.readCompanyNames(in);
            } catch (Exception e) { }
        }
        return list;
    }

    private List<String> loadLocationNames(String projectRoot) {
        List<String> list = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("LocationMaster.xlsx")) {
            if (in != null) list = LocationMasterExcelUtil.readLocationNames(in);
        } catch (Exception e) { }
        if (list.isEmpty() && Files.exists(Paths.get(projectRoot, "src", "test", "resources", "LocationMaster.xlsx"))) {
            try (InputStream in = Files.newInputStream(Paths.get(projectRoot, "src", "test", "resources", "LocationMaster.xlsx"))) {
                list = LocationMasterExcelUtil.readLocationNames(in);
            } catch (Exception e) { }
        }
        return list;
    }

    private List<String> loadDesignationNames(String projectRoot) {
        List<String> list = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("DesignationMaster.xlsx")) {
            if (in != null) list = DesignationMasterExcelUtil.readDesignationNames(in);
        } catch (Exception e) { }
        if (list.isEmpty() && Files.exists(Paths.get(projectRoot, "src", "test", "resources", "DesignationMaster.xlsx"))) {
            try (InputStream in = Files.newInputStream(Paths.get(projectRoot, "src", "test", "resources", "DesignationMaster.xlsx"))) {
                list = DesignationMasterExcelUtil.readDesignationNames(in);
            } catch (Exception e) { }
        }
        return list;
    }

    private List<int[]> loadExperienceRanges(String projectRoot) {
        List<int[]> list = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("ExperienceMaster.xlsx")) {
            if (in != null) list = ExperienceMasterExcelUtil.readExperienceRanges(in);
        } catch (Exception e) { }
        if (list.isEmpty() && Files.exists(Paths.get(projectRoot, "src", "test", "resources", "ExperienceMaster.xlsx"))) {
            try (InputStream in = Files.newInputStream(Paths.get(projectRoot, "src", "test", "resources", "ExperienceMaster.xlsx"))) {
                list = ExperienceMasterExcelUtil.readExperienceRanges(in);
            } catch (Exception e) { }
        }
        return list;
    }

}

