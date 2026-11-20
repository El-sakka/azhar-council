package eg.azhar.archive.entity;

import jakarta.persistence.*;

/** A line of a subject table, linked to the inventory file/register it describes. */
@Entity
@Table(name = "subject_entry")
public class SubjectEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subject_id")
    private Long subjectId;
    @Enumerated(EnumType.STRING)
    private EntryType entryType;
    private int serialNo;
    private String itemNumber;
    private String referenceCode;
    private String title;
    private String dateText;
    private Short yearFrom;
    private Short yearTo;
    private String subjectContent;
    @Column(name = "archive_file_id")
    private Long archiveFileId;
    @Column(name = "archive_register_id")
    private Long archiveRegisterId;

    public Long getId() { return id; }
    public Long getSubjectId() { return subjectId; }
    public EntryType getEntryType() { return entryType; }
    public int getSerialNo() { return serialNo; }
    public String getItemNumber() { return itemNumber; }
    public String getReferenceCode() { return referenceCode; }
    public String getTitle() { return title; }
    public String getDateText() { return dateText; }
    public Short getYearFrom() { return yearFrom; }
    public Short getYearTo() { return yearTo; }
    public String getSubjectContent() { return subjectContent; }
    public Long getArchiveFileId() { return archiveFileId; }
    public Long getArchiveRegisterId() { return archiveRegisterId; }
}
