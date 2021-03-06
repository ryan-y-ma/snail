package com.acgist.snail.utils;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.h2.jdbc.JdbcClob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acgist.snail.pojo.wrapper.ResultSetWrapper;

/**
 * <p>Bean工具</p>
 * 
 * @author acgist
 */
public final class BeanUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

	/**
	 * <p>工具类禁止实例化</p>
	 */
	private BeanUtils() {
	}
	
	/**
	 * <p>通过反射生成实例</p>
	 * <p>调用默认无参构造方法</p>
	 * 
	 * @param <T> 类型泛型
	 * 
	 * @param clazz 类型
	 * 
	 * @return 实例
	 */
	public static final <T> T newInstance(final Class<T> clazz) {
		Objects.requireNonNull(clazz);
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			LOGGER.error("通过反射生成实例异常：{}", clazz, e);
		}
		return null;
	}
	
	/**
	 * <p>获取实例属性Map</p>
	 * 
	 * <table border="1">
	 * 	<caption>属性类型转换</caption>
	 * 	<tr>
	 * 		<td>{@code String}</td>
	 * 		<td>{@code String}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@code Number}</td>
	 * 		<td>{@code Number}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@code Enum}</td>
	 * 		<td>{@code String}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@code Date}</td>
	 * 		<td>{@code String(yyyyMMddHHmmss)}</td>
	 * 	</tr>
	 * </table>
	 * 
	 * @param instance 实例
	 * 
	 * @return 属性Map
	 */
	public static final Map<String, Object> toMap(final Object instance) {
		Objects.requireNonNull(instance);
		final Map<String, Object> map = new HashMap<>();
		final String[] properties = properties(instance.getClass());
		for (String property : properties) {
			final Object object = propertyValue(instance, property);
			if(object instanceof Enum<?>) {
				map.put(property, ((Enum<?>) object).name());
			} else if(object instanceof Date) {
				// TODO：强转使用JDK最新写法
				// TODO：使用DateUtil工具替换
				final SimpleDateFormat formater = new SimpleDateFormat(DateUtils.DEFAULT_PATTERN);
				map.put(property, formater.format(object));
			} else {
				map.put(property, object);
			}
		}
		return map;
	}
	
	/**
	 * <p>获取类型所有属性名称</p>
	 * 
	 * @param clazz 类型
	 * 
	 * @return 所有属性名称
	 */
	public static final String[] properties(final Class<?> clazz) {
		Objects.requireNonNull(clazz);
		String[] properties = null;
		final Class<?> superClazz = clazz.getSuperclass(); // 父类
		if(superClazz != null) {
			// 递归获取属性
			properties = properties(superClazz);
		} else {
			properties = new String[0];
		}
		final Field[] fields = clazz.getDeclaredFields();
		return Stream.concat(
			Stream
				.of(fields)
				.filter(field -> !PropertyDescriptor.ignoreProperty(field))
				.map(field -> field.getName()),
			Stream.of(properties)
		).toArray(String[]::new);
	}
	
	/**
	 * <p>获取实例对象指定属性名称的属性值</p>
	 * 
	 * @param instance 实例
	 * @param properties 属性名称
	 * 
	 * @return 属性值
	 */
	public static final Object[] propertiesValue(final Object instance, final String[] properties) {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(properties);
		return Stream
			.of(properties)
			.map(property -> propertyValue(instance, property))
			.toArray();
	}
	
	/**
	 * <p>获取实例对象指定属性名称的属性值</p>
	 * 
	 * @param instance 实例
	 * @param property 属性名称
	 * 
	 * @return 属性值
	 */
	public static final Object propertyValue(final Object instance, final String property) {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(property);
		final Class<?> clazz = instance.getClass();
		try {
			final PropertyDescriptor descriptor = new PropertyDescriptor(property, clazz);
			return descriptor.getReadMethod().invoke(instance);
		} catch (Exception e) {
			LOGGER.error("获取实例对象指定属性名称的属性值异常：{}-{}", clazz, property, e);
		}
		return null;
	}
	
	/**
	 * <p>设置实例属性</p>
	 * 
	 * @param instance 实例
	 * @param wrapper 结果集包装器
	 */
	public static final void setProperties(Object instance, ResultSetWrapper wrapper) {
		Objects.requireNonNull(instance);
		Objects.requireNonNull(wrapper);
		final Class<?> clazz = instance.getClass();
		final String[] properties = properties(clazz);
		for (String property : properties) {
			try {
				final PropertyDescriptor descriptor = new PropertyDescriptor(property, clazz);
				final Object value = unpack(descriptor.getPropertyType(), wrapper.getIgnoreCase(property));
				descriptor.getWriteMethod().invoke(instance, value);
			} catch (Exception e) {
				LOGGER.info("设置实例属性异常：{}-{}", clazz, property, e);
			}
		}
	}
	
	/**
	 * <p>类型打包</p>
	 * <p>处理类型：枚举</p>
	 * 
	 * @param object 原始数据
	 * 
	 * @return 打包数据
	 */
	public static final Object pack(Object object) {
		if(object == null) {
			return null;
		}
		if(object instanceof Enum<?>) {
			// 枚举类型
			final Enum<?> value = (Enum<?>) object;
			return value.name();
		}
		return object;
	}
	
	/**
	 * <p>类型拆包</p>
	 * <p>处理类型：枚举、长字符串</p>
	 * 
	 * @param clazz 数据类型
	 * @param value 打包数据
	 * 
	 * @return 原始数据
	 */
	public static final Object unpack(Class<?> clazz, Object value) {
		if(clazz == null || value == null) {
			return null;
		}
		if(clazz.isEnum()) {
			// 枚举类型
			return unpackEnum(clazz, value);
		} else if(value instanceof JdbcClob) {
			// 长字符串
			return unpackJdbcClob(clazz, value);
		}
		return value;
	}
	
	/**
	 * <p>枚举拆包</p>
	 * 
	 * @param clazz 类型
	 * @param value 打包数据
	 * 
	 * @return 原始数据
	 */
	private static final Object unpackEnum(Class<?> clazz, Object value) {
		final var enums = clazz.getEnumConstants();
		for (Object object : enums) {
			if(object.toString().equals(value.toString())) {
				return object;
			}
		}
		return null;
	}
	
	/**
	 * <p>JdbcClob拆包</p>
	 * 
	 * @param clazz 类型
	 * @param value 打包数据
	 * 
	 * @return 原始数据
	 */
	private static final Object unpackJdbcClob(Class<?> clazz, Object value) {
		int index;
		final JdbcClob clob = (JdbcClob) value;
		final StringBuilder builder = new StringBuilder();
		try(final Reader reader = clob.getCharacterStream()) {
			final char[] chars = new char[1024];
			while((index = reader.read(chars)) != -1) {
				builder.append(new String(chars, 0, index));
			}
		} catch (SQLException | IOException e) {
			LOGGER.error("JdbcClob拆包异常：{}-{}", clazz, value, e);
		} finally {
			clob.free();
		}
		return builder.toString();
	}
	
}
