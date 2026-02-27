package Pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.openqa.selenium.WebElement;
import io.qameta.allure.Step;
import io.qameta.allure.Allure;


public class CustomerPage {
    private WebDriverWait wait;
    // Locators
    private By createCustomerButton = By.xpath("//button[text()='Add Customer']");
    private By companyNameField = By.xpath("//input[@placeholder='Enter company name']");
    // "Customer Single Point of Contact Name"
    private By customerNameField = By.xpath("//input[@placeholder='Enter customer single point of contact name']");
    private By designationField = By.xpath("//input[@placeholder='Enter designation']");
    // "Company Employee Size" (dropdown)
    private By employeeCountField = By.xpath("//*[self::label or self::p][contains(normalize-space(.),'Company Employee Size')]/following::select[1]");
    private By industryField = By.xpath("//input[@placeholder='Enter industry type']");
    private By domainField = By.xpath("//input[@placeholder='e.g., company.com']");
    
    private By mobileField = By.xpath("//input[@placeholder='Enter mobile number']");
    // "Single Point of Contact for Renewal and Communication"
    private By emailField = By.xpath("//input[@placeholder='Enter email address']");
    
    // Subscription Details
    private By licenseCountField = By.xpath("//label[contains(normalize-space(.),'Number of Licenses')]/following::input[1]");
    private By durationField = By.xpath("//label[contains(normalize-space(.),'Subscription Duration')]/following::select[1]");
    
    private By adminEmailField = By.xpath("//input[@placeholder='Enter admin email']");
    private By adminPasswordField = By.xpath("//input[@placeholder='Enter admin password']");
    private By adminConfirmPasswordField = By.xpath("//input[@placeholder='Confirm admin password']");
    private By saveCustomerButton = By.xpath("//button[text()='Create Customer']");
    
    private By deleteCustomerButton = By.xpath("//button[@title='Delete Customer']");
    private By confirmDeleteButton = By.xpath("(//button[text() = 'Delete'])[2]");
    
    private By searchField = By.xpath("//input[@placeholder='Search all customers...']");
    
    private By toggleLockButton = By.xpath("//div[@title='Lock Customer']");
    private By toggleUnlockButton = By.xpath("//div[@title='Unlock Customer']");
    private By backButton = By.xpath("//button[@class='cursor-pointer']");
	private WebDriver driver;
	private final By loader = By.xpath("//div[@role='progressbar']");
	
	
    public CustomerPage(WebDriver driver, WebDriverWait wait) {
        this.wait = wait;
        this.driver = driver;
    }


    private void fillField(By locator, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
        field.clear();
        field.sendKeys(value);
    }

    private void selectByVisibleText(By locator, String visibleText) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
        Select select = new Select(el);
        select.selectByVisibleText(visibleText);
    }
    
    @Step("Search for customer: {customerName}")
    public void searchCustomer(String customerName) throws Exception {
        Allure.step("Clicking search icon to open search bar.");
        
        String parentWindow = driver.getWindowHandle();
        Allure.step("Clicking search icon to open search bar.");

        try {
      	  driver.switchTo().window(parentWindow);
          driver.findElement(By.xpath("//a[@href='/customers']")).click();
          Thread.sleep(2000);
          wait.until(ExpectedConditions.elementToBeClickable(searchField)).click();

          Allure.step("Entering user full name '" + customerName + "' in the search field.");
          wait.until(ExpectedConditions.visibilityOfElementLocated(searchField));
          fillField(searchField, customerName);
          Thread.sleep(2000);
          
          WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
          shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));  // Wait for loader to disappear

          Allure.step("Clicking on the found user '" + companyNameField + "'");
          By userLink = By.xpath("//a[@target='_blank']");
          WebElement link = wait.until(ExpectedConditions.elementToBeClickable(userLink));
          link.click();
          
          for (String windowHandle : driver.getWindowHandles()) {
              if (!windowHandle.equals(parentWindow)) {
                  driver.switchTo().window(windowHandle);
                  break;
              }
          }

          System.out.println("✅ Switched to new tab for customer: " + customerName);

            

            // Perform delete in the new tab
