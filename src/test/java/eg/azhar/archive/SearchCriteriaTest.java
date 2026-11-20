package eg.azhar.archive;

import eg.azhar.archive.dto.SearchCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaTest {

    @Test
    void escapesLikeWildcards() {
        assertThat(new SearchCriteria("50%_\\", null, null, null).likeKeyword()).isEqualTo("50\\%\\_\\\\");
    }

    @Test
    void readsYearsIncludingArabicIndicDigits() {
        assertThat(new SearchCriteria(" 1930 ", null, null, null).keywordYear()).isEqualTo(1930);
        assertThat(new SearchCriteria("١٩٣٠", null, null, null).keywordYear()).isEqualTo(1930);
        assertThat(new SearchCriteria("جلسات", null, null, null).keywordYear()).isZero();
    }

    @Test
    void unknownFieldFallsBackToAll() {
        assertThat(new SearchCriteria(null, "drop table", null, null).safeField()).isEqualTo("all");
        assertThat(new SearchCriteria(null, "year", null, null).safeField()).isEqualTo("year");
    }
}
