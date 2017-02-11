package net.ehicks.eoi;

import java.util.ArrayList;
import java.util.List;

public class ProjectionColumn
{
    public String columnLabel = "";
    public String type = "";

    public ProjectionColumn(String columnLabel, String type)
    {
        this.columnLabel = columnLabel;
        this.type = type;
    }

    public static List<ProjectionColumn> getProjectionColumns(String query, DBMap dbMap)
    {
        List<ProjectionColumn> projectionColumns = new ArrayList<>();

        int indexOfSelect = query.indexOf("select");
        int indexOfFrom = query.indexOf("from");

        String projection = query.substring(indexOfSelect + "select".length(), indexOfFrom);
        String[] tokens = projection.split(",");
        for (String columnLabel : tokens)
        {
            columnLabel = columnLabel.trim();
            if (columnLabel.contains("count("))
                projectionColumns.add(new ProjectionColumn(columnLabel, DBMapField.LONG));
            else
            {
                DBMapField field = dbMap.getFieldByColumnName(columnLabel);
                projectionColumns.add(new ProjectionColumn(columnLabel, field.type));
            }
        }
        return projectionColumns;
    }
}
