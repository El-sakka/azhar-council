package eg.azhar.archive.dto;

import java.util.Set;

/**
 * Search input shared by the inventory and subject searches.
 *
 * @param q        free text typed in the search box (file/register title, subject, or a year)
 * @param field    which entry to search: all | title | subject | year
 * @param yearFrom optional lower bound of the date range
 * @param yearTo   optional upper bound of the date range
 */
public record SearchCriteria(String q, String field, Integer yearFrom, Integer yearTo) {

    private static final Set<String> FIELDS = Set.of("all", "title", "subject", "year");

    public String keyword() {
        return q == null ? "" : q.trim().replaceAll("\\s+", " ");
    }

    /** Keyword escaped for use inside a LIKE pattern (escape char is a backslash). */
    public String likeKeyword() {
        return keyword().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public String safeField() {
        return field != null && FIELDS.contains(field) ? field : "all";
    }

    /** The keyword interpreted as a year, or 0 when it is not a 4-digit year. */
    public int keywordYear() {
        String k = toWesternDigits(keyword());
        return k.matches("\\d{4}") ? Integer.parseInt(k) : 0;
    }

    public int from() {
        return yearFrom == null ? 0 : yearFrom;
    }

    public int to() {
        return yearTo == null ? 0 : yearTo;
    }

    public boolean isEmpty() {
        return keyword().isEmpty() && from() == 0 && to() == 0;
    }

    /** Arabic-Indic digits (١٩٣٠) are accepted in the search box. */
    private static String toWesternDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            b.append(c >= '٠' && c <= '٩' ? (char) ('0' + c - '٠') : c);
        }
        return b.toString();
    }
}
