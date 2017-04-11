package net.ehicks.eoi;

import net.ehicks.common.Common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DBMapField
{
    public static final String STRING = "STRING";           // varchar2
    public static final String INTEGER = "INTEGER";         // integer
    public static final String LONG = "LONG";               // bigint
    public static final String DECIMAL = "DECIMAL";         // decimal
    public static final String TIMESTAMP = "TIMESTAMP";     // timestamp
    public static final String BLOB = "BLOB";               // timestamp
    public static final String BOOLEAN = "BOOLEAN";               // timestamp

    public DBMap dbMap;
    public String className = "";
    public String fieldName = "";
    public String columnName = "";
    public String type = "";
    public Class clazz;
    public int length;
    public int precision;
    public int scale;
    public boolean nullable;
    public boolean primaryKey;
    public boolean autoIncrement;
    public String declaredColumnDefinition = "";

    public String toString()
    {
        return className + ":" + fieldName;
    }

    public String getColumnDefinition()
    {
        if (declaredColumnDefinition.length() > 0)
        {
            if (declaredColumnDefinition.contains("auto_increment"))
            {
                if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
                    declaredColumnDefinition = declaredColumnDefinition.replace("auto_increment", "IDENTITY(1,1)");
                if (EOI.dbBrand.equals(DbBrand.POSTGRES))
                    declaredColumnDefinition = declaredColumnDefinition.replace("bigint not null auto_increment primary key", "serial primary key");
            }
            if (declaredColumnDefinition.contains("varchar2(32000 CHAR)"))
            {
                if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
                    declaredColumnDefinition = declaredColumnDefinition.replace("varchar2(32000 CHAR)", "varchar(max)");
                if (EOI.dbBrand.equals(DbBrand.POSTGRES))
                    declaredColumnDefinition = declaredColumnDefinition.replace("varchar2(32000 CHAR)", "text");
            }
            return declaredColumnDefinition;
        }

        String columnDef = "";
        if (type.equals(DBMapField.STRING))
        {
            if (length == 0)
                length = 255;
            columnDef += "varchar(" + length + ")";
        }
        if (type.equals(DBMapField.INTEGER))
            columnDef += "integer";
        if (type.equals(DBMapField.LONG))
            columnDef += "bigint";
        if (type.equals(DBMapField.DECIMAL))
        {
            if (precision == 0)
                precision = 24;
            if (scale == 0)
                scale = 2;
            columnDef += "decimal(" + precision + "," + scale + ")";
        }
        if (type.equals(DBMapField.TIMESTAMP))
        {
            if (EOI.dbBrand.equals(DbBrand.H2) || EOI.dbBrand.equals(DbBrand.POSTGRES))
                columnDef += "timestamp";
            if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
                columnDef += "datetime";
        }
        if (type.equals(DBMapField.BLOB))
        {
            if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
                columnDef += "varbinary(max)";
            if (EOI.dbBrand.equals(DbBrand.POSTGRES))
                columnDef += "bytea";
            else
                columnDef += "blob";
        }
        if (type.equals(DBMapField.BOOLEAN))
        {
            if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
                columnDef += "bit";
            else
                columnDef += "boolean";
        }

        if (primaryKey)
            columnDef += " PRIMARY KEY";
        if (!nullable)
            columnDef += " NOT NULL";

        return columnDef;
    }

    public Method getGetter()
    {
        try
        {
            return dbMap.clazz.getDeclaredMethod("get" + Common.capFirstLetter(fieldName));
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Method getSetter()
    {
        try
        {
            return dbMap.clazz.getDeclaredMethod("set" + Common.capFirstLetter(fieldName), clazz);
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Object getValue(Object object)
    {
        try
        {
            Method getter = getGetter();
            return getter.invoke(object);
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
