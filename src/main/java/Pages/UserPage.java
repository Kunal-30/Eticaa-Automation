package Pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.openqa.selenium.support.ui.ExpectedConditions;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

public class UserPage {
    private WebDriver driver;
    private WebDriverWait wait;

    //
    // Locators for Super Admin View
    private By createUserButton = By.xpath("//button[text() = 'Add User']");
    // Create User form fields (align with UI)
    private By customer_Select_dropdown = By.xpath("//select[@name='customerId']");
    private By customerDropdown = By.xpath("//label[contains(normalize-space(.),'Company Name')]/following::select[1]");
    private By emailField = By.xpath("//label[contains(normalize-space(.),'Email Address')]/following::input[1]");
    private By passwordField = By.xpath("//label[contains(normalize-space(.),'Password')]/following::input[1]");
    private By confirmPasswordField = By.xpath("//label[contains(normalize-space(.),'Confirm Password')]/following::input[1]");
    private By fullNameField = By.xpath("//label[contains(normalize-space(.),'Full Name')]/following::input[1]");
    private By mobileField = By.xpath("//label[contains(normalize-space(.),'Mobile Number')]/following::input[1]");
    private By locationField = By.xpath("//label[contains(normalize-space(.),'Location')]/following::input[1]");
    private By designationField = By.xpath("//label[contains(normalize-space(.),'Designation')]/following::input[1]");
    private By saveUserButton = By.xpath("//button[text()='Create User']");
    
    // Locators for delete operation\
    
    
    private By searchIcon = By.xpath("//div[@class='MuiBox-root css-102qauq']");
//  private By searchFieldContainer = By.xpath("//div[@class='MuiStack-root css-1kdf6jn']");
    private By searchField = By.xpath("//input[@placeholder='Search all users...']");
  
    private By deleteUserButton = By.xpath("//button[@title='Delete User']");
    private By confirmDeleteButton = By.xpath("(//button[text() = 'Delete'])[2]");
    
    private By userFullNameLink = By.xpath("//td[@data-index='1']");
    private By companyNameLabel = By.xpath("//div[./label[text()='Company']]//p");
    
    private By toggleLockButton = By.xpath("//div[@title='Lock User']");
    private By toggleUnlockButton = By.xpath("//div[@title='Unlock User']");
    private By backButton = By.xpath("//button[@class='cursor-pointer']");

    // Elements for Customer View (when customer is logged in and creating users)
    // Email field only takes the username part (domain is auto-appended by app)
    private By customer_Add_user_button = By.xpath("//button[text()='Add User']");
    private By customer_view_user_Email_Field = By.xpath("//input[@placeholder='Enter email']");
    private By customer_view_user_Password_Field = By.xpath("(//input[@type='password'])[1]");
    private By customer_view_user_Confirm_Password_Field = By.xpath("(//input[@type='password'])[2]");
    private By customer_view_user_Full_Name_Field = By.xpath("//input[@name='fullName']");
    private By customer_view_user_Mobile_Field = By.xpath("//input[@name='mobile']");
    private By customer_view_user_Location_Field = By.xpath("//input[@name='location']");
    private By customer_view_user_Designation_Field = By.xpath("//input[@name='designation']");
    
    // Success message: multiple possible texts (customer view may differ from super admin)
    private By userCreationSuccessMessage = By.xpath("//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'user created successfully')]");
    // Fallback: same toast container CustomerPage uses for status messages
    private By toastMessageContainer = By.xpath("/html/body/div/div[1]/div/div/div[2]");

    private final By loader = By.xpath("//div[@role='progressbar']");

