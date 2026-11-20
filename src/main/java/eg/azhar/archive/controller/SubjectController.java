package eg.azhar.archive.controller;

import eg.azhar.archive.dto.SearchCriteria;
import eg.azhar.archive.dto.SubjectNodeDto;
import eg.azhar.archive.dto.SubjectResultDto;
import eg.azhar.archive.service.SubjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService subjects;

    public SubjectController(SubjectService subjects) {
        this.subjects = subjects;
    }

    @GetMapping
    public List<SubjectNodeDto> tree() {
        return subjects.tree();
    }

    /** The subject's own table plus the tables of its sub-subjects, optionally filtered. */
    @GetMapping("/{id}/entries")
    public SubjectResultDto entries(@PathVariable Long id, SearchCriteria criteria) {
        return subjects.search(id, criteria);
    }
}
