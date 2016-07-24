package net.ehicks.eoi;

import java.sql.Date;

public class EOIBean
{
    public Long m_createdBy = null;
    public Date m_createdOn = null;
    public Long m_lastUpdatedBy = null;
    public Date m_lastUpdatedOn = null;

    public Long getCreatedBy()
    {
        return m_createdBy;
    }

    public void setCreatedBy(Long createdBy)
    {
        m_createdBy = createdBy;
    }

    public Date getCreatedOn()
    {
        return m_createdOn;
    }

    public void setCreatedOn(Date createdOn)
    {
        m_createdOn = createdOn;
    }

    public Long getLastUpdatedBy()
    {
        return m_lastUpdatedBy;
    }

    public void setLastUpdatedBy(Long lastUpdatedBy)
    {
        m_lastUpdatedBy = lastUpdatedBy;
    }

    public Date getLastUpdatedOn()
    {
        return m_lastUpdatedOn;
    }

    public void setLastUpdatedOn(Date lastUpdatedOn)
    {
        m_lastUpdatedOn = lastUpdatedOn;
    }
}