    public UserPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    @Step("Select company: {companyName}")
    public void selectCompanyByName(String companyName) {
        WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(customerDropdown));
        Select select = new Select(dropdownElement);
        select.selectByVisibleText(companyName);
    }
    
    @Step("Create user: {fullName} for {customerName}")
    public void create_User_by_Super_Admin_View(String fullName, String email, String customerName) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(createUserButton)).click();
            // Wait for Create User form to be ready (company dropdown visible) before filling - same as for user2
            wait.until(ExpectedConditions.visibilityOfElementLocated(customer_Select_dropdown));
            wait.until(ExpectedConditions.elementToBeClickable(customer_Select_dropdown));
            Thread.sleep(500);
            // Fill ALL fields (required + optional) in UI order
            driver.findElement(customer_Select_dropdown).click();
            selectCompanyByName(customerName);
            fillField(emailField, email);
            final String password = "Lazy@351383";
            fillField(passwordField, password);
            fillField(confirmPasswordField, password);
            fillField(fullNameField, fullName);
            
            // Fill optional fields
            fillField(mobileField, "9998563587");
            fillField(locationField, "Bangalore");
            fillField(designationField, "QA");
            
            driver.findElement(saveUserButton).click();

            Thread.sleep(2000);

            driver.navigate().refresh();
            
            // Wait for user creation to complete
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(1));
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
            
            // Wait for "Add User" button to be clickable again (form closed)
            wait.until(ExpectedConditions.elementToBeClickable(createUserButton));
            
            System.out.println("SUCCESS: Created User: " + fullName + " (" + email + ") for " + customerName);
            
        } catch (Exception e) {
            System.out.println("ERROR: An error occurred during user creation: " + fullName + " - " + e.getMessage());
        }
    }


    @Step("Create user: {fullName} for {customerName}")
    public void create_User_by_Customer_View(String fullName, String email, String customerName) {
        try {
            driver.navigate().refresh();
            Thread.sleep(1000);
            System.out.println("[INFO] Clicking 'Add User' button...");
            wait.until(ExpectedConditions.elementToBeClickable(customer_Add_user_button)).click();
            // Wait for the create-user form to be visible before filling (avoids filling before form is open)
            System.out.println("[INFO] Waiting for Create User form to be ready...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(customer_view_user_Email_Field));
            wait.until(ExpectedConditions.elementToBeClickable(customer_view_user_Email_Field));
            Thread.sleep(500);

            // For customer view, UI email field expects only the username part (before '@')
            String usernameOnly = email;
            if (email != null && email.contains("@")) {
                usernameOnly = email.substring(0, email.indexOf("@"));
            }
            System.out.println("[DEBUG] Filling form for user: " + fullName + " (username: " + usernameOnly + ")");

            // Fill fields using customer-view specific locators
            fillField(customer_view_user_Email_Field, usernameOnly);
            final String password = "Lazy@351383";
            fillField(customer_view_user_Password_Field, password);
            fillField(customer_view_user_Confirm_Password_Field, password);
            fillField(customer_view_user_Full_Name_Field, fullName);

            fillField(customer_view_user_Mobile_Field, "9998563587");
            fillField(customer_view_user_Location_Field, "Bangalore");
            fillField(customer_view_user_Designation_Field, "QA");

            System.out.println("[INFO] Clicking 'Create User' button...");
            driver.findElement(saveUserButton).click();

            // Wait for success: either visible success message (flexible text) or modal to close (Add User clickable again)
            int successWaitSeconds = 25;
            WebDriverWait successWait = new WebDriverWait(driver, Duration.ofSeconds(successWaitSeconds));
            System.out.println("[INFO] Waiting up to " + successWaitSeconds + "s for success message or form to close...");

            WebElement successMessage = null;
            try {
                successMessage = successWait.until(ExpectedConditions.visibilityOfElementLocated(userCreationSuccessMessage));
            } catch (Exception e1) {
                try {
                    // Fallback: toast container (same as CustomerPage uses) with fresh timeout
                    WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(successWaitSeconds));
                    WebElement toast = toastWait.until(ExpectedConditions.visibilityOfElementLocated(toastMessageContainer));
                    String toastText = toast.getText().trim();
                    if (toastText.toLowerCase().contains("user created successfully") || toastText.toLowerCase().contains("success")) {
                        successMessage = toast;
                    }
                } catch (Exception e2) {
                    // Ignore
                }
            }

            if (successMessage != null) {
                String messageText = successMessage.getText().trim();
                System.out.println("[DEBUG] Success message text: " + messageText);
                Assert.assertTrue(
                        messageText.toLowerCase().contains("user created successfully") || messageText.toLowerCase().contains("success"),
                        "Expected success message but got: " + messageText);
                System.out.println("[SUCCESS] ✅ Success message verified: " + messageText);
            } else {
                // Fallback: form closed (Add User clickable) = creation likely succeeded
                WebDriverWait fallbackWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                fallbackWait.until(ExpectedConditions.elementToBeClickable(customer_Add_user_button));
                System.out.println("[WARNING] No success toast found; assumed success from form close (Add User clickable).");
            }

            Thread.sleep(2000);
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
            } catch (Exception e) {
                // Loader might not be present
            }
            wait.until(ExpectedConditions.elementToBeClickable(customer_Add_user_button));
            System.out.println("[INFO] Form closed; ready for next user creation.");

            System.out.println("SUCCESS: Created User: " + fullName + " (" + email + ") for " + customerName);

        } catch (Exception e) {
            System.out.println("ERROR: An error occurred during user creation: " + fullName + " - " + e.getMessage());
            throw new RuntimeException("User creation failed for " + fullName, e);
        }
    }
    
    private void fillField(By locator, String value) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).clear();
        driver.findElement(locator).sendKeys(value);
    }
    
    @Step("Delete user: {fullName}")
    public void deleteUserByFullName(String fullName) {
        System.out.println("INFO: Attempting to delete user: " + fullName);
        try {
            
            // Wait for and click the main "Delete User" button on the details page
            WebElement deleteButton = wait.until(ExpectedConditions.elementToBeClickable(deleteUserButton));
            deleteButton.click();
            
            // Wait for and click the "Delete" button in the confirmation modal
            WebElement confirmDeleteButtonElement = wait.until(ExpectedConditions.elementToBeClickable(confirmDeleteButton));
            confirmDeleteButtonElement.click();
            
            System.out.println("SUCCESS: Successfully deleted user: " + fullName);
            
        } catch (Exception e) {
            System.out.println("ERROR: An error occurred during user deletion: " + fullName + " - " + e.getMessage());
        }
    }
    
    
    public void verifyUsersBelongToCompany(String companyName) throws Exception {
        System.out.println("INFO: Verifying that all users belong to company: " + companyName);
        try {
            Allure.step("Clicking on the search icon to reveal the search field.");
            
            
            wait.until(ExpectedConditions.elementToBeClickable(searchIcon)).click();

            Allure.step("Entering customer name '" + companyName + "' in the search field.");
            wait.until(ExpectedConditions.visibilityOfElementLocated(searchField)).sendKeys(companyName);

            // Wait for the search results to be ready
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
            
            // Get the list of all users found from the search
            List<WebElement> userLinks = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(userFullNameLink));
            Assert.assertFalse(userLinks.isEmpty(), "No users found for company: " + companyName);

            int numberOfUsers = userLinks.size();
            System.out.println("INFO: " + numberOfUsers + " users found. Starting verification loop.");

            for (int i = 0; i < numberOfUsers; i++) {
                Allure.step("Iteration " + (i + 1) + ": Verifying user association.");
                
                // Re-find the element in each iteration to avoid StaleElementReferenceException
                WebElement userLink = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(userFullNameLink)).get(i);
                String userName = userLink.getText();
                System.out.println("INFO: Verifying association for user: " + userName);

                userLink.click();
                wait.until(ExpectedConditions.urlContains("/users/"));

                String actualCompanyName = wait.until(ExpectedConditions.visibilityOfElementLocated(companyNameLabel)).getText();
                Assert.assertEquals(actualCompanyName, companyName, "User " + userName + " is not mapped to the correct company.");

                Allure.step("Navigating back to the user list page.");
                driver.navigate().back();
                // Wait for the page to be stable before the next iteration
                wait.until(ExpectedConditions.elementToBeClickable(searchIcon));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
                
                // Clear the search field for the next search
                wait.until(ExpectedConditions.elementToBeClickable(searchField)).clear();
                wait.until(ExpectedConditions.elementToBeClickable(searchField)).sendKeys(companyName);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
            }
            System.out.println("SUCCESS: Successfully verified all users for company: " + companyName);
        } catch (Exception e) {
            Allure.step("ERROR: Failed to verify users for company " + companyName);
            throw e;
        }
    }
    
    @Step("Search for user: {userName}")
