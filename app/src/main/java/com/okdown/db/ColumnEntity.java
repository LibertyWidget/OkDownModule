package com.okdown.db;

public class ColumnEntity {

    public String columnName;
    public String columnType;
    public String[] compositePrimaryKey;
    public boolean isPrimary;
    public boolean isNotNull;
    public boolean isAutoincrement;

    public ColumnEntity(String columnName, String columnType) {
        this(columnName, columnType, false, false, false);
    }

    public ColumnEntity(String columnName, String columnType, boolean isPrimary, boolean isNotNull) {
        this(columnName, columnType, isPrimary, isNotNull, false);
    }

    public ColumnEntity(String columnName, String columnType, boolean isPrimary, boolean isNotNull, boolean isAutoincrement) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.isPrimary = isPrimary;
        this.isNotNull = isNotNull;
        this.isAutoincrement = isAutoincrement;
    }
}
