package ru.sharovse.spring.utils.db.values;

import static ru.sharovse.spring.utils.db.values.ValueDbConstants.NOT_SET;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import ru.sharovse.spring.utils.db.values.annotations.ValueDb;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSource;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDriverManagerDataSource;
import ru.sharovse.spring.utils.db.values.exceptions.DataSourceNotFoundException;

/**
 * Core library {@link BeanPostProcessor}. Find annotation, create cached
 * collections {@link DataSource}, eval {@link ValueDb} annotations and set
 * values to field.
 * 
 * @author sharov
 */
public class ValueDbAnnotationBeanPostProcessor
		implements BeanPostProcessor, Ordered, ApplicationListener<ContextRefreshedEvent> {

	private static Logger log = LoggerFactory.getLogger(ValueDbAnnotationBeanPostProcessor.class);

	@Autowired
	ConfigurableEnvironment environment;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext context = event.getApplicationContext();
		StoreItem<Map<String, ValueDb>> item = (String key, Map<String, ValueDb> value) -> {
			Object bean = context.getBean(key);
			for (Entry<String, ValueDb> propInfo : value.entrySet()) {
				try {
					setValueFromDb(context, bean, propInfo.getKey(), propInfo.getValue());
				} catch (Exception e) {
					log.error(ERROR, e);
				}
			}
		};
		storeAnnonatedFields.forEach(item);
	}

	static final String ERROR = NOT_SET;

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		Class<?> userClass = ClassUtils.getUserClass(bean.getClass());
		for (Field field : userClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(ValueDb.class)) {
				final ValueDb valueDb = field.getAnnotation(ValueDb.class);
				if (NOT_SET.equals(valueDb.dataSourceAnnotation())) {
					addToValueDbStore(beanName, field.getName(), valueDb);
				} else {
					addToInnerValueDbStore(beanName, field.getName(), valueDb);
				}
			}
			if (field.isAnnotationPresent(ValueDbDriverManagerDataSource.class)) {
				addToValueDbDataSourceStore(field.getAnnotation(ValueDbDriverManagerDataSource.class));
			}
			if (field.isAnnotationPresent(ValueDbDataSource.class)) {
				addToValueDbDataSourceStore(field.getAnnotation(ValueDbDataSource.class));
			}

		}
		return bean;
	}

	StoreValues<ValueDbDataSource> storeAnnonatedDataSource = new StoreValues<>();

	void addToValueDbDataSourceStore(ValueDbDataSource annotation) {
		StoreValue<ValueDbDataSource> createValue = () -> annotation;
		storeAnnonatedDataSource.createAndGetValue(annotation.name(), createValue);
	}

	StoreValues<ValueDbDriverManagerDataSource> storeDriverManagerAnnonatedDataSource = new StoreValues<>();

	void addToValueDbDataSourceStore(ValueDbDriverManagerDataSource annotation) {
		StoreValue<ValueDbDriverManagerDataSource> createValue = () -> annotation;
		storeDriverManagerAnnonatedDataSource.createAndGetValue(annotation.name(), createValue);
	}

	StoreValues<Map<String, ValueDb>> storeAnnonatedInnerFields = new StoreValues<>();

	void addToInnerValueDbStore(String beanName, String name, ValueDb annotation) {
		addToValueDbStore(beanName, name, annotation, storeAnnonatedInnerFields);
	}

	StoreValues<Map<String, ValueDb>> storeAnnonatedFields = new StoreValues<>();

	void addToValueDbStore(String beanName, String name, ValueDb annotation) {
		addToValueDbStore(beanName, name, annotation, storeAnnonatedFields);
	}

	void addToValueDbStore(String beanName, String name, ValueDb annotation, StoreValues<Map<String, ValueDb>> store) {
		StoreValue<Map<String, ValueDb>> createValue = HashMap::new;
		store.createAndGetValue(beanName, createValue).put(name, annotation);
	}

	String getKeyInAnnonatedFields(String beanName, Field field) {
		return beanName + ValueDbConstants.DOT + field.getName();
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (storeAnnonatedInnerFields.isContainKey(beanName)) {
			for (Entry<String, ValueDb> propInfo : storeAnnonatedInnerFields.get(beanName).entrySet()) {
				try {
					setValueFromDb(null, bean, propInfo.getKey(), propInfo.getValue());
				} catch (Exception e) {
					throw new BeanDefinitionStoreException(String.format("Error set value of %s", propInfo.getKey()),
							e);
				}
			}
		}
		return bean;
	}

	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, ValueDb annotation) {
		if (NOT_SET.equals(annotation.dataSourceAnnotation()) && NOT_SET.equals(annotation.dataSourceBean())) {
			throw new BeanDefinitionStoreException(
					String.format("Error. Must be will set property dataSourceAnnotation or dataSourceBean of %s",
							annotation.getClass()));
		}
		if (!NOT_SET.equals(annotation.dataSourceAnnotation())) {
			return getTemplateAsInnerBeanName(annotation.dataSourceAnnotation());
		} else {
			return getTemplateAsBeanName(context, annotation.dataSourceBean());
		}
	}

	@Autowired
	ConfigurableListableBeanFactory factoryBean;

	StoreValues<DataSource> storeDataSource = new StoreValues<>();

	/**
	 * Create and store to cache inner simple {@link DriverManagerDataSource}. Set
	 * only 4 properties from annotation.
	 * 
	 * @param dataSourceAnotation annotation dataSource.
	 * @return {@link DataSource}.
	 */
	protected DataSource createInnerDataSource(ValueDbDriverManagerDataSource dataSourceAnotation) {
		StoreValue<DataSource> createValue = () -> {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(
					evaluateProperty(dataSourceAnotation.driverClassName(), dataSourceAnotation.propertyPrefix()));
			dataSource.setUrl(evaluateProperty(dataSourceAnotation.url(), dataSourceAnotation.propertyPrefix()));
			dataSource.setUsername(
					evaluateProperty(dataSourceAnotation.username(), dataSourceAnotation.propertyPrefix()));
			dataSource.setPassword(evaluateProperty(dataSourceAnotation.pw(), dataSourceAnotation.propertyPrefix()));
			if (dataSourceAnotation.registerToContext()) {
				factoryBean.registerSingleton(dataSourceAnotation.name(), dataSource);
			}
			if (!NOT_SET.equals(dataSourceAnotation.importSql())) {
				importScript(dataSourceAnotation.importSql(), dataSource);
			}
			return dataSource;
		};
		return storeDataSource.createAndGetValue(dataSourceAnotation.name(), createValue);
	}

	/**
	 * Create and store to cache inner dataSource (as className). Set any not null
	 * properties from PropertySource.
	 * 
	 * @param dataSourceAnotation annotation dataSource.
	 * @return {@link DataSource}
	 */
	protected DataSource createInnerDataSource(ValueDbDataSource dataSourceAnotation) {
		StoreValue<DataSource> createValue = () -> {
			DataSource dataSource = createDataSourceFromClass(dataSourceAnotation);
			if (dataSourceAnotation.registerToContext()) {
				factoryBean.registerSingleton(dataSourceAnotation.name(), dataSource);
			}
			if (!NOT_SET.equals(dataSourceAnotation.importSql())) {
				importScript(dataSourceAnotation.importSql(), dataSource);
			}
			return dataSource;
		};
		return storeDataSource.createAndGetValue(dataSourceAnotation.name(), createValue);
	}

	static final String PROPERTY_FORMAT = "${%s}";

	protected DataSource createDataSourceFromClass(ValueDbDataSource dataSourceAnotation) {
		return createObjectFromClassNameAndProperies(dataSourceAnotation.className(),
				dataSourceAnotation.propertyPrefix());
	}

	@SuppressWarnings("unchecked")
	public <T> T createObjectFromClassNameAndProperies(String className, String prefix) {
		try {
			Class<?> clazz = Class.forName(className);
			Object dataSource = clazz.newInstance();
			for (Field field : getFields(clazz)) {
				String value = evaluateProperty(String.format(PROPERTY_FORMAT, field.getName()), prefix);
				if (value != null) {
					boolean access = field.isAccessible();
					field.setAccessible(true);
					field.set(dataSource, value);
					field.setAccessible(access);
				}
			}
			return (T) dataSource;
		} catch (Exception e) {
			throw new BeanDefinitionStoreException("create object from " + className + " error ", e);
		}
	}

	List<Field> getFields(Class<?> clazz) {
		List<Field> list = new LinkedList<>();
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				list.add(field);
			}
			clazz = clazz.getSuperclass();
		}
		return list;
	}

	/**
	 * Execute sql script.
	 * 
	 * @param fileSql    {@link ClassPathResource} file SQL.
	 * @param dataSource {@link DataSource}.
	 */
	void importScript(String fileSql, DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(fileSql)), false, false,
					ScriptUtils.DEFAULT_COMMENT_PREFIX, ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
					ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		} catch (Exception e) {
			log.error(ERROR, e);
		}
	}

	private Pattern varPattern = Pattern.compile("^\\$\\{(.*)\\}$");

	/**
	 * Get prefixed property value.
	 * 
	 * @param name   name property.
	 * @param prefix prefix variables form PropertySource.
	 * @return Value
	 */
	protected String evaluateProperty(String name, String prefix) {
		Matcher m = varPattern.matcher(name);
		if (m.find()) {
			final String prop = prefix + (prefix.equals(NOT_SET) ? NOT_SET : ValueDbConstants.DOT) + m.group(1);
			return environment.getProperty(prop);
		}
		return name;
	}

	StoreValues<NamedParameterJdbcTemplate> storeTemplates = new StoreValues<>();

	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, String beanDataSourceName) {
		final String key = BEAN_PREFIX + beanDataSourceName;
		StoreValue<NamedParameterJdbcTemplate> createValue = () -> createJdbcTemplate(context, beanDataSourceName);
		return storeTemplates.createAndGetValue(key, createValue);
	}

	NamedParameterJdbcTemplate getTemplateAsInnerBeanName(String dataSourceAnnotation) {
		final String key = INNER_PREFIX + dataSourceAnnotation;
		if (storeDriverManagerAnnonatedDataSource.isContainKey(dataSourceAnnotation)) {
			StoreValue<NamedParameterJdbcTemplate> createValue = () -> {
				ValueDbDriverManagerDataSource dataSourceAnotation = storeDriverManagerAnnonatedDataSource
						.get(dataSourceAnnotation);
				return new NamedParameterJdbcTemplate(createInnerDataSource(dataSourceAnotation));
			};
			return storeTemplates.createAndGetValue(key, createValue);
		} else {
			StoreValue<NamedParameterJdbcTemplate> createValue = () -> {
				ValueDbDataSource dataSourceAnotation = storeAnnonatedDataSource.get(dataSourceAnnotation);
				return new NamedParameterJdbcTemplate(createInnerDataSource(dataSourceAnotation));
			};
			return storeTemplates.createAndGetValue(key, createValue);
		}
	}

	public static final String BEAN_PREFIX = "bean.";
	public static final String INNER_PREFIX = "inner.";

	NamedParameterJdbcTemplate createJdbcTemplate(ApplicationContext context, String beanDataSourceName) {
		try {
			final DataSource dataSource = context.getBean(beanDataSourceName, DataSource.class);
			return new NamedParameterJdbcTemplate(dataSource);
		} catch (Exception e) {
			throw new DataSourceNotFoundException();
		}
	}

	/**
	 * Execute ValueDb and Set value in bean field.
	 * 
	 * @param context    Spring context for search DataSource (if
	 *                   {@link ValueDb#dataSourceBean()} set). Otherwise, read
	 *                   DataSource from inner cache.
	 * @param bean       target field
	 * @param fieldName  field name
	 * @param annotation ValueDb annotation
	 */
	void setValueFromDb(ApplicationContext context, Object bean, String fieldName, ValueDb annotation) {
		NamedParameterJdbcTemplate template = getTemplateAsBeanName(context, annotation);
		Map<String, Object> pars = new HashMap<>();
		pars.put(annotation.argPropertyName(), fieldName);
		List<Map<String, Object>> list = template.queryForList(annotation.valueSql(), pars);
		if (!list.isEmpty()) {
			Field field = ReflectionUtils.findField(bean.getClass(), fieldName);
			ReflectionUtils.makeAccessible(field);
			Object value = getValue(field, list, annotation);
			if (value != null) {
				ReflectionUtils.setField(field, bean, value);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Object getValue(Field field, List<Map<String, Object>> list, ValueDb annotation) {
		Class<?> type = field.getType();
		if (list.isEmpty())
			return null;
		if (type.isArray()) {
			return toArray(getListColumValue(list, annotation), (Class) type.getComponentType());
		} else if (type.equals(List.class)) {
			if (field.getGenericType() != null
					&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].getTypeName()
							.startsWith(Map.class.getName())) {
				return list;
			} else {
				return getListColumValue(list, annotation);
			}
		} else if (type.equals(Map.class)) {
			return list.get(0);
		} else {
			return getOneColumnValue(list.get(0), annotation);
		}
	}

	public static <T> T[] toArray(Collection<T> list, Class<T> clazz) {
		final int len = (list == null) ? 0 : list.size();
		@SuppressWarnings("unchecked")
		T[] blank = (T[]) Array.newInstance(clazz, len);
		if (list != null && len > 0) {
			return list.toArray(blank);
		}
		return blank;
	}

	List<Object> getListColumValue(List<Map<String, Object>> list, ValueDb annotation) {
		List<Object> result = new LinkedList<>();
		if (list != null) {
			for (Map<String, Object> cols : list) {
				result.add(getOneColumnValue(cols, annotation));
			}
		}
		return result;
	}

	Object getOneColumnValue(Map<String, Object> cols, ValueDb annotation) {
		if (cols != null && !cols.isEmpty()) {
			if (!NOT_SET.equals(annotation.valueColumnName())) {
				return cols.get(annotation.valueColumnName());
			} else {
				int i = 1;
				for (Object value : cols.values()) {
					if (i++ == annotation.valueColumnNumber()) {
						return value;
					}
				}
			}
		}
		return null;
	}

}