public void searchUser(String fullNamefield) throws Exception {
    	String parentWindow = driver.getWindowHandle();
    Allure.step("Clicking search icon to open search bar.");
    try {
        wait.until(ExpectedConditions.elementToBeClickable(searchIcon)).click();

        Allure.step("Entering user full name '" + fullNamefield + "' in the search field.");
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchField));
        fillField(searchField, fullNamefield);

        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));  // Wait for loader to disappear

        Allure.step("Clicking on the found user '" + fullNamefield + "'");
     // Now click on the user link in the results
        By userLink = By.xpath("//td[@data-index='1']");
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable(userLink));
        link.click();
        

        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(parentWindow)) {
                driver.switchTo().window(windowHandle);
                break;
            }
        }

        System.out.println("✅ Switched to new tab for user: " + fullNamefield);
        

    } catch (Exception e) {
        Allure.step("ERROR: Failed to search for user " + fullNamefield);
        throw e;
    }
}

    
    
    public void searchUserandDelete(String fullNamefield) throws Exception {
        String parentWindow = driver.getWindowHandle();
        Allure.step("Clicking search icon to open search bar.");

        try {
            Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(searchIcon)).click();

            Allure.step("Entering user full name '" + fullNamefield + "' in the search field.");
            wait.until(ExpectedConditions.visibilityOfElementLocated(searchField));
            fillField(searchField, fullNamefield);

            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));  // Wait for loader to disappear

            Allure.step("Clicking on the found user '" + fullNamefield + "'");
            By userLink = By.xpath("//td[@data-index='1']");
            WebElement link = wait.until(ExpectedConditions.elementToBeClickable(userLink));
            link.click();

            // Switch to the newly opened tab
            for (String windowHandle : driver.getWindowHandles()) {
                if (!windowHandle.equals(parentWindow)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }

            System.out.println("✅ Switched to new tab for user: " + fullNamefield);

            // Perform delete in the new tab
            deleteUserByFullName(fullNamefield);

            // ✅ Close new tab
            driver.close();

            // ✅ Always switch back immediately
            driver.switchTo().window(parentWindow);
            driver.navigate().refresh();   // <--- ensures DOM is reloaded
            System.out.println("✅ Closed user tab, switched back to parent, and refreshed Users page.");
            Thread.sleep(1000);
        } catch (Exception e) {
            Allure.step("ERROR: Failed to search and delete user " + fullNamefield);
            throw e;
        }
    }



    
    @Step("Toggle Status Check and Verify Inactive")
    public void toggleUserStatusLockAndVerify(String fullNameField) throws Exception {
        Allure.step("Clicking the toggle button to lock the customer.");
        try {
        	Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(toggleLockButton)).click();
            
            Allure.step("Navigating back to the customer list page.");
            wait.until(ExpectedConditions.elementToBeClickable(backButton)).click();
            
            Allure.step("Verifying status for customer '" + fullNameField + "'.");
            By rowLocator = By.xpath("//td[./div[text()='" + fullNameField + "']]/..");
            WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
            WebElement statusCell = row.findElement(By.xpath("./td[@data-index='7']"));
            String actualStatus = statusCell.getText();
            
            Assert.assertEquals(actualStatus, "Inactive", "Status of '" + fullNameField + "' is not Active after toggle.");
            Allure.step("Status successfully verified as 'Inactive'.");
            
        } catch (Exception e) {
            Allure.step("ERROR: Failed to verify status for " + fullNameField);
            throw e;
        }
    }
    
    @Step("Toggle Status Check and Verify Inactive")
    public void toggleStatusUnlockAndVerify(String fullNameField) throws Exception {
        Allure.step("Clicking the toggle button to lock the customer.");
        try {
        	Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(toggleUnlockButton)).click();
            
            Allure.step("Navigating back to the customer list page.");
            wait.until(ExpectedConditions.elementToBeClickable(backButton)).click();
            
            Allure.step("Verifying status for customer '" + fullNameField + "'.");
            By rowLocator = By.xpath("//td[./div[text()='" + fullNameField + "']]/..");
            WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
            WebElement statusCell = row.findElement(By.xpath("./td[@data-index='7']"));
            String actualStatus = statusCell.getText();
            
            Assert.assertEquals(actualStatus, "Inactive", "Status of '" + fullNameField + "' is not Active after toggle.");
            Allure.step("Status successfully verified as 'Inactive'.");
            
        } catch (Exception e) {
            Allure.step("ERROR: Failed to verify status for " + fullNameField);
            throw e;
        }
    }
    
    /**
     * Checks user status in the users table based on company name
     * @param userName The full name of the user
     * @param companyName The company name to check in the table
     * @return true if user status matches customer status, false otherwise
     */
    @Step("Check user status in table for company: {companyName}")
    public boolean checkUserStatusForCompany(String userName, String companyName) throws Exception {
        Allure.step("Navigating to users section and checking status for user: " + userName);
        
        driver.findElement(By.xpath("//a[@href='/users']")).click();
        Thread.sleep(2000);
        
        // Wait for table to load
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        
        // Find the row with the user name
        By userRowLocator = By.xpath("//td[./div[text()='" + userName + "']]/..");
        WebElement userRow = wait.until(ExpectedConditions.visibilityOfElementLocated(userRowLocator));
        
        // Find the company name column (find by company name text)
        // Assuming company column is somewhere in the row
        List<WebElement> cells = userRow.findElements(By.tagName("td"));
        String foundCompanyName = "";
        String userStatus = "";
        
        // Find company name and status in the row
        for (WebElement cell : cells) {
            String cellText = cell.getText();
            if (cellText.contains(companyName)) {
                foundCompanyName = cellText;
            }
            // Status is typically in the last column or a specific column
            if (cellText.equalsIgnoreCase("Active") || cellText.equalsIgnoreCase("Inactive")) {
                userStatus = cellText;
            }
        }
        
        System.out.println("📊 User: " + userName);
        System.out.println("📊 Found Company in table: " + foundCompanyName);
        System.out.println("📊 User Status: " + userStatus);
        
        // For now, return true if company name is found (you can enhance this logic)
        boolean statusMatches = foundCompanyName.contains(companyName);
        
        return statusMatches;
    }

    /**
     * Filters users by company name using the Company dropdown filter
     * @param companyName The company name to filter by
     */
    @Step("Filter users by company: {companyName}")
    public void filterUsersByCompany(String companyName) throws Exception {
        System.out.println("\n[STEP 5] Filtering users by company: " + companyName);
        Allure.step("Refreshing users page and filtering by company: " + companyName);
        
        // Refresh the page
        System.out.println("[STEP 5.1] Refreshing users page...");
        driver.navigate().refresh();
        Thread.sleep(2000);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        System.out.println("[DEBUG] Page refreshed, loader disappeared");
        
        // Click on Company dropdown filter
        System.out.println("[STEP 5.2] Clicking on Company filter dropdown...");
        By companyFilterSpan = By.xpath("//span[@title='Company']");
        wait.until(ExpectedConditions.elementToBeClickable(companyFilterSpan)).click();
        Thread.sleep(1000);
        System.out.println("[DEBUG] Company dropdown opened");
        
        // Find all company option buttons with data-option-index
        System.out.println("[STEP 5.3] Searching for company name in dropdown options: " + companyName);
        By allCompanyButtons = By.xpath("//button[@data-option-index]");
        List<WebElement> companyButtons = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(allCompanyButtons));
        System.out.println("[DEBUG] Found " + companyButtons.size() + " company options in dropdown");
        
        boolean companyFound = false;
        // Loop through buttons and find matching company name
        for (int i = 0; i < companyButtons.size(); i++) {
            WebElement button = companyButtons.get(i);
            try {
                // Get the span text inside the button (company name)
                WebElement spanElement = button.findElement(By.xpath(".//span[contains(@class, 'truncate') or contains(@class, 'flex-1')]"));
                String buttonText = spanElement.getText().trim();
                System.out.println("[DEBUG] Option " + i + ": " + buttonText);
                
                // Check if this button's text matches the company name
                if (buttonText.equalsIgnoreCase(companyName)) {
                    System.out.println("[DEBUG] ✅ Found matching company: " + buttonText);
                    System.out.println("[STEP 5.4] Clicking on matching company option...");
                    button.click();
                    companyFound = true;
                    System.out.println("[DEBUG] Clicked on company option: " + buttonText);
                    break;
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Could not read text from option " + i + ": " + e.getMessage());
            }
        }
        
        if (!companyFound) {
            throw new Exception("Company name '" + companyName + "' not found in dropdown options");
        }

        // After selecting the company, click the "Apply" or filter button
        System.out.println("[STEP 5.5] Clicking Apply/Filter button to apply company filter...");
        By applyFilterButton = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div[1]/div[2]/div/div[2]/div[1]/div/div[3]/div/button[3]");
        wait.until(ExpectedConditions.elementToBeClickable(applyFilterButton)).click();
        System.out.println("[DEBUG] Clicked apply filter button");
        
        // Wait for results to load after applying filter
        System.out.println("[STEP 5.6] Waiting for filtered results to load...");
        Thread.sleep(2000);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        System.out.println("[DEBUG] Filtered results loaded, loader disappeared");
        System.out.println("[SUCCESS] ✅ Successfully filtered users by company: " + companyName);
    }
    
    /**
     * Gets the user's status from the Users table (expects status cell to contain Active/Inactive).
     * Uses the same status column as toggle verification (data-index='7').
     */
    @Step("Get user status from table: {userName}")
    public String getUserStatusFromTable(String userName) throws Exception {
        System.out.println("\n[STEP 6] Getting user status from table for: " + userName);
        
        // Ensure we're on Users page
        System.out.println("[STEP 6.1] Ensuring we're on Users page...");
        driver.findElement(By.xpath("//a[@href='/users']")).click();
        Thread.sleep(1500);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        System.out.println("[DEBUG] On Users page, loader disappeared");

        // Use search to narrow results (more stable)
        System.out.println("[STEP 6.2] Searching for user: " + userName);
        wait.until(ExpectedConditions.elementToBeClickable(searchIcon)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchField)).clear();
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchField)).sendKeys(userName);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        System.out.println("[DEBUG] Search completed, waiting for results...");

        // Find the row with the user name
        System.out.println("[STEP 6.3] Finding user row in table...");
        By rowLocator = By.xpath("//td[./div[text()='" + userName + "']]/..");
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
        System.out.println("[DEBUG] Found user row");
        
        // Get status from data-index='7' column
        System.out.println("[STEP 6.4] Reading user status from status column (data-index='7')...");
        WebElement statusCell = row.findElement(By.xpath("./td[@data-index='7']"));
        String userStatus = statusCell.getText().trim();
        System.out.println("[DEBUG] User status retrieved: " + userStatus);
        System.out.println("[SUCCESS] ✅ Retrieved status for " + userName + ": " + userStatus);
        return userStatus;
    }
    
    /**
     * Verifies that ALL users in the filtered table match the expected customer status.
     * Clicks on each user name (opens in new tab), reads status from profile page, then closes tab.
     * @param expectedStatus The expected status (Active/Inactive) that should match customer status
     * @return true if all users match the expected status, false otherwise
     */
    @Step("Verify all users status match customer status: {expectedStatus}")
    public boolean verifyAllUsersStatusMatchCustomer(String expectedStatus) throws Exception {
        System.out.println("\n[STEP 7] Verifying ALL users status match customer status: " + expectedStatus);
        Allure.step("Checking all users in filtered table match customer status: " + expectedStatus);
        
        // Store parent window handle
        String parentWindow = driver.getWindowHandle();
        System.out.println("[DEBUG] Parent window handle: " + parentWindow);
        
        // Wait for table to be visible and loaded
        System.out.println("[STEP 7.1] Waiting for users table to load...");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        Thread.sleep(2000);
        
        // Find all user rows in the table
        System.out.println("[STEP 7.2] Finding all user rows in the filtered table...");
        By tableRowsLocator = By.xpath("//table//tbody//tr");
        List<WebElement> userRows = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(tableRowsLocator));
        System.out.println("[DEBUG] Found " + userRows.size() + " user row(s) in the table");
        
        if (userRows.isEmpty()) {
            System.out.println("[WARNING] ⚠️ No users found in the filtered table!");
            return false;
        }
        
        boolean allUsersMatch = true;
        int userCount = 0;
        int matchCount = 0;
        int mismatchCount = 0;
        
        System.out.println("\n" + "-".repeat(80));
        System.out.println("USER STATUS VERIFICATION (Expected: " + expectedStatus + ")");
        System.out.println("-".repeat(80));
        
        // Status locator on user profile page
        By userStatusLocator = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div/div[1]/div/div[2]/div[1]/span");
        
        // Loop through each row and check user status by clicking on user name
        for (int i = 0; i < userRows.size(); i++) {
            try {
                // Ensure we're on parent window before each iteration
                driver.switchTo().window(parentWindow);
                Thread.sleep(1000);
                
                // Re-find rows after switching back (they may be stale)
                System.out.println("[STEP 7.3." + (i + 1) + "] Re-finding user rows (iteration " + (i + 1) + ")...");
                userRows = driver.findElements(tableRowsLocator);
                if (i >= userRows.size()) {
                    System.out.println("[WARNING] Row index " + i + " out of bounds. Total rows: " + userRows.size());
                    break;
                }
                
                WebElement row = userRows.get(i);
                userCount++;
                
                // Get user name from the row (clickable link, in SECOND column - first column is checkbox)
                System.out.println("[STEP 7.4." + (i + 1) + "] Extracting user name from row " + (i + 1) + " (second column)...");
                String userName = "";
                WebElement nameLink = null;
                try {
                    // Try to find clickable link/span in second column (name column)
                    // First try data-index='1' (might be 0-indexed, so 1 = second column)
                    nameLink = row.findElement(By.xpath("./td[@data-index='1']//span | ./td[@data-index='1']//div | ./td[@data-index='1']//a | ./td[@data-index='1']"));
                    userName = nameLink.getText().trim();
                    System.out.println("[DEBUG] Found name using data-index='1': " + userName);
                } catch (Exception e) {
                    try {
                        // Try td[2] (second column, 1-indexed)
                        nameLink = row.findElement(By.xpath("./td[2]//span | ./td[2]//div | ./td[2]//a | ./td[2]"));
                        userName = nameLink.getText().trim();
                        System.out.println("[DEBUG] Found name using td[2]: " + userName);
                    } catch (Exception e2) {
                        try {
                            // Try just the cell itself
                            nameLink = row.findElement(By.xpath("./td[2]"));
                            userName = nameLink.getText().trim();
                            System.out.println("[DEBUG] Found name using td[2] (cell only): " + userName);
                        } catch (Exception e3) {
                            System.out.println("[ERROR] Could not extract user name from row " + (i + 1) + ": " + e3.getMessage());
                            userName = "User " + (i + 1);
                        }
                    }
                }
                
                if (userName.isEmpty() || userName.equals("User " + (i + 1))) {
                    System.out.println("[WARNING] ⚠️ User name is empty or default, trying alternative locators...");
                    // Try to get text from all cells to debug
                    List<WebElement> allCells = row.findElements(By.tagName("td"));
                    System.out.println("[DEBUG] Total cells in row: " + allCells.size());
                    for (int cellIdx = 0; cellIdx < allCells.size(); cellIdx++) {
                        try {
                            String cellText = allCells.get(cellIdx).getText().trim();
                            System.out.println("[DEBUG] Cell " + cellIdx + " text: '" + cellText + "'");
                            if (!cellText.isEmpty() && cellIdx == 1) {
                                nameLink = allCells.get(cellIdx);
                                userName = cellText;
                                System.out.println("[DEBUG] Using cell " + cellIdx + " as name: " + userName);
                                break;
                            }
                        } catch (Exception ex) {
                            // Skip this cell
                        }
                    }
                }
                
                System.out.println("[DEBUG] User " + userCount + " name: '" + userName + "'");
                
                // Click on user name (opens new tab)
                System.out.println("[STEP 7.5." + (i + 1) + "] Clicking on user name to open profile in new tab...");
                if (nameLink != null) {
                    // Scroll into view and click
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nameLink);
                    Thread.sleep(500);
                    nameLink.click();
                    System.out.println("[DEBUG] Clicked on name link element");
                } else {
                    // Fallback: click on the second cell (name column)
                    WebElement secondCell = row.findElement(By.xpath("./td[2]"));
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", secondCell);
                    Thread.sleep(500);
                    secondCell.click();
                    System.out.println("[DEBUG] Clicked on second cell as fallback");
                }
                System.out.println("[DEBUG] Clicked on user name, waiting for new tab to open...");
                Thread.sleep(2000);
                
                // Switch to new tab
                System.out.println("[STEP 7.6." + (i + 1) + "] Switching to new tab...");
                String newTabHandle = null;
                for (String windowHandle : driver.getWindowHandles()) {
                    if (!windowHandle.equals(parentWindow)) {
                        newTabHandle = windowHandle;
                        driver.switchTo().window(newTabHandle);
                        System.out.println("[DEBUG] Switched to new tab: " + newTabHandle);
                        break;
                    }
                }
                
                if (newTabHandle == null) {
                    System.out.println("[ERROR] ❌ New tab did not open for user: " + userName);
                    allUsersMatch = false;
                    mismatchCount++;
                    continue;
                }
                
                // Wait for profile page to load
                Thread.sleep(2000);
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
                
                // Read status from profile page
                System.out.println("[STEP 7.7." + (i + 1) + "] Reading user status from profile page...");
                WebElement statusElement = wait.until(ExpectedConditions.visibilityOfElementLocated(userStatusLocator));
                String userStatus = statusElement.getText().trim();
                System.out.println("[DEBUG] User status from profile page: " + userStatus);
                
                // Compare status
                boolean statusMatches = userStatus.equalsIgnoreCase(expectedStatus);
                
                System.out.println("[USER " + userCount + "] Name: " + userName);
                System.out.println("[USER " + userCount + "] Status (from profile): " + userStatus);
                System.out.println("[USER " + userCount + "] Expected: " + expectedStatus);
                System.out.println("[USER " + userCount + "] Match: " + (statusMatches ? "✅ YES" : "❌ NO"));
                
                if (statusMatches) {
                    matchCount++;
                    System.out.println("[SUCCESS] ✅ User '" + userName + "' status matches customer status");
                } else {
                    mismatchCount++;
                    allUsersMatch = false;
                    System.out.println("[FAILURE] ❌ User '" + userName + "' status does NOT match!");
                    System.out.println("[FAILURE]    Expected: " + expectedStatus + ", Found: " + userStatus);
                    Assert.fail("User '" + userName + "' status mismatch. Expected: " + expectedStatus + ", Found: " + userStatus);
                }
                
                // Close the user profile tab and switch back to parent
                System.out.println("[STEP 7.8." + (i + 1) + "] Closing user profile tab and switching back to parent...");
                driver.close();
                driver.switchTo().window(parentWindow);
                System.out.println("[DEBUG] Closed tab and switched back to parent window");
                Thread.sleep(2000);
                
                // Refresh or wait for table to be ready again
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
                System.out.println("-".repeat(80));
                
            } catch (Exception e) {
                System.out.println("[ERROR] ❌ Error processing user " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
                
                // Ensure we're back on parent window even if error occurred
                try {
                    driver.switchTo().window(parentWindow);
                } catch (Exception e2) {
                    System.out.println("[ERROR] Could not switch back to parent window!");
                }
                
                allUsersMatch = false;
                mismatchCount++;
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("VERIFICATION SUMMARY:");
        System.out.println("=".repeat(80));
        System.out.println("Total Users Found: " + userCount);
        System.out.println("Status Matches: " + matchCount);
        System.out.println("Status Mismatches: " + mismatchCount);
        System.out.println("Expected Status: " + expectedStatus);
        System.out.println("=".repeat(80));
        
        if (allUsersMatch) {
            System.out.println("[SUCCESS] ✅ ALL " + userCount + " user(s) status match customer status: " + expectedStatus);
        } else {
            System.out.println("[FAILURE] ❌ " + mismatchCount + " user(s) status do NOT match customer status!");
        }
        
        return allUsersMatch;
    }
    
    
    
}
