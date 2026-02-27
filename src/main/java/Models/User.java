package Models;

public class User {
    private String fullName;
    private String email;
    private String mobile;
    private String location;
    private String customerName;
    private String password;
    
    // Constructor with required fields
    public User(String fullName, String email, String customerName) {
        this.fullName = fullName;
        this.email = email;
        this.customerName = customerName;
        setDefaultValues();
    }
    
    // Constructor with all fields
    public User(String fullName, String email, String mobile, 
               String location, String customerName, String password) {
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.location = location;
        this.customerName = customerName;
        this.password = password;
    }
    
    private void setDefaultValues() {
        this.mobile = "9999999999";
        this.location = "Bangalore";
        this.password = "Testing@351383";
    }
    
    // Getters
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getLocation() { return location; }
    public String getCustomerName() { return customerName; }
    public String getPassword() { return password; }
    
    // Setters with method chaining
    public User setFullName(String fullName) { this.fullName = fullName; return this; }
    public User setEmail(String email) { this.email = email; return this; }
    public User setMobile(String mobile) { this.mobile = mobile; return this; }
    public User setLocation(String location) { this.location = location; return this; }
    public User setPassword(String password) { this.password = password; return this; }
    
    @Override
    public String toString() {
        return "User{" +
                "fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", customerName='" + customerName + '\'' +
                '}';
    }
}