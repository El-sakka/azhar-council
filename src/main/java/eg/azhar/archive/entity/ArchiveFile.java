package eg.azhar.archive.entity;

import jakarta.persistence.*;

/** A row of the inventory list of files (قائمة حصر الملفات). */
@Entity
@Table(name = "archive_file")
public class ArchiveFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int serialNo;
    private String fileNumber;
    private String referenceCode;
    private String title;
    private String coverColor;
    private String fileSize;
    private String startDate;
    private String endDate;
    private Short yearFrom;
    private Short yearTo;
    private Integer sheetCount;
    private String language;
    private String subjectContent;
    private String physicalCondition;
    private String notes;

    public Long getId() { return id; }
    public int getSerialNo() { return serialNo; }
    public String getFileNumber() { return fileNumber; }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getCoverColor() { return coverColor; }
    public String getFileSize() { return fileSize; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Short getYearFrom() { return yearFrom; }
    public Short getYearTo() { return yearTo; }
    public Integer getSheetCount() { return sheetCount; }
    public String getLanguage() { return language; }
    public String getSubjectContent() { return subjectContent; }
    public String getPhysicalCondition() { return physicalCondition; }
    public String getNotes() { return notes; }
}
