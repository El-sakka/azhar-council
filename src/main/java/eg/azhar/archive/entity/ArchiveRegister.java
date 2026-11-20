package eg.azhar.archive.entity;

import jakarta.persistence.*;

/** A row of the inventory list of registers (قائمة حصر السجلات). */
@Entity
@Table(name = "archive_register")
public class ArchiveRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int serialNo;
    private String registerNumber;
    private String referenceCode;
    private String title;
    private String subUnitName;
    private String direction;
    private String startDate;
    private String endDate;
    private Short yearFrom;
    private Short yearTo;
    private String subjectsText;
    private Integer writtenSheets;
    private Integer blankSheets;
    private String language;
    private String scriptType;
    private Integer lengthCm;
    private Integer widthCm;
    private String physicalCondition;

    public Long getId() { return id; }
    public int getSerialNo() { return serialNo; }
    public String getRegisterNumber() { return registerNumber; }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getSubUnitName() { return subUnitName; }
    public String getDirection() { return direction; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Short getYearFrom() { return yearFrom; }
    public Short getYearTo() { return yearTo; }
    public String getSubjectsText() { return subjectsText; }
    public Integer getWrittenSheets() { return writtenSheets; }
    public Integer getBlankSheets() { return blankSheets; }
    public String getLanguage() { return language; }
    public String getScriptType() { return scriptType; }
    public Integer getLengthCm() { return lengthCm; }
    public Integer getWidthCm() { return widthCm; }
    public String getPhysicalCondition() { return physicalCondition; }
}
