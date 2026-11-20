package eg.azhar.archive.dto;

import java.util.List;

/** Node of the subject tree; counts are distinct files/registers in the node and its descendants. */
public record SubjectNodeDto(Long id, String code, String name, int level, long fileCount, long registerCount,
                             List<SubjectNodeDto> children) {
}
