package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class superAdminPage {
   private WebDriver driver;
   private WebDriverWait wait;

   public superAdminPage(WebDriver driver) {
       this.driver = driver;
       this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
   }

   // Navigation locators (match current app routes)
   private By customerTab = By.xpath("//a[@href='/customer-management/customers']");
   private By userTab = By.xpath("//a[@href='/user-management/users']");
   private By deleteBtn = By.xpath("//button[contains(text(),'Delete')]");
   private By confirmOkBtn = By.xpath("//button[text()='OK']");

   // Navigate to Customers tab
   public void openCustomers() {
       System.out.println("[NAV] Navigating to Customers page...");
       // Wait for page to be ready (no loaders)
       try {
           wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='progressbar'] | //div[contains(@class,'loading')] | //div[contains(@class,'spinner')]")));
       } catch (Exception e) {
           // No loader found, continue
       }
       // Wait for customer tab to be clickable and click
       WebElement customerLink = wait.until(ExpectedConditions.elementToBeClickable(customerTab));
       customerLink.click();
       System.out.println("[NAV] ✅ Clicked Customers tab");
       // Wait for customers page to load
       wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='progressbar'] | //div[contains(@class,'loading')] | //div[contains(@class,'spinner')]")));
       System.out.println("[NAV] ✅ Customers page loaded");
   }

   // Navigate to Users tab
   public void openUsers() {
       System.out.println("[NAV] Navigating to Users page...");
       // Wait for page to be ready (no loaders)
    //    try {
    //        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='progressbar'] | //div[contains(@class,'loading')] | //div[contains(@class,'spinner')]")));
    //    } catch (Exception e) {
    //        // No loader found, continue
    //    }
       // Wait for user tab to be clickable and click
       WebElement userLink = wait.until(ExpectedConditions.elementToBeClickable(userTab));
       userLink.click();
       System.out.println("[NAV] ✅ Clicked Users tab");
       // Wait for users page to load
    //    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@role='progressbar'] | //div[contains(@class,'loading')] | //div[contains(@class,'spinner')]")));
       System.out.println("[NAV] ✅ Users page loaded for {customerName} : {userName}");
   }

   // Delete user/customer by checkbox
   public void deleteByEmail(String email) {
       By checkbox = By.xpath("//td[contains(text(),'" + email + "')]/preceding-sibling::td//input[@type='checkbox']");
       wait.until(ExpectedConditions.elementToBeClickable(checkbox)).click();
       driver.findElement(deleteBtn).click();
       wait.until(ExpectedConditions.elementToBeClickable(confirmOkBtn)).click();
   }

   // Deactivate customer by email
   public void deactivateCustomer(String email) {
       By deactivateLink = By.xpath("//td[contains(text(),'" + email + "')]/following-sibling::td//button[contains(text(),'Deactivate')]");
       wait.until(ExpectedConditions.elementToBeClickable(deactivateLink)).click();
       wait.until(ExpectedConditions.elementToBeClickable(confirmOkBtn)).click();
   }
}
