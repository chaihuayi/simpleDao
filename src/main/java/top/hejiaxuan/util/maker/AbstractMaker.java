package top.hejiaxuan.util.maker;

import org.springframework.util.Assert;
import top.hejiaxuan.util.jdbc.EntityMapperFactory;
import top.hejiaxuan.util.jdbc.EntityTableRowMapper;
import top.hejiaxuan.util.jdbc.util.StringUtils;

import java.util.*;

/**
 * 默认实现
 */
public abstract class AbstractMaker implements SqlMaker {

    public AbstractMaker() {
    }

    //sql 是否创建完成
    private boolean sqlComplete = false;

    //sqlValues 是否创建完成
    private boolean sqlValueComplete = false;

    //sql中需要的值
    protected Object[] sqlValues = {};

    //sql中的条件
    protected List<And> ands = new ArrayList<>();

    //sql
    protected String sql;

    //sql 中的基本字段
    protected Set<String> sqlColumns;

    //实体类与数据库的映射
    protected EntityTableRowMapper entityTableRowMapper;

    /**
     * 设置目标
     *
     * @param entity
     * @return
     */
    @Override
    public SqlMaker target(Class entity) {
        Assert.notNull(entity);
        EntityTableRowMapper entityTableRowMapper = EntityMapperFactory.getMapper(entity);
        this.entityTableRowMapper = entityTableRowMapper;
        this.sqlColumns = entityTableRowMapper.getColumnNames();
        return this;
    }

    /**
     * 检测查询元素是是否存在于数据库表中
     *
     * @param columnName
     * @return
     */
    final protected boolean checkColumn(final String columnName) {
        Assert.notNull(entityTableRowMapper, "没有指定 entity.");
        Class tableClass = entityTableRowMapper.getTableClass();
        if (tableClass == null) {
            return true;
        }
        Set<String> columnNames = entityTableRowMapper.getColumnNames();
        if (columnNames.contains(columnName)) {
            return true;
        }
        throw new UnsupportedOperationException(
                "字段: >" + columnName + "< 不存在于 >" + tableClass.getSimpleName() + "< 表中.");
    }

    /**
     * 获取数据库的字段名称.
     * 如果在非sql模式下, 根据传入的name取得相应的数据库中的字段
     *
     * @param name
     * @return
     */
    final protected String getColumnName(final String name) {
        Map<String, String> fieldNameColumnMapper = entityTableRowMapper.getFieldNameColumnMapper();
        return fieldNameColumnMapper.get(name);
    }

    /**
     * 获取sql
     * 只生成一次，之后取缓存起来的sql
     *
     * @return
     */
    @Override
    final public String toSql() {
        if (isSqlComplete()) {
            return sql;
        }
        this.sql = makeSql();
        this.sqlComplete = true;
        return sql;
    }

    /**
     * 获取sql中需要的value
     * 只生成一次，之后取缓存起来的sqlValues
     *
     * @return
     */
    @Override
    final public Object[] getSqlValues() {
        if (isSqlValueComplete()) {
            return sqlValues;
        }
        this.sqlValues = makeSqlValue().toArray();
        this.sqlValueComplete = true;
        return sqlValues;
    }

    @Override
    public boolean isSqlComplete() {
        return sqlComplete;
    }

    @Override
    public boolean isSqlValueComplete() {
        return sqlValueComplete;
    }

    @Override
    public Class<?> getEntity() {
        Assert.notNull(entityTableRowMapper, "没有指定 entity.");
        return entityTableRowMapper.getTableClass();
    }

    @Override
    public String getTableName() {
        Assert.notNull(entityTableRowMapper, "没有指定 entity.");
        return entityTableRowMapper.getTableName();
    }

    @Override
    public EntityTableRowMapper getEntityTableRowMapper() {
        return entityTableRowMapper;
    }

    @Override
    final public SqlMaker where(List<And> ands) {
        List<Object> objects = new ArrayList<>(makeSqlValue());
        for (And and : ands) {
            this.ands.add(and);
            //是否有值
            if (and.isHasValue()) {
                for (Object value : and.getSqlValues()) {
                    objects.add(value);
                }
            }
        }
        this.sqlValues = objects.toArray();
        return this;
    }

    @Override
    public SqlMaker where(And... ands) {
        return where(Arrays.asList(ands));
    }

    /**
     * 获取sql 中where 条件
     *
     * @return
     */
    final protected String sqlWhere() {
        StringBuilder sql = new StringBuilder();
        if (ands.size() != 0) {
            sql.append("WHERE ");
            for (int i = 0; i < ands.size(); i++) {
                And and = ands.get(i);
                sql.append(StringUtils.append(and.getSql()));
                if (i != ands.size() - 1) {
                    sql.append(StringUtils.AND);
                }
            }
        }
        return sql.toString();
    }

    protected abstract String makeSql();

    protected abstract List<Object> makeSqlValue();

}
