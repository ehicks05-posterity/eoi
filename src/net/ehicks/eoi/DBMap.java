package net.ehicks.eoi;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBMap
{
    public static List<DBMap> dbMaps = new ArrayList<>();

    public Class clazz;
    public Constructor constructor;
    public String packageName = "";
    public String className = "";
    public String tableName = "";
    public List<DBMapField> fields = new ArrayList<>();

    public static DBMap getDBMapByTableName(String tableName)
    {
        for (DBMap dbMap : dbMaps)
            if (dbMap.tableName.equals(tableName))
                return dbMap;
        return null;
    }

    public static DBMap getDBMapByClass(Class clazz)
    {
        for (DBMap dbMap : dbMaps)
            if (dbMap.clazz.equals(clazz))
                return dbMap;
        return null;
    }

    public static void loadDbMaps(String realPathToBeans, String packagePath)
    {
        try
        {
            File beans = Paths.get(realPathToBeans).toFile();
            for (String bean : beans.list())
            {
                if (new File(realPathToBeans + File.separator + bean).isFile())
                {
                    DBMap dbMap = new DBMap();

                    Class beanClass = Class.forName(packagePath + "." + bean.substring(0, bean.lastIndexOf(".")));
                    dbMap.packageName = beanClass.getPackage().getName();
                    dbMap.clazz = beanClass;
                    dbMap.className = beanClass.getSimpleName();
                    dbMap.constructor = beanClass.getConstructor();

                    Table annotation = (Table) beanClass.getAnnotation(Table.class);
                    dbMap.tableName = annotation.name();

                    for (Field f : beanClass.getDeclaredFields())
                    {
                        Column column = f.getAnnotation(Column.class);
                        if (column != null)
                        {
                            DBMapField dbMapField = new DBMapField();
                            dbMapField.dbMap = dbMap;
                            dbMapField.className = f.getDeclaringClass().getSimpleName();
                            dbMapField.fieldName = f.getName();
                            dbMapField.columnName = column.name();

                            dbMapField.type = getTypeFromJavaType(f);
                            dbMapField.clazz = getClassFromJavaType(f);
                            dbMapField.length = column.length();
                            dbMapField.precision = column.precision();
                            dbMapField.scale = column.scale();
                            dbMapField.nullable = column.nullable();
                            dbMapField.primaryKey = f.getAnnotation(Id.class) != null;
                            dbMapField.autoIncrement = column.columnDefinition().contains("auto_increment");
                            dbMapField.declaredColumnDefinition = column.columnDefinition();
                            dbMap.fields.add(dbMapField);
                        }
                    }

                    DBMap.dbMaps.add(dbMap);
                }
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static String getTypeFromJavaType(Field f)
    {
        if (f.getType().isAssignableFrom(String.class)){return DBMapField.STRING;}
        if (f.getType().isAssignableFrom(Date.class)){return DBMapField.TIMESTAMP;}
        if (f.getType().isAssignableFrom(Integer.class)){return DBMapField.INTEGER;}
        if (f.getType().isAssignableFrom(Long.class)){return DBMapField.LONG;}
        if (f.getType().isAssignableFrom(BigDecimal.class)){return DBMapField.DECIMAL;}
        return null;
    }

    private static Class getClassFromJavaType(Field f)
    {
        if (f.getType().isAssignableFrom(String.class)){return String.class;}
        if (f.getType().isAssignableFrom(Date.class)){return Date.class;}
        if (f.getType().isAssignableFrom(Integer.class)){return Integer.class;}
        if (f.getType().isAssignableFrom(Long.class)){return Long.class;}
        if (f.getType().isAssignableFrom(BigDecimal.class)){return BigDecimal.class;}
        return null;
    }

    public DBMapField getFieldByColumnName(String columnName)
    {
        for (DBMapField field : fields)
            if (field.columnName.equals(columnName))
                return field;
        return null;
    }

    public List<DBMapField> getPKFields()
    {
        List<DBMapField> pkFields = new ArrayList<>();
        for (DBMapField field : fields)
            if (field.primaryKey)
                pkFields.add(field);
        return pkFields;
    }

    public List<DBMapField> getNonPKFields()
    {
        List<DBMapField> nonPkFields = new ArrayList<>();
        for (DBMapField field : fields)
            if (!field.primaryKey)
                nonPkFields.add(field);
        return nonPkFields;
    }
}
