package Models;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    
    // Predefined customer data (matching super admin test data)
    public static Customer getCustomerOne() {
        return new Customer("Customer One Company", "customer@customerone.com", "customerone.com");
    }
    
    public static Customer getCustomerTwo() {
        return new Customer("Customer Two Company", "customer@customertwo.com", "customertwo.com");
    }
    
    // Custom customer with specific requirements
    public static Customer getCustomCustomer(String name, String email, String domain) {
        return new Customer(name, email, domain)
                .setIndustry("Healthcare")
                .setEmployeeCount("100")
                .setCity("Mumbai")
                .setLicenseCount("25");
    }
    
    // Predefined users for Customer One (matching super admin test data)
    // Note: Email is just "user1" or "user2" - full email is constructed by the application
    public static List<User> getUsersForCustomerOne() {
        List<User> users = new ArrayList<>();
        users.add(new User("User1 Customer1", "user1", "Customer One Company"));
        users.add(new User("User2 Customer1", "user2", "Customer One Company"));
        return users;
    }
    
    // Predefined users for Customer Two (matching super admin test data)
    // Note: Email is just "user1" or "user2" - full email is constructed by the application
    public static List<User> getUsersForCustomerTwo() {
        List<User> users = new ArrayList<>();
        users.add(new User("User1 Customer2", "user1", "Customer Two Company"));
        users.add(new User("User2 Customer2", "user2", "Customer Two Company"));
        return users;
    }
    
    // Helper methods to get individual user data (for direct access)
    public static User getUser1Customer1() {
        return new User("User1 Customer1", "user1", "Customer One Company");
    }
    
    public static User getUser2Customer1() {
        return new User("User2 Customer1", "user2", "Customer One Company");
    }
    
    public static User getUser1Customer2() {
        return new User("User1 Customer2", "user1", "Customer Two Company");
    }
    
    public static User getUser2Customer2() {
        return new User("User2 Customer2", "user2", "Customer Two Company");
    }
    
    // Generate multiple customers
    public static List<Customer> getMultipleCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            customers.add(new Customer(
                "Customer " + i, 
                "customer" + i + "@test.com", 
                "customer" + i + ".com"
            ));
        }
        return customers;
    }
    
    // Generate users for a customer
    public static List<User> generateUsers(String customerName, int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(new User(
                "User" + i + " " + customerName,
                "user" + i + customerName.toLowerCase().replace(" ", "") + "@test.com",
                customerName
            ));
        }
        return users;
    }
}
