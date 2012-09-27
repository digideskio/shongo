package cz.cesnet.shongo.controller.report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a report (e.g., text message) describing some event concerning {@link ReportablePersistentObject}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@DiscriminatorColumn(length = 50)
public abstract class Report
{
    /**
     * Persistent object must have an unique identifier.
     */
    private Long id;

    /**
     * Parent {@link Report} to which it belongs.
     */
    private Report parentReport;

    /**
     * List of child resources (e.g., physical room can contain some videoconferencing equipment).
     */
    private List<Report> childReports = new ArrayList<Report>();

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return {@link #parentReport}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Report getParentReport()
    {
        return parentReport;
    }

    /**
     * @param parentReport sets the {@link #parentReport}
     */
    public void setParentReport(Report parentReport)
    {
        // Manage bidirectional association
        if (parentReport != this.parentReport) {
            if (this.parentReport != null) {
                Report oldParentMessage = this.parentReport;
                this.parentReport = null;
                oldParentMessage.removeChildMessage(this);
            }
            if (parentReport != null) {
                this.parentReport = parentReport;
                this.parentReport.addChildMessage(this);
            }
        }
    }

    /**
     * @return {@link #childReports}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentReport")
    @Access(AccessType.FIELD)
    public List<Report> getChildReports()
    {
        return childReports;
    }

    /**
     * @param childReports sets the {@link #childReports}
     */
    public void setChildReports(List<Report> childReports)
    {
        this.childReports = childReports;
    }

    /**
     * @param message to be added to the {@link #childReports}
     */
    public void addChildMessage(Report message)
    {
        // Manage bidirectional association
        if (childReports.contains(message) == false) {
            childReports.add(message);
            message.setParentReport(this);
        }
    }

    /**
     * @param message to be removed from the {@link #childReports}
     */
    public void removeChildMessage(Report message)
    {
        // Manage bidirectional association
        if (childReports.contains(message)) {
            childReports.remove(message);
            message.setParentReport(null);
        }
    }

    @Transient
    public abstract String getText();

    @Transient
    public String getHelp()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getReport();
    }

    /**
     * @return formatted text and help of the {@link Report}
     */
    @Transient
    public String getReport()
    {
        StringBuilder stringBuilder = new StringBuilder();

        String text = getText();
        text = text.replace("\n", "\n ");
        stringBuilder.append("-");
        stringBuilder.append(text);

        String help = getHelp();
        if (help != null) {
            stringBuilder.append("\n");
            stringBuilder.append(help);
        }

        if (childReports.size() > 0) {
            int childReportsCount = childReports.size();
            for (int index = 0; index < childReportsCount; index++) {
                stringBuilder.append("\n |");
                String childReportString = childReports.get(index).getReport();
                childReportString = childReportString.replace("\n",
                        (index < (childReportsCount - 1) ? "\n |  " : "\n    "));
                stringBuilder.append("\n +-");
                stringBuilder.append(childReportString);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * @return {@link ReportException} with this {@link Report} as description
     */
    public ReportException exception()
    {
        return new ReportException(this);
    }
}