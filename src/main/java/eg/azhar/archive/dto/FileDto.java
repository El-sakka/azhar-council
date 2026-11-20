package eg.azhar.archive.dto;

import eg.azhar.archive.entity.ArchiveFile;

public record FileDto(Long id, int serialNo, String fileNumber, String referenceCode, String title,
                      String coverColor, String fileSize, String startDate, String endDate, Integer sheetCount,
                      String language, String subjectContent, String physicalCondition, String notes) {
    public static FileDto from(ArchiveFile f) {
        return new FileDto(f.getId(), f.getSerialNo(), f.getFileNumber(), f.getReferenceCode(), f.getTitle(),
                f.getCoverColor(), f.getFileSize(), f.getStartDate(), f.getEndDate(), f.getSheetCount(),
                f.getLanguage(), f.getSubjectContent(), f.getPhysicalCondition(), f.getNotes());
    }
}
