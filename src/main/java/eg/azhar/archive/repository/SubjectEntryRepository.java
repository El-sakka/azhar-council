package eg.azhar.archive.repository;

import eg.azhar.archive.entity.SubjectEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SubjectEntryRepository extends JpaRepository<SubjectEntry, Long> {

    @Query(value = """
            SELECT e.* FROM subject_entry e JOIN subject s ON s.id = e.subject_id
            WHERE e.subject_id IN (:subjectIds)
              AND (:q = ''
                OR (:field IN ('all', 'title') AND normalize_ar(e.title) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field IN ('all', 'subject') AND normalize_ar(e.subject_content) LIKE '%' || normalize_ar(:q) || '%' ESCAPE '\\')
                OR (:field = 'all' AND (e.item_number = :q OR e.reference_code LIKE '%' || :q || '%' ESCAPE '\\'))
                OR (:qYear > 0 AND :field IN ('all', 'year') AND e.year_from <= :qYear AND e.year_to >= :qYear))
              AND (:yearFrom = 0 OR e.year_to >= :yearFrom)
              AND (:yearTo = 0 OR e.year_from <= :yearTo)
            ORDER BY s.sort_order, e.entry_type, e.serial_no, e.id
            """, nativeQuery = true)
    List<SubjectEntry> search(@Param("subjectIds") Collection<Long> subjectIds, @Param("q") String q,
                              @Param("field") String field, @Param("qYear") int qYear,
                              @Param("yearFrom") int yearFrom, @Param("yearTo") int yearTo);

    /** Lightweight rows for counting: [subjectId, archiveFileId, archiveRegisterId]. */
    @Query("select e.subjectId, e.archiveFileId, e.archiveRegisterId from SubjectEntry e")
    List<Object[]> findAllLinks();
}
