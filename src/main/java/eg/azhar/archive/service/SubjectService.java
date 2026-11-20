package eg.azhar.archive.service;

import eg.azhar.archive.dto.*;
import eg.azhar.archive.entity.EntryType;
import eg.azhar.archive.entity.Subject;
import eg.azhar.archive.entity.SubjectEntry;
import eg.azhar.archive.repository.SubjectEntryRepository;
import eg.azhar.archive.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class SubjectService {
    private final SubjectRepository subjects;
    private final SubjectEntryRepository entries;

    public SubjectService(SubjectRepository subjects, SubjectEntryRepository entries) {
        this.subjects = subjects;
        this.entries = entries;
    }

    /** Full subject tree with the number of distinct files and registers under each node. */
    public List<SubjectNodeDto> tree() {
        List<Subject> all = subjects.findAllByOrderBySortOrder();
        Map<Long, Set<Long>> files = new HashMap<>();
        Map<Long, Set<Long>> registers = new HashMap<>();
        for (Object[] row : entries.findAllLinks()) {
            Long subjectId = (Long) row[0];
            if (row[1] != null) files.computeIfAbsent(subjectId, k -> new HashSet<>()).add((Long) row[1]);
            if (row[2] != null) registers.computeIfAbsent(subjectId, k -> new HashSet<>()).add((Long) row[2]);
        }
        Map<Long, List<Subject>> byParent = childrenIndex(all);
        return byParent.getOrDefault(null, List.of()).stream()
                .map(s -> node(s, byParent, files, registers)).toList();
    }

    public SubjectResultDto search(Long subjectId, SearchCriteria criteria) {
        List<Subject> all = subjects.findAllByOrderBySortOrder();
        Map<Long, Subject> byId = new LinkedHashMap<>();
        all.forEach(s -> byId.put(s.getId(), s));
        Subject root = Optional.ofNullable(byId.get(subjectId))
                .orElseThrow(() -> new NotFoundException("الموضوع غير موجود"));

        List<Long> ids = new ArrayList<>();
        collect(root, childrenIndex(all), ids);
        List<SubjectEntry> found = entries.search(ids, criteria.likeKeyword(), criteria.safeField(),
                criteria.keywordYear(), criteria.from(), criteria.to());

        Map<Long, List<SubjectEntry>> bySubject = new LinkedHashMap<>();
        found.forEach(e -> bySubject.computeIfAbsent(e.getSubjectId(), k -> new ArrayList<>()).add(e));

        List<SubjectGroupDto> groups = new ArrayList<>();
        bySubject.forEach((id, list) -> {
            Subject s = byId.get(id);
            groups.add(new SubjectGroupDto(id, s.getCode(), s.getName(), path(s, byId),
                    list.stream().filter(e -> e.getEntryType() == EntryType.FILE).map(SubjectEntryDto::from).toList(),
                    list.stream().filter(e -> e.getEntryType() == EntryType.REGISTER).map(SubjectEntryDto::from).toList()));
        });
        long fileCount = found.stream().filter(e -> e.getEntryType() == EntryType.FILE)
                .map(e -> e.getArchiveFileId() != null ? "f" + e.getArchiveFileId() : "e" + e.getId()).distinct().count();
        long registerCount = found.stream().filter(e -> e.getEntryType() == EntryType.REGISTER)
                .map(e -> e.getArchiveRegisterId() != null ? "r" + e.getArchiveRegisterId() : "e" + e.getId()).distinct().count();
        return new SubjectResultDto(root.getId(), root.getCode(), root.getName(), path(root, byId),
                fileCount, registerCount, groups);
    }

    private SubjectNodeDto node(Subject s, Map<Long, List<Subject>> byParent,
                                Map<Long, Set<Long>> files, Map<Long, Set<Long>> registers) {
        List<SubjectNodeDto> children = byParent.getOrDefault(s.getId(), List.of()).stream()
                .map(c -> node(c, byParent, files, registers)).toList();
        List<Long> ids = new ArrayList<>();
        collect(s, byParent, ids);
        Set<Long> f = new HashSet<>();
        Set<Long> r = new HashSet<>();
        ids.forEach(id -> {
            f.addAll(files.getOrDefault(id, Set.of()));
            r.addAll(registers.getOrDefault(id, Set.of()));
        });
        return new SubjectNodeDto(s.getId(), s.getCode(), s.getName(), s.getLevel(), f.size(), r.size(), children);
    }

    private static Map<Long, List<Subject>> childrenIndex(List<Subject> all) {
        Map<Long, List<Subject>> byParent = new HashMap<>();
        all.forEach(s -> byParent.computeIfAbsent(s.getParentId(), k -> new ArrayList<>()).add(s));
        return byParent;
    }

    private static void collect(Subject s, Map<Long, List<Subject>> byParent, List<Long> out) {
        out.add(s.getId());
        byParent.getOrDefault(s.getId(), List.of()).forEach(c -> collect(c, byParent, out));
    }

    private static String path(Subject s, Map<Long, Subject> byId) {
        Deque<String> parts = new ArrayDeque<>();
        for (Subject cur = s; cur != null; cur = cur.getParentId() == null ? null : byId.get(cur.getParentId())) {
            parts.addFirst(cur.getName());
        }
        return String.join(" / ", parts);
    }
}
