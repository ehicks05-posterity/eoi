package net.ehicks.eoi.diagnostic;

import net.ehicks.eoi.EOI;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project implements Serializable
{
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "bigint not null auto_increment primary key")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name = "";

    @Column(name = "prefix", nullable = false, unique = true)
    private String prefix = "";

    @Column(name = "incept_date")
    @Temporal(TemporalType.DATE)
    private Date inceptDate;

    @Column(name = "last_updated_on")
    @Temporal(TemporalType.DATE)
    private Date lastUpdatedOn;

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Project)) return false;
        Project that = (Project) obj;
        return this.id.equals(that.getId());
    }

    @Override
    public int hashCode()
    {
        return 17 * 37 * id.intValue();
    }

    public String toString()
    {
        return this.getClass().getSimpleName() + ":" + id;
    }

    public static List<Project> getAll()
    {
        return EOI.executeQuery("select * from projects");
    }

    public static Project getById(Long id)
    {
        return EOI.executeQueryOneResult("select * from projects where id=?", Arrays.asList(id));
    }

    // -------- Getters / Setters ----------


    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public Date getInceptDate()
    {
        return inceptDate;
    }

    public void setInceptDate(Date inceptDate)
    {
        this.inceptDate = inceptDate;
    }

    public Date getLastUpdatedOn()
    {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(Date lastUpdatedOn)
    {
        this.lastUpdatedOn = lastUpdatedOn;
    }
}
