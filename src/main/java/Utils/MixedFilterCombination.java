package Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds random mixed filter combinations from Excel data:
 * - 2, 3, or 4 different filter types
 * - Sometimes 1 company, sometimes 2 companies
 * - Sometimes 1 designation, sometimes 2 designations
 * - 1 location, 1 experience range
 */
public class MixedFilterCombination {

    private static final Random RND = new Random();

    public static final String FILTER_COMPANY = "Company";
    public static final String FILTER_DESIGNATION = "Designation";
    public static final String FILTER_LOCATION = "Location";
    public static final String FILTER_EXPERIENCE = "Experience";

    /** Number of companies to add: 1 or 2 */
    public int companyCount;
    /** Company filter rows (1 or 2 companies with types) */
    public List<FilterRow> companyRows;

    /** Number of designations to add: 1 or 2 */
    public int designationCount;
    /** Designation filter rows */
    public List<FilterRow> designationRows;

    /** Include Location filter */
    public boolean hasLocation;
    public FilterRow locationRow;

    /** Include Experience filter */
    public boolean hasExperience;
    public int expMin;
    public int expMax;

    public MixedFilterCombination() {
        companyRows = new ArrayList<>();
        designationRows = new ArrayList<>();
    }

    /**
     * Builds a random combination: 2–4 filter types, with 1–2 companies and 1–2 designations when those types are included.
     */
    public static MixedFilterCombination build(
            List<String> allCompanies,
            List<String> allDesignations,
            List<String> allLocations,
            List<int[]> allExperienceRanges) {

        MixedFilterCombination combo = new MixedFilterCombination();

        List<String> available = new ArrayList<>();
        if (allCompanies != null && !allCompanies.isEmpty()) available.add(FILTER_COMPANY);
        if (allDesignations != null && !allDesignations.isEmpty()) available.add(FILTER_DESIGNATION);
        if (allLocations != null && !allLocations.isEmpty()) available.add(FILTER_LOCATION);
        if (allExperienceRanges != null && !allExperienceRanges.isEmpty()) available.add(FILTER_EXPERIENCE);

        if (available.isEmpty()) return combo;

        // Pick 2, 3, or 4 filter types
        Collections.shuffle(available);
        int numTypes = 2 + RND.nextInt(Math.min(3, available.size())); // 2, 3, or 4
        numTypes = Math.min(numTypes, available.size());
        List<String> selected = new ArrayList<>(available.subList(0, numTypes));

        if (selected.contains(FILTER_COMPANY) && allCompanies != null && !allCompanies.isEmpty()) {
            combo.companyCount = 1 + RND.nextInt(2); // 1 or 2 companies
            combo.companyCount = Math.min(combo.companyCount, allCompanies.size());
            combo.companyRows = CompanyMasterExcelUtil.getRandomCompanyFilterRows(allCompanies, combo.companyCount);
        }

        if (selected.contains(FILTER_DESIGNATION) && allDesignations != null && !allDesignations.isEmpty()) {
            combo.designationCount = 1 + RND.nextInt(2); // 1 or 2 designations
            combo.designationCount = Math.min(combo.designationCount, allDesignations.size());
            combo.designationRows = DesignationMasterExcelUtil.getRandomDesignationFilterRows(allDesignations, combo.designationCount);
        }

        if (selected.contains(FILTER_LOCATION)) {
            combo.hasLocation = true;
            List<FilterRow> locRows = LocationMasterExcelUtil.getRandomLocationFilterRows(allLocations, 1);
            combo.locationRow = locRows.isEmpty() ? null : locRows.get(0);
        }

        if (selected.contains(FILTER_EXPERIENCE)) {
            combo.hasExperience = true;
            List<int[]> ranges = ExperienceMasterExcelUtil.getRandomExperienceRanges(allExperienceRanges, 1);
            if (!ranges.isEmpty()) {
                combo.expMin = ranges.get(0)[0];
                combo.expMax = ranges.get(0)[1];
            }
        }

        return combo;
    }

    public int totalFilterCount() {
        int n = 0;
        n += companyRows.size();
        n += designationRows.size();
        if (hasLocation && locationRow != null) n++;
        if (hasExperience) n++;
        return n;
    }

    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        if (!companyRows.isEmpty()) {
            sb.append("Companies=").append(companyRows.size()).append("(");
            for (int i = 0; i < companyRows.size(); i++) {
                if (i > 0) sb.append(",");
                FilterRow r = companyRows.get(i);
                sb.append(r.getFilterValue()).append("[").append(r.getType()).append("]");
            }
            sb.append(") ");
        }
        if (!designationRows.isEmpty()) {
            sb.append("Designations=").append(designationRows.size()).append("(");
            for (int i = 0; i < designationRows.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(designationRows.get(i).getFilterValue());
            }
            sb.append(") ");
        }
        if (hasLocation && locationRow != null) {
            sb.append("Location=").append(locationRow.getFilterValue()).append("[").append(locationRow.getType()).append("] ");
        }
        if (hasExperience) {
            sb.append("Experience=").append(expMin).append("-").append(expMax).append("y");
        }
        return sb.toString().trim();
    }
}
