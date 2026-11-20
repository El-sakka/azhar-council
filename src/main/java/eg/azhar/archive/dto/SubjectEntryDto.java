package eg.azhar.archive.dto;

import eg.azhar.archive.entity.SubjectEntry;

public record SubjectEntryDto(Long id, String type, int serialNo, String itemNumber, String referenceCode,
                              String title, String dateText, String subjectContent, Long archiveFileId,
                              Long archiveRegisterId) {
    public static SubjectEntryDto from(SubjectEntry e) {
        return new SubjectEntryDto(e.getId(), e.getEntryType().name(), e.getSerialNo(), e.getItemNumber(),
                e.getReferenceCode(), e.getTitle(), e.getDateText(), e.getSubjectContent(), e.getArchiveFileId(),
                e.getArchiveRegisterId());
    }
}
