package eg.azhar.archive.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    @Column(name = "parent_id")
    private Long parentId;
    private short level;
    private int sortOrder;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Long getParentId() { return parentId; }
    public short getLevel() { return level; }
    public int getSortOrder() { return sortOrder; }
}
