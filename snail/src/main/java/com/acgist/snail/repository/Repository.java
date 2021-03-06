package com.acgist.snail.repository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acgist.snail.context.exception.RepositoryException;
import com.acgist.snail.pojo.entity.Entity;
import com.acgist.snail.pojo.wrapper.ResultSetWrapper;
import com.acgist.snail.utils.BeanUtils;
import com.acgist.snail.utils.CollectionUtils;

/**
 * <p>数据库</p>
 * 
 * @author acgist
 */
public abstract class Repository<T extends Entity> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);
	
	/**
	 * <p>数据库列正则表达式：{@value}</p>
	 */
	private static final String COLUMN_REGEX = "[a-zA-Z]+";
	/**
	 * <p>更新字段验证</p>
	 * <p>不更新字段：ID、创建时间</p>
	 */
	private static final Predicate<String> UPDATE_PROPERTY_VERIFY = property ->
		!Entity.PROPERTY_ID.equals(property) &&
		!Entity.PROPERTY_CREATE_DATE.equals(property);
	
	/**
	 * <p>数据库管理器</p>
	 */
	private final DatabaseManager databaseManager = DatabaseManager.getInstance();
	
	/**
	 * <p>数据库表名</p>
	 */
	private final String table;
	/**
	 * <p>实体类型</p>
	 * <p>注：不使用反射获取泛型（反射获取泛型时存在警告）</p>
	 */
	private final Class<T> entityClazz;
	
	/**
	 * @param table 数据库表明
	 * @param entityClazz 实体类型
	 */
	protected Repository(String table, Class<T> entityClazz) {
		this.table = table;
		this.entityClazz = entityClazz;
	}
	
	/**
	 * <p>合并</p>
	 * <p>数据存在更新，反之新建。</p>
	 * 
	 * @param t 实体
	 */
	public void merge(T t) {
		RepositoryException.requireNotNull(t);
		if(t.getId() == null) {
			this.save(t);
		} else {
			this.update(t);
		}
	}
	
	/**
	 * <p>保存</p>
	 * 
	 * @param t 实体
	 */
	public void save(T t) {
		RepositoryException.requireNotNull(t);
		RepositoryException.requireNull(t.getId());
		t.setId(UUID.randomUUID().toString());
		t.setCreateDate(new Date());
		t.setModifyDate(new Date());
		final String[] properties = BeanUtils.properties(t.getClass());
		final String sqlProperty = Stream.of(properties)
			.map(property -> "`" + property + "`")
			.collect(Collectors.joining(",", "(", ")"));
		final String sqlValue = Stream.of(properties)
			.map(property -> "?")
			.collect(Collectors.joining(",", "(", ")"));
		final Object[] parameters = Stream.of(properties)
			.map(property -> BeanUtils.propertyValue(t, property))
			.toArray();
		final StringBuilder sql = new StringBuilder();
		sql
			.append("INSERT INTO ")
			.append(this.table)
			.append(sqlProperty)
			.append(" VALUES ")
			.append(sqlValue);
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("保存SQL语句：{}", sql);
			LOGGER.debug("保存SQL参数：{}", Arrays.asList(parameters));
		}
		this.databaseManager.update(sql.toString(), parameters);
	}

	/**
	 * <p>更新</p>
	 * <p>更新除ID和创建时间外的所有字段</p>
	 * 
	 * @param t 实体
	 */
	public void update(T t) {
		RepositoryException.requireNotNull(t);
		RepositoryException.requireNotNull(t.getId());
		t.setModifyDate(new Date());
		final String[] properties = BeanUtils.properties(t.getClass());
		final String sqlProperty = Stream.of(properties)
			.filter(UPDATE_PROPERTY_VERIFY)
			.map(property -> "`" + property + "` = ?")
			.collect(Collectors.joining(","));
		final Object[] parameters = Stream.concat(
				Stream.of(properties)
					.filter(UPDATE_PROPERTY_VERIFY)
					.map(property -> BeanUtils.propertyValue(t, property)),
				Stream.of(t.getId())
			).toArray();
		final StringBuilder sql = new StringBuilder();
		sql
			.append("UPDATE ")
			.append(this.table)
			.append(" SET ")
			.append(sqlProperty)
			.append(" WHERE ID = ?");
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("更新SQL语句：{}", sql);
			LOGGER.debug("更新SQL参数：{}", Arrays.asList(parameters));
		}
		this.databaseManager.update(sql.toString(), parameters);
	}
	
	/**
	 * <p>删除</p>
	 * 
	 * @param id id
	 */
	public void delete(String id) {
		RepositoryException.requireNotNull(id);
		final StringBuilder sql = new StringBuilder();
		sql
			.append("DELETE FROM ")
			.append(this.table)
			.append(" WHERE ID = ?");
		this.databaseManager.update(sql.toString(), id);
	}

	/**
	 * <p>查找</p>
	 * 
	 * @param id ID
	 * 
	 * @return 实体
	 */
	public T findOne(String id) {
		RepositoryException.requireNotNull(id);
		final StringBuilder sql = new StringBuilder();
		sql
			.append("SELECT * FROM ")
			.append(this.table)
			.append(" WHERE ID = ? limit 1");
		final List<ResultSetWrapper> list = this.databaseManager.select(sql.toString(), id);
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return this.newInstance(list.get(0));
	}
	
	/**
	 * <p>查找</p>
	 * 
	 * @param property 属性名称
	 * @param value 属性值
	 * 
	 * @return 实体
	 */
	public T findOne(String property, String value) {
		RepositoryException.requireNotNull(property);
		RepositoryException.requireMatch(property, COLUMN_REGEX);
		final StringBuilder sql = new StringBuilder();
		sql
			.append("SELECT * FROM ")
			.append(this.table)
			.append(" WHERE ")
			.append(property)
			.append(" = ? limit 1");
		final List<ResultSetWrapper> list = this.databaseManager.select(sql.toString(), value);
		if(CollectionUtils.isEmpty(list)) {
			return null;
		}
		return this.newInstance(list.get(0));
	}
	
	/**
	 * <p>查找</p>
	 * 
	 * @param sql sql
	 * @param parameters 参数
	 * 
	 * @return 实体列表
	 */
	public List<T> findList(String sql, Object ... parameters) {
		RepositoryException.requireNotNull(sql);
		final List<ResultSetWrapper> list = this.databaseManager.select(sql, parameters);
		if(CollectionUtils.isEmpty(list)) {
			return List.of();
		}
		return list.stream()
			.map(wrapper -> this.newInstance(wrapper))
			.collect(Collectors.toList());
	}
	
	/**
	 * <p>查找所有数据</p>
	 * 
	 * @return 实体列表
	 */
	public List<T> findAll() {
		final StringBuilder sql = new StringBuilder();
		sql
			.append("SELECT * FROM ")
			.append(this.table);
		return this.findList(sql.toString());
	}
	
	/**
	 * <p>新建实体</p>
	 * 
	 * @param wrapper 数据
	 * 
	 * @return 实体
	 */
	private T newInstance(ResultSetWrapper wrapper) {
		final T t = this.newInstance();
		BeanUtils.setProperties(t, wrapper);
		return t;
	}
	
	/**
	 * <p>新建实体</p>
	 * 
	 * @return 实体
	 */
	private T newInstance() {
		return BeanUtils.newInstance(this.entityClazz);
	}
	
}
