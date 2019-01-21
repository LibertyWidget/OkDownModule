package com.okdown.db;

public class ColumnEntity {

    public String columnName;               //列的名字
    public String columnType;               //列的类型
    public String[] compositePrimaryKey;    //复合主键
    public boolean isPrimary;               //是否是主键
    public boolean isNotNull;               //是否不能为空
    public boolean isAutoincrement;         //AUTOINCREMENT 是否自增

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
