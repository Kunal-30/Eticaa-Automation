package Utils;

/**
 * One row from AdvancedFilterTestData.xlsx: filter type, value, type (Current + Past/Current/Past), Include/Exclude, notes.
 */
public class FilterRow {
    private String filterType;
    private String filterValue;
    private String type;           // Current + Past / Current / Past (default Current + Past)
    private String includeExclude; // Include / Exclude (default Include)
    private String notes;

    public FilterRow(String filterType, String filterValue, String notes) {
        this(filterType, filterValue, null, null, notes);
    }

    public FilterRow(String filterType, String filterValue, String type, String includeExclude, String notes) {
        this.filterType = filterType == null ? "" : filterType.trim();
        this.filterValue = filterValue == null ? "" : filterValue.trim();
        this.type = type == null || type.trim().isEmpty() ? "Current + Past" : type.trim();
        this.includeExclude = includeExclude == null || includeExclude.trim().isEmpty() ? "Include" : includeExclude.trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    public String getFilterType() { return filterType; }
    public String getFilterValue() { return filterValue; }
    /** Current + Past / Current / Past */
    public String getType() { return type; }
    /** Include / Exclude */
    public String getIncludeExclude() { return includeExclude; }
    public String getNotes() { return notes; }

    public boolean isEmpty() {
        return filterType.isEmpty() && filterValue.isEmpty();
    }
}
