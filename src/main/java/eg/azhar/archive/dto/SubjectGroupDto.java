package eg.azhar.archive.dto;

import java.util.List;

/** The table of one subject: its files ("أولًا: الملفات") and registers ("ثانيًا: السجلات"). */
public record SubjectGroupDto(Long subjectId, String code, String name, String path,
                              List<SubjectEntryDto> files, List<SubjectEntryDto> registers) {
}
