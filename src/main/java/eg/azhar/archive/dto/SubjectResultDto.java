package eg.azhar.archive.dto;

import java.util.List;

public record SubjectResultDto(Long subjectId, String code, String name, String path, long fileCount,
                               long registerCount, List<SubjectGroupDto> groups) {
}
