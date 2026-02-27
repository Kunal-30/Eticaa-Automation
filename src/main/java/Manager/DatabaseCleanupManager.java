package Manager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import Utils.DatabaseUtil;
import io.qameta.allure.Step;
import org.testng.ITestResult;

/**
 * Manager class for database cleanup operations.
 * This class handles business logic for cleanup operations.
 * 
 * Responsibilities:
 * - Define which data to clean up (business rules)
 * - Orchestrate cleanup operations
 * - Provide high-level cleanup methods
 * 
 * Uses DatabaseUtil for actual database operations.
 */
public class DatabaseCleanupManager {
    
    /**
     * Deletes users from the database by their email addresses.
     * This is a business-level method that uses DatabaseUtil for execution.
     * 
     * @param emails List of email addresses to delete
     * @return Number of users deleted
     * @throws SQLException if database operation fails
     */
    @Step("Delete users from database by emails: {emails}")
    public static int deleteUsersByEmails(List<String> emails) throws SQLException {
        System.out.println("\n[CLEANUP] =========================================");
        System.out.println("[CLEANUP] Starting user cleanup by emails");
        System.out.println("[CLEANUP] =========================================");
        
        if (emails == null || emails.isEmpty()) {
            System.out.println("[CLEANUP] WARNING: No emails provided, skipping deletion.");
            return 0;
        }
        
        System.out.println("[CLEANUP] Emails to delete: " + emails);
        
        // Build DELETE query with dynamic placeholders
        StringBuilder queryBuilder = new StringBuilder(
            "DELETE FROM public.users WHERE email IN ("
        );
        
        // Add placeholders for each email
        for (int i = 0; i < emails.size(); i++) {
            if (i > 0) {
                queryBuilder.append(", ");
            }
            queryBuilder.append("?");
        }
        queryBuilder.append(")");
        
        String query = queryBuilder.toString();
        
        // Use DatabaseUtil to execute the query
        int deletedCount = DatabaseUtil.executeDelete(query, emails);
        
        System.out.println("[CLEANUP] =========================================");
        System.out.println("[CLEANUP] User cleanup completed");
        System.out.println("[CLEANUP] =========================================");
        
        return deletedCount;
    }
    
    /**
     * Deletes predefined test users from the database.
     * This method contains the business logic for which test users to clean up.
     * 
     * @return Number of users deleted
     * @throws SQLException if database operation fails
     */
    @Step("Delete test users from database")
    public static int cleanupTestUsers() throws SQLException {
        System.out.println("\n[CLEANUP] =========================================");
        System.out.println("[CLEANUP] DATABASE CLEANUP: Deleting test users");
        System.out.println("[CLEANUP] =========================================");
        
        // Business logic: Define which test users to clean up
        List<String> testEmails = Arrays.asList(
            "customer@customerone.com",
            "customer@customertwo.com",
            "user1@customerone.com",
            "user2@customerone.com",
            "user1@customertwo.com",
            "user2@customertwo.com"
        );
        
        System.out.println("[CLEANUP] Test users to delete:");
        for (String email : testEmails) {
            System.out.println("[CLEANUP]   - " + email);
        }
        
        // Use the generic delete method
        int deletedCount = deleteUsersByEmails(testEmails);
        
        System.out.println("[CLEANUP] =========================================");
        System.out.println("[CLEANUP] ✅ Test user cleanup completed");
        System.out.println("[CLEANUP] Deleted " + deletedCount + " user(s)");
        System.out.println("[CLEANUP] =========================================");
        
        return deletedCount;
    }

    /**
     * One reusable method to be called from @AfterMethod in any test class.
     * It prints the same logs and performs cleanup for the fixed 6 emails.
     */
    @Step("AfterMethod Cleanup: Delete fixed test users from database")
    public static void runAfterMethodCleanup(ITestResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DATABASE CLEANUP: Deleting test users from database");
        System.out.println("=".repeat(80));

        // Clear test status indication
        String testStatus = result.isSuccess() ? "PASS" : "FAIL";
        String statusIcon = result.isSuccess() ? "✅" : "❌";
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST STATUS: " + statusIcon + " " + testStatus);
        System.out.println("=".repeat(80));
        System.out.println("[INFO] Test Method: " + result.getMethod().getMethodName());
        System.out.println("[INFO] Test Status: " + (result.isSuccess() ? "PASSED ✅" : "FAILED ❌"));
        System.out.println("[INFO] Running database cleanup after test completion...");

        try {
            int deletedCount = cleanupTestUsers();
            System.out.println("\n[SUCCESS] ✅ Database cleanup completed successfully!");
            System.out.println("[SUCCESS] ✅ Deleted " + deletedCount + " user(s) from database.");
        } catch (Exception e) {
            System.out.println("\n[ERROR] ❌ Database cleanup failed!");
            System.out.println("[ERROR] ❌ Error: " + e.getMessage());
            System.out.println("[ERROR] ❌ Please check:");
            System.out.println("[ERROR]    1. Database connection parameters (set via -Ddb.url, -Ddb.user, -Ddb.password)");
            System.out.println("[ERROR]    2. PostgreSQL is running and accessible");
            System.out.println("[ERROR]    3. Database credentials are correct");
            System.out.println("[ERROR]    4. Network connectivity to database server");
            e.printStackTrace();
            // Don't fail the test if database cleanup fails, but log the error
        }

        // Final status summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL TEST RESULT: " + statusIcon + " " + testStatus);
        System.out.println("=".repeat(80));
    }
    
    /**
     * Deletes users for a specific customer.
     * This method contains business logic for cleaning up customer-specific users.
     * 
     * @param customerEmail Customer email address
     * @return Number of users deleted
     * @throws SQLException if database operation fails
     */
    @Step("Delete users for customer: {customerEmail}")
    public static int cleanupUsersForCustomer(String customerEmail) throws SQLException {
        System.out.println("\n[CLEANUP] =========================================");
        System.out.println("[CLEANUP] Cleaning up users for customer: " + customerEmail);
        System.out.println("[CLEANUP] =========================================");
        
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            System.out.println("[CLEANUP] WARNING: Customer email is empty, skipping cleanup.");
            return 0;
        }
        
        // Business logic: Delete customer and all users associated with this customer
        // This assumes users have emails that match the customer domain pattern
        // Adjust the query based on your actual database schema
        String query = "DELETE FROM public.users WHERE email LIKE ? OR email = ?";
        List<String> parameters = Arrays.asList("%@" + extractDomain(customerEmail), customerEmail);
        
        int deletedCount = DatabaseUtil.executeDelete(query, parameters);
        
        System.out.println("[CLEANUP] =========================================");
        System.out.println("[CLEANUP] ✅ Customer user cleanup completed");
        System.out.println("[CLEANUP] Deleted " + deletedCount + " user(s) for customer: " + customerEmail);
        System.out.println("[CLEANUP] =========================================");
        
        return deletedCount;
    }
    
    /**
     * Helper method to extract domain from email address.
     * 
     * @param email Email address
     * @return Domain part of email
     */
    private static String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(email.indexOf("@") + 1);
    }
}
