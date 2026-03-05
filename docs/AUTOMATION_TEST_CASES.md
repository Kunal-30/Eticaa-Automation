# Eticaa Project – Automation Test Cases Documentation

This document explains the automation framework structure, all test cases, and how they work.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Folder Structure (Detailed)](#2-folder-structure-detailed)
3. [Execution Flow – How Tests Run](#3-execution-flow--how-tests-run)
4. [Test Cases – Complete List](#4-test-cases--complete-list)
5. [Helper Logic – How Sampling & Assertions Work](#5-helper-logic--how-sampling--assertions-work)
6. [How to Run Tests](#6-how-to-run-tests)
7. [Reports](#7-reports)

---

## 1. Project Overview

| Item | Details |
|------|---------|
| **Project Name** | Eticaa Project Automation |
| **Purpose** | UI automation for the Eticaa DMS (Candidate Management System) web application |
| **Application URL** | https://dms.eticaadev.co.in/auth/login |
| **Tech Stack** | Java 11, Selenium WebDriver 4, TestNG, Allure, Maven |
| **Pattern** | Page Object Model (POM) |

The framework automates Super Admin flows (customer/user creation, status toggling) and Advanced Filter flows (Company, Location, Designation, Experience, CTC, Notice Period) on the Candidates page. Test data is read from Excel files where applicable.

---

## 2. Folder Structure (Detailed)

```
Eticaa-Automation/
│
├── pom.xml                          # Maven project file: dependencies (Selenium, TestNG, POI, Allure, etc.)
│                                    # and plugins (Surefire, Allure, SSH tunnel)
│
├── testng.xml                       # TestNG suite config: which test class and methods to run
│                                    # Currently runs: create_And_Verify_CustomersAndUsers_ActivationAndDeactivation
│
├── src/
│   ├── main/java/                   # Production/support code (Page Objects, Utils, Manager, Models)
│   │   │
│   │   ├── Pages/                   # Page Object Model – one class per screen
│   │   │   ├── basePage.java        # Base setup: launches Chrome, navigates to login URL,
│   │   │   │                        # sets up WebDriverWait, takes screenshot on test failure
│   │   │   ├── loginPage.java       # Login actions (email, password, logout)
│   │   │   ├── superAdminPage.java  # Super Admin navigation (Customers, Users links)
│   │   │   ├── DashboardPage.java   # Dashboard elements and values
│   │   │   ├── CustomerPage.java    # Customer create, profile, status toggle
│   │   │   ├── UserPage.java        # User create, filter by company, status check
│   │   │   ├── CandidatesPage.java  # Candidates list: filters, pagination, result count,
│   │   │   │                        # candidate name links, scroll helpers
│   │   │   └── CandidateDetailsPage # Candidate profile: name, company, designation,
│   │   │                            # location, CTC, notice period, work experience
│   │   │
│   │   ├── Utils/                   # Utilities and Excel readers
│   │   │   ├── ScrollHelper.java    # Scroll element into view (for tables, modals)
│   │   │   ├── SSHTunnelManager.java# Starts/stops SSH tunnel for DB access
│   │   │   ├── DatabaseUtil.java    # Database connection and queries
│   │   │   ├── CompanyMasterExcelUtil.java    # Reads company names from Excel
│   │   │   ├── LocationMasterExcelUtil.java   # Reads location names
│   │   │   ├── DesignationMasterExcelUtil.java# Reads designation names
│   │   │   ├── ExperienceMasterExcelUtil.java # Reads experience ranges (min–max years)
│   │   │   ├── CTCMasterExcelUtil.java        # Reads CTC ranges
│   │   │   ├── AdvancedFilterExcelUtil.java   # General filter Excel helpers
│   │   │   ├── FilterRow.java       # Model for one filter row (value, type)
│   │   │   └── MixedFilterCombination.java   # Builds random 2–4 filter combinations
│   │   │
│   │   ├── Manager/
│   │   │   └── DatabaseCleanupManager.java    # After each test: deletes test users from DB
│   │   │
│   │   └── Models/
│   │       ├── User.java            # User data model
│   │       ├── Customer.java        # Customer data model
│   │       └── TestData.java        # Test data holder
│   │
│   └── test/java/
│       └── testPackage/
│           └── superadmintest.java  # All Super Admin test methods (extends basePage)
│
├── src/test/resources/              # Test resources
│   ├── CompanyMaster.xlsx           # Company filter test data
│   ├── LocationMaster.xlsx          # Location filter test data
│   ├── DesignationMaster.xlsx       # Designation filter test data
│   ├── ExperienceMaster.xlsx        # Experience ranges
│   ├── CTCMaster.xlsx               # CTC ranges
│   ├── session-sharing 2.pem        # SSH key for tunnel (DB access)
│   ├── log4j2.xml                   # Logging config
│   └── logging.properties
│
├── target/                          # Maven build output
│   └── surefire-reports/            # TestNG HTML/XML reports
│
└── allure-results/                  # Allure report data (generated on test run)
```

### Why This Structure?

- **Pages/** – Separates UI locators and actions from test logic; easier to maintain when UI changes.
- **Utils/** – Reusable helpers and Excel readers keep tests clean.
- **Manager/** – Centralized DB cleanup avoids test data pollution.
- **Resources/** – Test data in Excel allows non-developers to change test data without code changes.

---

## 3. Execution Flow – How Tests Run

Every test follows the same lifecycle:

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. @BeforeClass (once for the whole class)                       │
│    setUpSshTunnel() → Starts SSH tunnel for database access      │
└─────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. @BeforeMethod (before each @Test)                             │
│    basePage.setUp() → Launch Chrome, go to login URL, init wait  │
└─────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. @Test                                                         │
│    The actual test method runs (e.g. login, create customer…)    │
└─────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. @AfterMethod (after each @Test)                               │
│    a) tearDown() → Screenshot on failure, close browser          │
│    b) cleanupDatabase() → Delete test users from DB              │
└─────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. @AfterClass (once after all tests)                            │
│    tearDownSshTunnel() → Stop SSH tunnel                         │
└─────────────────────────────────────────────────────────────────┘
```

**Why SSH tunnel?** The tests connect to a PostgreSQL database (e.g. for cleanup). The database is on a remote server, so an SSH tunnel forwards local port 5432 to the remote DB port.

---

## 4. Test Cases – Complete List

All tests live in `superadmintest.java` and extend `basePage` (which provides `driver`, `wait`, and setup/teardown).

---

### Test 1: create_And_Verify_CustomersAndUsers_ActivationAndDeactivation

**Purpose:** End-to-end flow: create customers and users, toggle customer status, and verify user status follows customer status.

**How it works:**

1. **Phase 1 – Create**
   - Login as Super Admin.
   - Create Customer 1 and Customer 2.
   - Create users for each customer.

2. **Phase 2 – Verify and toggle**
   - Open Customer 2 profile in a new tab.
   - Verify customer details.
   - Toggle status (Active ↔ Inactive).
   - Close tab, go to Users.
   - Filter by Customer 2 and verify all users match the new customer status.

3. **Phase 3 – Reactivation**
   - Toggle Customer 2 status again.
   - Verify all users under Customer 2 again match the new status.

**Assertions:** Customer details match creation data; user status matches customer status after each toggle.

---

### Test 2: customer_Creates_Users_And_Verify_Login

**Purpose:** Verify that a customer can create users and those users can log in.

**How it works:**

1. Super Admin creates a customer and logs out.
2. Customer logs in and creates two users.
3. Verify both users exist.
4. Logout.
5. Log in as each created user and verify successful login.

**Assertions:** Users are created; each user can log in successfully.

---

### Test 3: dashboard_Verify_TotalProfiles_And_CandidatesCount

**Purpose:** Verify dashboard content without opening candidate profiles.

**How it works:**

1. Login as Super Admin.
2. Navigate to dashboard.
3. Assert main heading.
4. Log dashboard values (e.g. total profiles, candidates count).

**Assertions:** Dashboard heading is correct.

---

### Test 4: advancedFilter_FromExcel_AssertAndMarkCandidates_for_Company_Filter

**Purpose:** Validate Company filter by checking that sampled candidates actually have the company in their work experience.

**How it works:**

1. Go to Candidates, open Advance Search, clear filters.
2. Load companies from `CompanyMaster.xlsx`.
3. Pick 10 random companies and types (Past/Current/Current+Past).
4. For each:
   - Apply Company filter.
   - Get result count.
   - Sample 2–3 candidates per page (or 10–12 if ≤25 results).
   - Open each sample’s details in a new tab.
   - Check if the company appears in Work Experience (Current or Past).
   - Log as MATCH or MARKED.
   - Close tab and continue.

**Assertions:** `CandidateDetailsPage.isFilterValuePresentInSection("Company", value)` – verifies the company in the profile.

---

### Test 5: advancedFilter_FromExcel_AssertAndMarkCandidates_for_Location_Filter

**Purpose:** Same idea as Test 4 but for Location, with different location modes.

**How it works:**

- Location modes: Either (Current or Preferred), Current Location, Preferred Location, Both (Current and Preferred).
- Reads locations from `LocationMaster.xlsx`.
- Applies filter, samples candidates, opens details, checks `getCurrentLocation()` and `getPrefLocations()`.
- Asserts that the filter value appears in the correct section per mode.

---

### Test 6: advancedFilter_FromExcel_AssertAndMarkCandidates_for_Designation_Filter

**Purpose:** Validate Designation filter against candidate details.

**How it works:**

- Reads designations from `DesignationMaster.xlsx`.
- Applies filter, samples candidates, opens details.
- Asserts `getDesignationFromCurrent()` matches or contains the filter value.

---

### Test 7: advancedFilter_FromExcel_AssertAndMarkCandidates_for_Experience_Filter

**Purpose:** Validate Experience filter (min–max years).

**How it works:**

- Reads experience ranges from `ExperienceMaster.xlsx`.
- Applies min/max years filter.
- Logs result counts (does not open candidate profiles).

---

### Test 8: advancedFilter_FromExcel_AssertAndMarkCandidates_for_CurrentCTC_Filter

**Purpose:** Validate Current CTC filter by checking CTC on candidate details.

**How it works:**

1. Reads CTC ranges from `CTCMaster.xlsx`.
2. Applies Current CTC min–max filter.
3. Samples 2–3 candidates per page.
4. Opens each profile, reads `getCurrentCTCLakhs()`.
5. Asserts CTC is within the filter range.
6. Logs MATCH (within range) or MARKED (outside range).

---

### Test 9: advancedFilter_FromExcel_AssertAndMarkCandidates_for_ExpectedCTC_Filter

**Purpose:** Same as Test 8 but for Expected CTC.

**How it works:**

- Same flow, uses `getExpectedCTCLakhs()`.
- Asserts value is within the selected Expected CTC range.

---

### Test 10: noticePeriodFilter_OpenAllPages_FourPerPage_LogCandidateNames

**Purpose:** Validate Notice Period filter by opening all pages, sampling 4 candidates per page, and asserting notice period on details.

**How it works:**

1. For each Notice Period option: Immediate Joiner, 0 - 15 days, 1 month, 2 months, 3 months:
2. Apply only Notice Period filter.
3. Get total result count (parses first number only, e.g. 8648 from "8648 candidates for NP 0 - 15 days").
4. If results > 25:
   - Calculate total pages (25 per page).
   - For each page:
     - Scroll to bottom (pagination visible).
     - Select page.
     - Scroll to top.
     - Sample 4 random candidates (or fewer on last page).
     - For each candidate: open details, get name and notice period, assert match, log.
5. If results ≤ 25: sample 4 random candidates on single page and do the same.

**Assertions:** `noticePeriodMatchesFilter(filter, valueFromDetails)` – ensures details page notice period is consistent with the applied filter.

---

### Test 11: filterAddAndClearLoop_NoCandidateOpening

**Purpose:** Stress-test mixed filters without opening profiles.

**How it works:**

- Loop: apply random combination of Company, Location, Designation, Experience, Current CTC, Expected CTC, Notice Period.
- Log result counts.
- No candidate profile opening; stop manually when done.

---

### Test 12: advancedFilter_FromExcel_VariableFilterCombinations

**Purpose:** Test variable filter combinations (2–4 filter types) from Excel.

**How it works:**

- Uses `MixedFilterCombination.build()` to create 2–4 filter combinations from Company, Designation, Location, Experience.
- Applies each combination and logs result counts.
- No candidate profile opening.

---

## 5. Helper Logic – How Sampling & Assertions Work

### Pagination and Sampling

- **PAGE_SIZE = 25** – Candidates per page.
- If total results > 25: pagination appears at the bottom; tests must scroll down to use it.

### sampleCandidatesForFilter (2–3 per page)

Used by Company, Location, Designation, CTC tests.

- **total > 25:**
  - Loop through all pages.
  - On each page: pick 2–3 random candidates.
  - Open each, run callback (e.g. assert on details), close tab.
- **total ≤ 25:**
  - Single page: pick 10–12 random candidates (or all if fewer).
  - Same callback logic.

### sampleCandidatesForFilterFixedPerPage (4 per page)

Used by Notice Period test.

- **total > 25:**
  - Loop through all pages.
  - Scroll to bottom before page change.
  - On each page: pick 4 random candidates (or all if fewer).
- **total ≤ 25:**
  - Pick 4 random candidates on the single page.

### Result Count Parsing

Text like `"8648 candidates for NP 0 - 15 days"` must parse to **8648**, not 8648015.

- `CandidatesPage.getResultCount()` uses `parseFirstNumberFromText()` to extract only the first number.

### Notice Period Matching

`noticePeriodMatchesFilter(filter, valueFromDetails)` returns true when the details value matches the filter, e.g.:

- Filter "0 - 15 days" → details "15 days", "0-15 days", "Immediate" → match.
- Filter "1 month" → details "1 month", "30 days" → match.

---

## 6. How to Run Tests

**Prerequisites:**

- Java 11
- Maven
- Chrome browser
- SSH key at `src/test/resources/session-sharing 2.pem` (for DB tunnel)

**Commands:**

```bash
# Navigate to project
cd Eticaa-Automation

# Run all tests (as per testng.xml)
mvn test

# Run a specific test
mvn -Dtest=testPackage.superadmintest#noticePeriodFilter_OpenAllPages_FourPerPage_LogCandidateNames test

# Generate and view Allure report
mvn allure:serve
```

**testng.xml** – Edit to include more tests:

```xml
<methods>
  <include name="create_And_Verify_CustomersAndUsers_ActivationAndDeactivation"/>
  <include name="noticePeriodFilter_OpenAllPages_FourPerPage_LogCandidateNames"/>
  <!-- add more method names -->
</methods>
```

---

## 7. Reports

| Report | Location | Purpose |
|--------|----------|---------|
| TestNG HTML | `target/surefire-reports/index.html` | Pass/fail, duration |
| Allure | `allure-results/` + `mvn allure:serve` | Detailed steps, screenshots on failure |
| Screenshots | Attached in Allure on failure | Captured in `basePage.tearDown()` |

---

## Summary

- **Folder structure** – Pages, Utils, Manager, Models; test data in `resources`.
- **Execution** – SSH tunnel → browser setup → test → cleanup → close tunnel.
- **Tests** – 12 methods covering Super Admin and Advanced Filters, with sampling and assertions on candidate details.
- **Helpers** – Reusable sampling and parsing logic for paginated results and filter validation.
