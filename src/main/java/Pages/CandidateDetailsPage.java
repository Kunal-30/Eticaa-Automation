package Pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.ArrayList;

import Utils.ScrollHelper;

/**
 * Candidate details page: get candidate name, current company, designation,
 * current location, pref locations, role, and check if filter value is present in the right section.
 */
public class CandidateDetailsPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By candidateNameH1 = By.xpath("//h1[contains(@class,'text-2xl') and contains(@class,'font-bold')]");
    private final By currentRow = By.xpath("//span[text()='Current:']/../..");
    private final By prefLocationsRow = By.xpath("//span[text()='Pref. Locations:']/..");
    /** Current location value in the section with map-pin-house icon (e.g. span with "Mumbai"). */
    private final By currentLocationSpan = By.xpath("//*[contains(@class,'map-pin-house')]/parent::span");
    /** Preferred locations value span (comma-separated list). */
    private final By prefLocationsValueSpan = By.xpath("//span[text()='Pref. Locations:']/following-sibling::span");
    /** Experience value in the section with briefcase-business icon (e.g. "3y 6m"). */
    private final By experienceSpan = By.xpath("//*[contains(@class,'briefcase-business')]/parent::span");
    private final By workSummarySection = By.xpath("//h2[text()='Work Summary']/..");
    private static final By[] WORK_EXP_SECTION_LOCATORS = {
        By.xpath("//h2[contains(.,'Work Experience') or contains(.,'work experience')]/.."),
        By.xpath("//h2[contains(.,'Work Experience')]/following-sibling::*[1]"),
        By.xpath("//*[self::h2 or self::h3][contains(.,'Work Experience')]/..")
    };

    public CandidateDetailsPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public String getCandidateName() {
        try {
            WebElement h1 = wait.until(ExpectedConditions.visibilityOfElementLocated(candidateNameH1));
            ScrollHelper.scrollIntoView(driver, h1);
            return h1.getText().trim();
        } catch (Exception e) {
            try {
                WebElement breadcrumb = driver.findElement(By.xpath("//nav//li[contains(@class,'font-medium')]"));
                ScrollHelper.scrollIntoView(driver, breadcrumb);
                return breadcrumb.getText().trim();
            } catch (Exception e2) {
                return "Unknown";
            }
        }
    }

    public String getCurrentCompany() {
        try {
            WebElement row = driver.findElement(currentRow);
            ScrollHelper.scrollIntoView(driver, row);
            String full = row.getText();
            if (full.contains(" at ")) {
                String afterAt = full.substring(full.indexOf(" at ") + 4).trim();
                return afterAt.split("\\r?\\n")[0].trim();
            }
            return full.split("\\r?\\n")[0].trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getDesignationFromCurrent() {
        try {
            WebElement row = driver.findElement(currentRow);
            ScrollHelper.scrollIntoView(driver, row);
            String full = row.getText();
            if (full.contains(" at ")) {
                return full.substring(0, full.indexOf(" at ")).trim();
            }
            return full.trim();
        } catch (Exception e) {
            return "";
        }
    }

    /** Gets Current Location value from the section with map-pin-house icon (e.g. "Mumbai"). */
    public String getCurrentLocation() {
        try {
            WebElement span = driver.findElement(currentLocationSpan);
            ScrollHelper.scrollIntoView(driver, span);
            String t = span.getText().trim();
            if (!t.isEmpty()) return t;
        } catch (Exception e) { /* fallback below */ }
        try {
            List<WebElement> spans = driver.findElements(By.xpath("//div[contains(@class,'flex flex-wrap')]//span[contains(@class,'font-medium')]"));
            for (WebElement s : spans) {
                ScrollHelper.scrollIntoView(driver, s);
                String t = s.getText().trim();
                if (!t.isEmpty() && !t.contains("@") && !t.matches("\\d+") && t.length() < 50) {
                    return t;
                }
            }
            WebElement pin = driver.findElement(By.xpath("//*[contains(@class,'lucide-map-pin') or contains(@class,'map-pin')]/following-sibling::span"));
            ScrollHelper.scrollIntoView(driver, pin);
            return pin.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getPrefLocations() {
        try {
            WebElement valueSpan = driver.findElement(prefLocationsValueSpan);
            ScrollHelper.scrollIntoView(driver, valueSpan);
            return valueSpan.getText().trim();
        } catch (Exception e) {
            try {
                WebElement row = driver.findElement(prefLocationsRow);
                ScrollHelper.scrollIntoView(driver, row);
                return row.getText().replace("Pref. Locations:", "").trim();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    /** Preferred locations as a list (split by comma). */
    public List<String> getPrefLocationsAsList() {
        String pref = getPrefLocations();
        if (pref == null || pref.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String s : pref.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    /** Gets experience text from the briefcase-business icon span (e.g. "3y 6m"). */
    public String getExperienceText() {
        try {
            WebElement span = driver.findElement(experienceSpan);
            ScrollHelper.scrollIntoView(driver, span);
            return span.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Parses experience text (e.g. "3y 6m", "3y", "6m", "Fresher") to total years.
     * Returns null if unparseable.
     */
    public static Double parseExperienceToYears(String text) {
        if (text == null || text.isEmpty()) return null;
        String t = text.trim().toLowerCase();
        if (t.contains("fresher") || t.equals("0")) return 0.0;
        int years = 0;
        int months = 0;
        java.util.regex.Pattern yPattern = java.util.regex.Pattern.compile("(\\d+)\\s*y");
        java.util.regex.Pattern mPattern = java.util.regex.Pattern.compile("(\\d+)\\s*m");
        java.util.regex.Matcher ym = yPattern.matcher(t);
        if (ym.find()) years = Integer.parseInt(ym.group(1));
        java.util.regex.Matcher mm = mPattern.matcher(t);
        if (mm.find()) months = Integer.parseInt(mm.group(1));
        if (years == 0 && months == 0) return null;
        return years + months / 12.0;
    }

    /** Gets experience on page as years (e.g. "3y 6m" -> 3.5). Returns null if not found or unparseable. */
    public Double getExperienceYears() {
        return parseExperienceToYears(getExperienceText());
    }

    /**
     * Gets Current CTC from the page in lakhs (LPA). Searches for "Current CTC", "CCTC", "Current Salary" etc.
     * Returns null if not found or unparseable.
     */
    public Double getCurrentCTCLakhs() {
        return parseCTCFromPage("Current CTC", "CCTC", "Current Salary", "Current CTC (Lakhs)");
    }

    /**
     * Gets Expected CTC from the page in lakhs (LPA). Searches for "Expected CTC", "ECTC", "Expected Salary" etc.
     * Returns null if not found or unparseable.
     */
    public Double getExpectedCTCLakhs() {
        return parseCTCFromPage("Expected CTC", "ECTC", "Expected Salary", "Expected CTC (Lakhs)");
    }

    private Double parseCTCFromPage(String... labels) {
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            if (bodyText == null || bodyText.isEmpty()) return null;
            for (String label : labels) {
                int idx = bodyText.toLowerCase().indexOf(label.toLowerCase());
                if (idx < 0) continue;
                String after = bodyText.substring(idx + label.length());
                // Take next ~50 chars that might contain the number
                String snippet = after.length() > 80 ? after.substring(0, 80) : after;
                Double val = parseLakhsFromText(snippet);
                if (val != null) return val;
            }
            // Fallback: look for "X LPA", "X Lakh", "X L" near any CTC mention
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)\\s*(?:LPA|Lakh|Lakhs|L)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(bodyText);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    private Double parseLakhsFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)\\s*(?:LPA|Lakh|Lakhs|L)\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) { return null; }
        }
        p = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)");
        m = p.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    public String getRoleFromWorkSummary() {
        try {
            WebElement section = driver.findElement(workSummarySection);
            ScrollHelper.scrollIntoView(driver, section);
            List<WebElement> rows = section.findElements(By.xpath(".//div[contains(.,'Role')]"));
            for (WebElement r : rows) {
                ScrollHelper.scrollIntoView(driver, r);
                if (r.getText().contains("Role")) {
                    return r.getText().replace("Role", "").trim();
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static class WorkExperienceEntry {
        final String company;
        final String dateLine;

        WorkExperienceEntry(String company, String dateLine) {
            this.company = company == null ? "" : company.trim();
            this.dateLine = dateLine == null ? "" : dateLine.trim();
        }

        boolean isCurrent() {
            if (dateLine.isEmpty()) return true;
            String d = dateLine.toLowerCase();
            return d.contains("till date") || d.contains("present");
        }

        boolean isPast() {
            if (dateLine.isEmpty()) return false;
            if (isCurrent()) return false;
            if (dateLine.contains(" to ") || dateLine.contains(" - ")) return true;
            if (dateLine.contains("\u2013") || dateLine.contains("\u2014")) return true;
            if (dateLine.matches(".*\\d{4}.*\\d{4}.*")) return true;
            return false;
        }
    }

    private List<WorkExperienceEntry> getWorkExperienceEntries() {
        List<WorkExperienceEntry> entries = new ArrayList<>();
        WebElement section = null;
        for (By loc : WORK_EXP_SECTION_LOCATORS) {
            try {
                section = driver.findElement(loc);
                break;
            } catch (Exception ignored) { }
        }
        if (section == null) {
            entries = parseWorkExperienceFromPageText();
            System.out.println("[DETAILS] Work exp section not found; parsed from page text: " + entries.size() + " entries");
            logParsedCompanies(entries);
            return entries;
        }
        try {
            ScrollHelper.scrollIntoView(driver, section);
            List<WebElement> headers = section.findElements(By.xpath(".//h3[contains(.,' at ')]"));
            if (headers.isEmpty()) {
                headers = section.findElements(By.xpath(".//*[contains(.,' at ') and string-length(normalize-space(.)) < 150 and string-length(normalize-space(.)) > 10]"));
            }
            for (WebElement h : headers) {
                ScrollHelper.scrollIntoView(driver, h);
                String full = h.getText();
                if (full == null || full.trim().isEmpty()) continue;
                full = full.split("\\r?\\n")[0].trim();
                if (!full.contains(" at ")) continue;
                String company = full.substring(full.indexOf(" at ") + 4).trim();
                if (company.isEmpty()) continue;
                String dateLine = getDateLineForWorkExpEntry(h);
                entries.add(new WorkExperienceEntry(company, dateLine));
            }
            if (entries.isEmpty()) {
                entries = parseWorkExperienceFromPageText();
                System.out.println("[DETAILS] Work exp section found but 0 h3; parsed from page text: " + entries.size() + " entries");
            } else {
                System.out.println("[DETAILS] Work exp entries from section: " + entries.size());
            }
            logParsedCompanies(entries);
        } catch (Exception e) {
            entries = parseWorkExperienceFromPageText();
            System.out.println("[DETAILS] Work exp parse error; fallback page text: " + entries.size() + " entries");
            logParsedCompanies(entries);
        }
        return entries;
    }

    /** Fallback: parse "Role at Company" and date lines from full page text when section/h3 not found. */
    private List<WorkExperienceEntry> parseWorkExperienceFromPageText() {
        List<WorkExperienceEntry> entries = new ArrayList<>();
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            if (bodyText == null || bodyText.isEmpty()) return entries;
            String[] lines = bodyText.split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty() || line.length() > 200) continue;
                if (!line.contains(" at ")) continue;
                if (line.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) continue;
                int atIdx = line.indexOf(" at ");
                if (atIdx > 70) continue;
                String company = line.substring(atIdx + 4).trim();
                if (company.isEmpty() || company.length() > 120) continue;
                if (company.toLowerCase().contains("university") || company.toLowerCase().contains("college") || company.toLowerCase().contains("institute")) continue;
                String dateLine = "";
                for (int j = i + 1; j < Math.min(i + 4, lines.length); j++) {
                    String next = lines[j].trim();
                    if (next.contains("till date") || next.contains(" to ") || next.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                        dateLine = next;
                        break;
                    }
                }
                entries.add(new WorkExperienceEntry(company, dateLine));
            }
        } catch (Exception e) {
            // ignore
        }
        return entries;
    }

    private void logParsedCompanies(List<WorkExperienceEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (WorkExperienceEntry e : entries) names.add(e.company);
        // System.out.println("[DETAILS] Parsed through - Companies: " + names);
        System.out.println("All companies (parsed) : " + (names != null ? names : "[]"));

    }

    /** Get date line for a work exp h3: try following-sibling p, then next sibling block, then parent block text for date pattern. */
    private String getDateLineForWorkExpEntry(WebElement h3) {
        try {
            WebElement dateEl = h3.findElement(By.xpath("following-sibling::p | parent::div/p | parent::*/p"));
            String t = dateEl.getText();
            if (t != null && !t.trim().isEmpty()) return t.trim();
        } catch (Exception ignored) { }
        try {
            WebElement nextSib = h3.findElement(By.xpath("following-sibling::*[1]"));
            String t = nextSib.getText();
            if (t != null && !t.trim().isEmpty() && (t.contains("till date") || t.contains(" to ") || t.matches(".*\\d{4}-\\d{2}-\\d{2}.*")))
                return t.trim();
        } catch (Exception ignored) { }
        try {
            WebElement parent = h3.findElement(By.xpath("parent::*"));
            String blockText = parent.getText();
            if (blockText != null && !blockText.isEmpty()) {
                for (String line : blockText.split("\\r?\\n")) {
                    String l = line.trim();
                    if (l.isEmpty()) continue;
                    if (l.contains("till date") || l.contains(" to ") || l.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                        return l;
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    /** True if company name (full string from page) contains the keyword, case-insensitive, trimmed. */
    private static boolean companyContainsKeyword(String company, String keyword) {
        if (company == null || keyword == null || keyword.isEmpty()) return false;
        String c = company.trim().toLowerCase();
        String k = keyword.trim().toLowerCase();
        return c.contains(k);
    }

    private static String parseCompanyPeriodFromNotes(String notes) {
        if (notes == null) return null;
        String n = notes.trim().toLowerCase();
        if (n.isEmpty()) return null;
        if (n.contains("current + past") || n.contains("current+past") || (n.contains("current") && n.contains("past"))) return null;
        if (n.contains("past")) return "Past";
        if (n.contains("current")) return "Current";
        return null;
    }

    private static void logCompanyCheckResult(String fullCompanyName, String typeChosen, String typeFound, boolean pass, List<String> allCompaniesParsed) {
        String companyDisplay = (fullCompanyName != null && !fullCompanyName.isEmpty()) ? fullCompanyName.trim() : "-";
        System.out.println("Full Company Name (where keyword found) : " + companyDisplay);
        System.out.println("Type (chosen) : " + (typeChosen != null ? typeChosen : "-") + " | Type (found in) : " + (typeFound != null ? typeFound : "-"));
        System.out.println("Pass (match filter) : " + (pass ? "Yes" : "No"));
        
        System.out.println("----------------------------------------");
    }

    public boolean isFilterValuePresentInSection(String filterType, String filterValue) {
        return isFilterValuePresentInSection(filterType, filterValue, "Current + Past", "Include", null);
    }

    public boolean isFilterValuePresentInSection(String filterType, String filterValue, String notes) {
        return isFilterValuePresentInSection(filterType, filterValue, "Current + Past", "Include", notes);
    }

    /**
     * @param companyType Current + Past / Current / Past
     * @param includeExclude Include / Exclude. Include = pass when keyword found; Exclude = pass when keyword NOT found.
     */
    public boolean isFilterValuePresentInSection(String filterType, String filterValue, String companyType, String includeExclude, String notes) {
        if (filterValue == null || filterValue.isEmpty()) return true;
        String v = filterValue.trim().toLowerCase();
        String type = filterType == null ? "" : filterType.trim();
        String period = (companyType != null && !companyType.trim().isEmpty()) ? companyType.trim() : "Current + Past";
        String incExc = (includeExclude != null && !includeExclude.trim().isEmpty()) ? includeExclude.trim() : "Include";
        boolean isExclude = "Exclude".equalsIgnoreCase(incExc);

        if (type.equalsIgnoreCase("Company")) {
            String candidateName = getCandidateName();
            String currentRowCompany = getCurrentCompany();
            System.out.println("Candidate Name : " + (candidateName != null ? candidateName : "-"));
            System.out.println("Current Company : " + (currentRowCompany != null && !currentRowCompany.isEmpty() ? currentRowCompany : "-"));

            String periodParsed;
            if (period == null || period.isEmpty()) {
                periodParsed = parseCompanyPeriodFromNotes(notes);
                if (periodParsed == null) periodParsed = "";
            } else {
                periodParsed = "Current + Past".equalsIgnoreCase(period) ? "" : period.trim();
            }
            List<WorkExperienceEntry> entries = getWorkExperienceEntries();

            String fullCompanyName = null;
            String typeFound = null;

            if (periodParsed.isEmpty() || "Current + Past".equalsIgnoreCase(periodParsed)) {
                if (currentRowCompany != null && companyContainsKeyword(currentRowCompany, filterValue)) {
                    fullCompanyName = currentRowCompany.trim();
                    typeFound = "Current";
                }
                if (fullCompanyName == null) {
                    for (WorkExperienceEntry e : entries) {
                        if (companyContainsKeyword(e.company, filterValue)) {
                            fullCompanyName = e.company.trim();
                            typeFound = e.isCurrent() ? "Current" : "Past";
                            break;
                        }
                    }
                }
            } else if ("Current".equalsIgnoreCase(periodParsed)) {
                if (currentRowCompany != null && companyContainsKeyword(currentRowCompany, filterValue)) {
                    fullCompanyName = currentRowCompany.trim();
                    typeFound = "Current";
                }
                if (fullCompanyName == null) {
                    for (WorkExperienceEntry e : entries) {
                        if (e.isCurrent() && companyContainsKeyword(e.company, filterValue)) {
                            fullCompanyName = e.company.trim();
                            typeFound = "Current";
                            break;
                        }
                    }
                }
            } else if ("Past".equalsIgnoreCase(periodParsed)) {
                for (WorkExperienceEntry e : entries) {
                    if (e.isPast() && companyContainsKeyword(e.company, filterValue)) {
                        fullCompanyName = e.company.trim();
                        typeFound = "Past";
                        break;
                    }
                }
            }

            boolean keywordFound = fullCompanyName != null;
            boolean pass = isExclude ? !keywordFound : keywordFound;
            List<String> allCompaniesParsed = new ArrayList<>();
            for (WorkExperienceEntry e : entries) allCompaniesParsed.add(e.company);
            System.out.println(pass
                ? "[ADVANCE FILTER] MATCH (value found): Candidate name = " + candidateName + " | Filter: Company Name = " + filterValue
                : "[ADVANCE FILTER] MARKED (value not found): Candidate name = " + candidateName + " | Filter: Company Name = " + filterValue);
            logCompanyCheckResult(fullCompanyName, period, typeFound, pass, allCompaniesParsed);
            return pass;
        }
        if (type.equalsIgnoreCase("Location")) {
            String current = getCurrentLocation();
            String pref = getPrefLocations();
            String candidateName = getCandidateName();

            String currentSafe = current == null ? "" : current.trim();
            String prefSafe = pref == null ? "" : pref.trim();

            // Some UIs show experience like "3y" or "Fresher" in the same area; do not treat that as a location in logs
            if (currentSafe.matches("(?i)\\d+\\s*y.*") || currentSafe.matches("(?i)\\d+[ym]") ||
                currentSafe.equalsIgnoreCase("fresher") || currentSafe.equalsIgnoreCase("student")) {
                currentSafe = "";
            }

            String mode = (companyType != null) ? companyType.trim() : "";
            boolean inCurrent = !currentSafe.isEmpty() && currentSafe.toLowerCase().contains(v);
            boolean inPref = !prefSafe.isEmpty() && prefSafe.toLowerCase().contains(v);

            boolean keywordFound;
            if (mode.equalsIgnoreCase("Current")) {
                keywordFound = inCurrent;
            } else if (mode.equalsIgnoreCase("Preferred")) {
                keywordFound = inPref;
            } else if (mode.toLowerCase().startsWith("both")) {
                keywordFound = inCurrent && inPref;
            } else {
                // Either (Current or Preferred) or default
                keywordFound = inCurrent || inPref;
            }
            boolean pass = isExclude ? !keywordFound : keywordFound;

            System.out.println("Candidate Name : " + (candidateName != null ? candidateName : "-"));
            System.out.println("Location Mode (chosen) : " + (!mode.isEmpty() ? mode : "-"));

            if (mode.equalsIgnoreCase("Current")) {
                System.out.println("Current Location (section) : " + (!currentSafe.isEmpty() ? currentSafe : "-"));
            } else if (mode.equalsIgnoreCase("Preferred")) {
                List<String> prefList = getPrefLocationsAsList();
                System.out.println("Preferred Locations (section) – all values:");
                if (prefList.isEmpty()) {
                    System.out.println("  (none)");
                } else {
                    for (String loc : prefList) System.out.println("  - " + loc);
                }
            } else {
                // Either or Both: print values of both sections
                System.out.println("Current Location (section) : " + (!currentSafe.isEmpty() ? currentSafe : "-"));
                List<String> prefList = getPrefLocationsAsList();
                System.out.println("Preferred Locations (section) – all values:");
                if (prefList.isEmpty()) {
                    System.out.println("  (none)");
                } else {
                    for (String loc : prefList) System.out.println("  - " + loc);
                }
                if (mode.toLowerCase().startsWith("both")) {
                    if (inCurrent && inPref) {
                        System.out.println("Found in: Current location section and Preferred location section");
                    } else if (inCurrent) {
                        System.out.println("Found in: Current location section only (not in Preferred)");
                    } else if (inPref) {
                        System.out.println("Found in: Preferred location section only (not in Current)");
                    } else {
                        System.out.println("Found in: Neither section");
                    }
                } else {
                    if (inCurrent && inPref) {
                        System.out.println("Found in: Current location section and Preferred location section");
                    } else if (inCurrent) {
                        System.out.println("Found in: Current location section");
                    } else if (inPref) {
                        System.out.println("Found in: Preferred location section");
                    } else {
                        System.out.println("Found in: Neither section");
                    }
                }
            }

            System.out.println(pass
                ? "[ADVANCE FILTER] MATCH (location value found): Candidate name = " + candidateName + " | Filter: Location = " + filterValue
                : "[ADVANCE FILTER] MARKED (location value not found): Candidate name = " + candidateName + " | Filter: Location = " + filterValue);
            System.out.println("----------------------------------------");

            return pass;
        }
        if (type.equalsIgnoreCase("Designation")) {
            String designation = getDesignationFromCurrent();
            String candidateName = getCandidateName();

            String desSafe = designation == null ? "" : designation.trim();
            boolean keywordFound = desSafe.toLowerCase().contains(v);
            boolean pass = isExclude ? !keywordFound : keywordFound;

            System.out.println("Candidate Name : " + (candidateName != null ? candidateName : "-"));
            System.out.println("Current Designation (before ' at ') : " + (!desSafe.isEmpty() ? desSafe : "-"));
            System.out.println(pass
                ? "[ADVANCE FILTER] MATCH (designation value found): Candidate name = " + candidateName + " | Filter: Designation = " + filterValue
                : "[ADVANCE FILTER] MARKED (designation value not found): Candidate name = " + candidateName + " | Filter: Designation = " + filterValue);
            System.out.println("----------------------------------------");

            return pass;
        }
        if (type.equalsIgnoreCase("Role")) {
            String role = getRoleFromWorkSummary();
            return role.toLowerCase().contains(v);
        }
        return false;
    }
}
