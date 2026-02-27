package Models;

public class Customer {
    // Basic Info
    private String companyName;
    private String customerName;
    private String designation;
    private String employeeCount;
    private String industry;
    private String domain;
    
    // Address Info
    private String city;
    private String country;
    private String state;
    private String pinCode;
    private String address;
    
    // Contact Info
    private String mobile;
    private String email;
    
    // Subscription Info
    private String licenseCount;
    private String duration;
    private String adminPassword;
    
    // Constructor with required fields
    public Customer(String customerName, String email, String domain) {
        this.customerName = customerName;
        this.email = email;
        this.domain = domain;
        setDefaultValues();
    }
    
    // Constructor with all fields
    public Customer(String companyName, String customerName, String email, 
                   String domain, String designation, String employeeCount, 
                   String industry, String city, String country, String state,
                   String pinCode, String address, String mobile, 
                   String licenseCount, String duration, String adminPassword) {
        this.companyName = companyName;
        this.customerName = customerName;
        this.email = email;
        this.domain = domain;
        this.designation = designation;
        this.employeeCount = employeeCount;
        this.industry = industry;
        this.city = city;
        this.country = country;
        this.state = state;
        this.pinCode = pinCode;
        this.address = address;
        this.mobile = mobile;
        this.licenseCount = licenseCount;
        this.duration = duration;
        this.adminPassword = adminPassword;
    }
    
    private void setDefaultValues() {
        this.companyName = this.customerName + " Company";
        this.designation = "Manager";
        this.employeeCount = "50";
        this.industry = "IT";
        this.city = "Bangalore";
        this.country = "India";
        this.state = "Karnataka";
        this.pinCode = "560001";
        this.address = "MG Road";
        this.mobile = "9876543210";
        this.licenseCount = "10";
        this.duration = "12";
        this.adminPassword = "Testing@351383";
    }
    
    // Getters
    public String getCompanyName() { return companyName; }
    public String getCustomerName() { return customerName; }
    public String getDesignation() { return designation; }
    public String getEmployeeCount() { return employeeCount; }
    public String getIndustry() { return industry; }
    public String getDomain() { return domain; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getState() { return state; }
    public String getPinCode() { return pinCode; }
    public String getAddress() { return address; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public String getLicenseCount() { return licenseCount; }
    public String getDuration() { return duration; }
    public String getAdminPassword() { return adminPassword; }
    
    // Setters for customization
    public Customer setCompanyName(String companyName) { this.companyName = companyName; return this; }
    public Customer setDesignation(String designation) { this.designation = designation; return this; }
    public Customer setEmployeeCount(String employeeCount) { this.employeeCount = employeeCount; return this; }
    public Customer setIndustry(String industry) { this.industry = industry; return this; }
    public Customer setCity(String city) { this.city = city; return this; }
    public Customer setCountry(String country) { this.country = country; return this; }
    public Customer setState(String state) { this.state = state; return this; }
    public Customer setPinCode(String pinCode) { this.pinCode = pinCode; return this; }
    public Customer setAddress(String address) { this.address = address; return this; }
    public Customer setMobile(String mobile) { this.mobile = mobile; return this; }
    public Customer setLicenseCount(String licenseCount) { this.licenseCount = licenseCount; return this; }
    public Customer setDuration(String duration) { this.duration = duration; return this; }
    public Customer setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; return this; }
    
    @Override
    public String toString() {
        return "Customer{" +
                "customerName='" + customerName + '\'' +
                ", email='" + email + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }
}