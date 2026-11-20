package eg.azhar.archive.repository;

import eg.azhar.archive.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderBySortOrder();
}
