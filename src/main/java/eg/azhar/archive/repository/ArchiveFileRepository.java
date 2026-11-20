package eg.azhar.archive.repository;

import eg.azhar.archive.entity.ArchiveFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchiveFileRepository extends JpaRepository<ArchiveFile, Long> {

    /**
     * Parameters: q (LIKE-escaped keyword or ''), field (all|title|subject|year), qYear (year typed in the
     * search box or 0), yearFrom / yearTo (date-range filter, 0 = open).
     */
    String FILTER = """
            WHERE (:q = ''
                OR (:field IN ('all', 'title') AND normalize_ar(f.title) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field IN ('all', 'subject') AND normalize_ar(f.subject_content) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field = 'all' AND (f.file_number = :q OR f.reference_code LIKE '%' || :q || '%' ESCAPE '\\'))
                OR (:qYear > 0 AND :field IN ('all', 'year') AND f.year_from <= :qYear AND f.year_to >= :qYear))
              AND (:yearFrom = 0 OR f.year_to >= :yearFrom)
              AND (:yearTo = 0 OR f.year_from <= :yearTo)
            """;

    @Query(value = "SELECT f.* FROM archive_file f " + FILTER + " ORDER BY f.serial_no",
            countQuery = "SELECT count(*) FROM archive_file f " + FILTER,
            nativeQuery = true)
    Page<ArchiveFile> search(@Param("q") String q, @Param("field") String field, @Param("qYear") int qYear,
                             @Param("yearFrom") int yearFrom, @Param("yearTo") int yearTo, Pageable pageable);
}
