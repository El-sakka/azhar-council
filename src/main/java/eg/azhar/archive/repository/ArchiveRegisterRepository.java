package eg.azhar.archive.repository;

import eg.azhar.archive.entity.ArchiveRegister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchiveRegisterRepository extends JpaRepository<ArchiveRegister, Long> {

    /** Same parameters as {@link ArchiveFileRepository#FILTER}. */
    String FILTER = """
            WHERE (:q = ''
                OR (:field IN ('all', 'title') AND normalize_ar(r.title) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field IN ('all', 'subject') AND normalize_ar(r.subjects_text) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field = 'all' AND (r.register_number = :q OR r.reference_code LIKE '%' || :q || '%' ESCAPE '\\'
                    OR normalize_ar(r.direction) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\'))
                OR (:qYear > 0 AND :field IN ('all', 'year') AND r.year_from <= :qYear AND r.year_to >= :qYear))
              AND (:yearFrom = 0 OR r.year_to >= :yearFrom)
              AND (:yearTo = 0 OR r.year_from <= :yearTo)
            """;

    @Query(value = "SELECT r.* FROM archive_register r " + FILTER + " ORDER BY r.serial_no",
            countQuery = "SELECT count(*) FROM archive_register r " + FILTER,
            nativeQuery = true)
    Page<ArchiveRegister> search(@Param("q") String q, @Param("field") String field, @Param("qYear") int qYear,
                                 @Param("yearFrom") int yearFrom, @Param("yearTo") int yearTo, Pageable pageable);
}