//            deleteCustomerByName(customerName);
//
//            // ✅ Close new tab
//            driver.close();
//
//            // ✅ Always switch back immediately
//            driver.switchTo().window(parentWindow);
//            driver.navigate().refresh();   // <--- ensures DOM is reloaded
//            System.out.println("✅ Closed user tab, switched back to parent, and refreshed Users page.");
//            Thread.sleep(1000);
        } catch (Exception e) {
            Allure.step("ERROR: Failed to search and delete customer " + customerName);
            throw e;
        }
    }
    
    
    
    @Step("Search and delete customer: {customerName}")
    public void searchAndDeleteCustomer(String customerName) throws Exception {
        String parentWindow = driver.getWindowHandle();
        try {
        	  driver.switchTo().window(parentWindow);
            driver.findElement(By.xpath("//a[@href='/customers']")).click();
            Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(searchField)).click();

            Allure.step("Entering user full name '" + customerName + "' in the search field.");
            wait.until(ExpectedConditions.visibilityOfElementLocated(searchField));
            fillField(searchField, customerName);
            Thread.sleep(2000);
            
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));  // Wait for loader to disappear

            Allure.step("Clicking on the found user '" + companyNameField + "'");
            By userLink = By.xpath("//a[@target='_blank']");
            WebElement link = wait.until(ExpectedConditions.elementToBeClickable(userLink));
            link.click();
            
            for (String windowHandle : driver.getWindowHandles()) {
                if (!windowHandle.equals(parentWindow)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }

            System.out.println("✅ Switched to new tab for customer: " + customerName);

//             perform delete here
            WebElement deleteButton = wait.until(ExpectedConditions.elementToBeClickable(deleteCustomerButton));
            
            Thread.sleep(4000);
            deleteButton.click();
            Thread.sleep(4000);
            WebElement confirmDeleteButtonElement = wait.until(ExpectedConditions.elementToBeClickable(confirmDeleteButton));
            confirmDeleteButtonElement.click();

            System.out.println("✅ Deleted customer: " + customerName);

            driver.close(); // close new tab
            driver.switchTo().window(parentWindow); // back to parent
            driver.navigate().refresh(); // reload customer list
            System.out.println("✅ Closed tab and refreshed parent window.");
            Thread.sleep(1000);
        } catch (Exception e) {
            Allure.step("❌ ERROR: Failed to search and delete customer " + customerName);
            throw e;
        }
    }

    

  
    
    
    @Step("Create customer: {customerName} with email {customerEmail}")
    public void createCustomer(String customerName, String customerEmail, String domain) {
        Allure.step("Attempting to create a new customer.");
        try {
            wait.until(ExpectedConditions.elementToBeClickable(createCustomerButton)).click();
            
            // Fill ONLY required fields (*) in the same order as the UI.
            Allure.step("Filling required Company Information fields.");
            fillField(companyNameField, customerName);
            fillField(customerNameField, customerName);
            fillField(designationField, "Manager");
            selectByVisibleText(employeeCountField, "11-50");
            fillField(industryField, "IT");
            fillField(domainField, domain);

            Allure.step("Filling required Contact Information fields.");
            fillField(mobileField, "9876543210");
            fillField(emailField, customerEmail);

            Allure.step("Filling required Subscription Details fields.");
            // Clear existing value and set to "1"
            WebElement licenseField = wait.until(ExpectedConditions.elementToBeClickable(licenseCountField));
            licenseField.clear();
            licenseField.sendKeys("10");
            // Start Date appears optional in the UI, so we don't set it here.
            selectByVisibleText(durationField, "1 month");

            Allure.step("Filling required Customer Admin Login Details fields.");
            fillField(adminEmailField, customerEmail);
            fillField(adminPasswordField, "Lazy@351383");
            fillField(adminConfirmPasswordField, "Lazy@351383");

            Allure.step("Clicking 'Create Customer' button to save.");
            wait.until(ExpectedConditions.elementToBeClickable(saveCustomerButton)).click();
            
            System.out.println("SUCCESS: Created customer: " + customerName);
        } catch (Exception e) {
            Allure.step("ERROR: Failed to create customer " + customerName);
            throw e;
        }
    }
    
    @Step("Delete customer: {customerName}")
    public void deleteCustomerByName(String customerName) throws Exception {
        Allure.step("Attempting to delete customer '" + customerName + "'.");
        try {
            searchCustomer(customerName);
            
            
            Allure.step("Clicking the delete button.");
            WebElement deleteButton = wait.until(ExpectedConditions.elementToBeClickable(deleteCustomerButton));
            deleteButton.click();
            
            Allure.step("Clicking the confirmation delete button on the modal.");
            WebElement confirmDeleteButtonElement = wait.until(ExpectedConditions.elementToBeClickable(confirmDeleteButton));
            confirmDeleteButtonElement.click();
            
            System.out.println("SUCCESS: Successfully deleted customer: " + customerName);
            
        } catch (Exception e) {
            Allure.step("ERROR: An error occurred during customer deletion: " + customerName);
            throw e;
        }
    }
    
    @Step("Toggle Status Check and Verify Inactive")
    public void toggleStatusLockAndVerify(String customerName) throws Exception {
        Allure.step("Clicking the toggle button to lock the customer.");
        try {
        	Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(toggleLockButton)).click();
            
            Allure.step("Navigating back to the customer list page.");
            wait.until(ExpectedConditions.elementToBeClickable(backButton)).click();
            
            Allure.step("Verifying status for customer '" + customerName + "'.");
            By rowLocator = By.xpath("//td[./div[text()='" + customerName + "']]/..");
            WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
            WebElement statusCell = row.findElement(By.xpath("./td[@data-index='8']"));
            String actualStatus = statusCell.getText();
            
            Assert.assertEquals(actualStatus, "Inactive", "Status of '" + customerName + "' is not Active after toggle.");
            Allure.step("Status successfully verified as 'Inactive'.");
            
        } catch (Exception e) {
            Allure.step("ERROR: Failed to verify status for " + customerName);
            throw e;
        }
    }
    
    @Step("Toggle Status Check and Verify Inactive")
    public void toggleStatusUnlockAndVerify(String customerName) throws Exception {
        Allure.step("Clicking the toggle button to lock the customer.");
        try {
        	Thread.sleep(2000);
            wait.until(ExpectedConditions.elementToBeClickable(toggleUnlockButton)).click();
            
            Allure.step("Navigating back to the customer list page.");
            wait.until(ExpectedConditions.elementToBeClickable(backButton)).click();
            
            Allure.step("Verifying status for customer '" + customerName + "'.");
            By rowLocator = By.xpath("//td[./div[text()='" + customerName + "']]/..");
            WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
            WebElement statusCell = row.findElement(By.xpath("./td[@data-index='8']"));
            String actualStatus = statusCell.getText();
            
            Assert.assertEquals(actualStatus, "Active", "Status of '" + customerName + "' is not Inctive after toggle.");
            Allure.step("Status successfully verified as 'Active'.");
            
        } catch (Exception e) {
            Allure.step("ERROR: Failed to verify status for " + customerName);
            throw e;
        }
    }
    
    
 // New locator for the company name on the details page
    private By companyNameOnDetailsPage = By.xpath("(//p[@class='font-semibold text-gray-800'])[1]");

    /**
     * Gets the company name from the customer details page.
     * Assumes the driver is already on the correct page.
     * @return The company name as a string.
     */
    public String getCompanyNameFromDetailsPage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(companyNameOnDetailsPage)).getText();
    }
    
    /**
     * Opens customer profile in new tab and switches to it
     * @param customerName The name of the customer to open
     * @return The parent window handle to switch back later
     */
    @Step("Open customer profile: {customerName}")
    public String openCustomerProfileInNewTab(String customerName) throws Exception {
        System.out.println("\n[STEP 1] Opening customer profile in new tab for: " + customerName);
        String parentWindow = driver.getWindowHandle();
        System.out.println("[DEBUG] Parent window handle: " + parentWindow);
        Allure.step("Navigating to customers section and searching for: " + customerName);

        System.out.println("[STEP 1.1] Waiting for search field to be clickable...");
        Thread.sleep(2000);
        By searchFieldLocator = By.xpath("//input[@placeholder='Search all customers...']");
        wait.until(ExpectedConditions.elementToBeClickable(searchFieldLocator)).click();
        System.out.println("[DEBUG] Clicked on search field");
        
        Allure.step("Entering customer name in search field: " + customerName);
        System.out.println("[STEP 1.2] Entering customer name in search field: " + customerName);
        fillField(searchFieldLocator, customerName);
        Thread.sleep(2000);
        System.out.println("[DEBUG] Entered customer name and waiting for search results...");
        
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        shortWait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        System.out.println("[DEBUG] Search results loaded, loader disappeared");
        
        Allure.step("Clicking on customer profile link");
        System.out.println("[STEP 1.3] Clicking on customer profile link to open in new tab...");
        By customerLink = By.xpath("//div[@class='MuiBox-root css-1goodgy']");
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable(customerLink));
        link.click();
        System.out.println("[DEBUG] Clicked on customer profile link");
        
        // Switch to new tab
        System.out.println("[STEP 1.4] Switching to new tab...");
        int windowCountBefore = driver.getWindowHandles().size();
        System.out.println("[DEBUG] Window count before switch: " + windowCountBefore);
        
        for (String windowHandle : driver.getWindowHandles()) {
            if (!windowHandle.equals(parentWindow)) {
                driver.switchTo().window(windowHandle);
                System.out.println("[DEBUG] Switched to new window handle: " + windowHandle);
                break;
            }
        }
        
        int windowCountAfter = driver.getWindowHandles().size();
        System.out.println("[DEBUG] Window count after switch: " + windowCountAfter);
        System.out.println("[SUCCESS] ✅ Switched to new tab for customer: " + customerName);
        Thread.sleep(2000);
        return parentWindow;
    }
    
    /**
     * Verifies customer details on the details page
     */
    @Step("Verify customer details")
    public void verifyCustomerDetails(String expectedCompanyName, String expectedEmail, String expectedDomain) {
        System.out.println("\n[STEP 2] Verifying customer details on profile page...");
        Allure.step("Verifying customer details match creation data");
        
        System.out.println("[STEP 2.1] Reading company name from profile page...");
        String actualCompanyName = getCompanyNameFromDetailsPage();
        System.out.println("[DEBUG] Expected Company Name: " + expectedCompanyName);
        System.out.println("[DEBUG] Actual Company Name from page: " + actualCompanyName);
        
        Assert.assertEquals(actualCompanyName, expectedCompanyName, 
            "Company name mismatch. Expected: " + expectedCompanyName + ", Actual: " + actualCompanyName);
        System.out.println("[SUCCESS] ✅ Verified Company Name matches: " + actualCompanyName);
        System.out.println("[DEBUG] Expected Email: " + expectedEmail);
        System.out.println("[DEBUG] Expected Domain: " + expectedDomain);
        
        // Add more verifications as needed for email, domain, etc.
    }
    
    /**
     * Checks status on the profile page using the exact locators you provided,
     * asserts texts, fills the popup, and returns the new status after change.
     */
    @Step("Check and toggle customer status with popup")
    public String checkAndToggleCustomerStatus() throws Exception {
        System.out.println("\n[STEP 3] Checking and toggling customer status...");
        
        // 1. Read current status
        System.out.println("[STEP 3.1] Reading current customer status from profile page...");
        By statusLocator = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div/div[1]/div/div[2]/div[1]/span");
        WebElement statusElement = wait.until(ExpectedConditions.visibilityOfElementLocated(statusLocator));
        String currentStatus = statusElement.getText().trim();
        System.out.println("[DEBUG] Current customer status (from span): " + currentStatus);
        
        Assert.assertTrue(
                currentStatus.equalsIgnoreCase("Active") || currentStatus.equalsIgnoreCase("Inactive"),
                "Unexpected customer status text: " + currentStatus);
        System.out.println("[SUCCESS] ✅ Status is valid: " + currentStatus);

        // 2. Capture action button text at /html/body/div/div[2]/div[2]/div[3]/div/div/div[2]/button[3]
        System.out.println("[STEP 3.2] Reading action button text (Deactivate/Reactivate)...");
        By actionButtonLocator = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div/div[2]/button[3]");
        WebElement actionButton = wait.until(ExpectedConditions.elementToBeClickable(actionButtonLocator));
        String actionText = actionButton.getText().trim();
        System.out.println("[DEBUG] Action button text: " + actionText);

        String wordToType;
        String expectedNewStatus;

        if (currentStatus.equalsIgnoreCase("Active")) {
            System.out.println("[DEBUG] Status is Active, expecting Deactivate button...");
            // When Active, button should be Deactivate
            Assert.assertTrue(
                    actionText.toLowerCase().contains("deactivate"),
                    "Expected Deactivate button when status is Active, but found: " + actionText);
            wordToType = "deactivate";
            expectedNewStatus = "Inactive";
            System.out.println("[DEBUG] Will type 'deactivate' in popup, expecting status to change to: " + expectedNewStatus);
        } else {
            System.out.println("[DEBUG] Status is Inactive, expecting Reactivate button...");
            // When Inactive, button should be Reactivate
            Assert.assertTrue(
                    actionText.toLowerCase().contains("reactivate"),
                    "Expected Reactivate button when status is Inactive, but found: " + actionText);
            wordToType = "reactivate";
            expectedNewStatus = "Active";
            System.out.println("[DEBUG] Will type 'reactivate' in popup, expecting status to change to: " + expectedNewStatus);
        }
        System.out.println("[SUCCESS] ✅ Action button text matches expected: " + actionText);

        // Click the action button (Deactivate / Reactivate)
        System.out.println("[STEP 3.3] Clicking action button (" + actionText + ")...");
        actionButton.click();
        System.out.println("[DEBUG] Clicked action button, waiting for popup...");
        Thread.sleep(1000);

        // 3. In popup, type wordToType into (//input[@type="text"])[3]
        System.out.println("[STEP 3.4] Typing '" + wordToType + "' in popup input field...");
        By popupInputLocator = By.xpath("(//input[@type='text'])[3]");
        WebElement popupInput = wait.until(ExpectedConditions.visibilityOfElementLocated(popupInputLocator));
        popupInput.clear();
        popupInput.sendKeys(wordToType);
        System.out.println("[DEBUG] Entered text: " + wordToType);

        // 4. Click confirm button: /html/body/div/div[2]/div[2]/div[3]/div/div[2]/div/div[2]/div[2]/button[2]
        System.out.println("[STEP 3.5] Clicking confirm button in popup...");
        By confirmButtonLocator = By.xpath("/html/body/div/div[2]/div[2]/div[3]/div/div[2]/div/div[2]/div[2]/button[2]");
        wait.until(ExpectedConditions.elementToBeClickable(confirmButtonLocator)).click();
        System.out.println("[DEBUG] Clicked confirm button, waiting for status change...");
        Thread.sleep(2000);

        // 5. Verify success/deactivation/reactivation message at /html/body/div/div[1]/div/div/div[2]
        System.out.println("[STEP 3.6] Verifying success message...");
        By messageLocator = By.xpath("/html/body/div/div[1]/div/div/div[2]");
        WebElement messageElement = wait.until(ExpectedConditions.visibilityOfElementLocated(messageLocator));
        String messageText = messageElement.getText().trim();
        System.out.println("[DEBUG] Status change message: " + messageText);

        // Basic assertion: message should contain the word we typed or 'success'
        Assert.assertTrue(
                messageText.toLowerCase().contains(wordToType) || messageText.toLowerCase().contains("success"),
                "Unexpected status change message: " + messageText);
        System.out.println("[SUCCESS] ✅ Success message verified: " + messageText);

        // 6. Finally, wait until the status span reflects the new status
        System.out.println("[STEP 3.7] Waiting for status to change to: " + expectedNewStatus);
        wait.until(ExpectedConditions.textToBe(statusLocator, expectedNewStatus));
        String finalStatus = wait.until(ExpectedConditions.visibilityOfElementLocated(statusLocator)).getText().trim();
        System.out.println("[DEBUG] Final customer status after toggle: " + finalStatus);

        Assert.assertEquals(finalStatus, expectedNewStatus,
                "Customer status did not change to expected value after toggle.");
        System.out.println("[SUCCESS] ✅ Customer status successfully changed from " + currentStatus + " to " + finalStatus);
        return finalStatus;
    }
    
    /**
     * Closes current tab and switches back to parent window
     */
    @Step("Close tab and switch back")
    public void closeTabAndSwitchBack(String parentWindow) {
        System.out.println("\n[STEP 4] Closing customer profile tab and switching back to parent window...");
        System.out.println("[DEBUG] Current window handle: " + driver.getWindowHandle());
        System.out.println("[DEBUG] Parent window handle: " + parentWindow);
        
        driver.close();
        System.out.println("[DEBUG] Closed current tab");
        
        driver.switchTo().window(parentWindow);
        System.out.println("[DEBUG] Switched back to parent window: " + parentWindow);
        System.out.println("[SUCCESS] ✅ Closed customer tab and switched back to parent window");
    }
    

}
