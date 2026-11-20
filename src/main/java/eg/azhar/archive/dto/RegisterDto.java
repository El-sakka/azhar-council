package eg.azhar.archive.dto;

import eg.azhar.archive.entity.ArchiveRegister;

public record RegisterDto(Long id, int serialNo, String registerNumber, String referenceCode, String title,
                          String subUnitName, String direction, String startDate, String endDate,
                          String subjectsText, Integer writtenSheets, Integer blankSheets, String language,
                          String scriptType, Integer lengthCm, Integer widthCm, String physicalCondition) {
    public static RegisterDto from(ArchiveRegister r) {
        return new RegisterDto(r.getId(), r.getSerialNo(), r.getRegisterNumber(), r.getReferenceCode(),
                r.getTitle(), r.getSubUnitName(), r.getDirection(), r.getStartDate(), r.getEndDate(),
                r.getSubjectsText(), r.getWrittenSheets(), r.getBlankSheets(), r.getLanguage(),
                r.getScriptType(), r.getLengthCm(), r.getWidthCm(), r.getPhysicalCondition());
    }
}
