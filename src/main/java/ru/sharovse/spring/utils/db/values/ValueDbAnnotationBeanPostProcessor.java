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
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSourceBean;
import ru.sharovse.spring.utils.db.values.exceptions.DataSourceNotFoundException;

public class ValueDbAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered, ApplicationListener<ContextRefreshedEvent> {

	private static Logger log = LoggerFactory.getLogger(ValueDbAnnotationBeanPostProcessor.class);
	
	@Autowired
	ConfigurableEnvironment environment;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext context = event.getApplicationContext();
		storeAnnonatedFields.forEach(new StoreItem<HashMap<String,ValueDb>>() {
			@Override
			public void item(String key, HashMap<String, ValueDb> value) {
				Object bean = context.getBean(key);
				for (Entry<String,ValueDb> propInfo  : value.entrySet()) {
					try {
						setValueFromDb(context, bean, propInfo.getKey(), propInfo.getValue());
					} catch (Exception e) {
						log.error(ERROR,e);
					}
				}	
			}
		});
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
				if(NOT_SET.equals(valueDb.dataSourceAnnotation())){
					addToValueDbStore(beanName, field.getName(), valueDb);
				}else{
					addToInnerValueDbStore(beanName, field.getName(), valueDb);
				}
			}
			if(field.isAnnotationPresent(ValueDbDataSourceBean.class)){
				addToValueDbDataSourceStore(field.getAnnotation(ValueDbDataSourceBean.class));
			}
		}
		return bean;
	}
	
	StoreValues<ValueDbDataSourceBean> storeAnnonatedDataSource = new StoreValues<>();
	void addToValueDbDataSourceStore(ValueDbDataSourceBean annotation) {
		storeAnnonatedDataSource.createAndGetValue(annotation.name(), new StoreValue<ValueDbDataSourceBean>() {
			@Override
			public ValueDbDataSourceBean createValue(String key) {
				return annotation;
			}
		});
	}

	StoreValues<HashMap<String,ValueDb>> storeAnnonatedInnerFields = new StoreValues<>();
	void addToInnerValueDbStore(String beanName, String name, ValueDb annotation) {
		addToValueDbStore(beanName, name, annotation, storeAnnonatedInnerFields);
	}
	
	StoreValues<HashMap<String,ValueDb>> storeAnnonatedFields = new StoreValues<>();
	void addToValueDbStore(String beanName, String name, ValueDb annotation) {
		addToValueDbStore(beanName, name, annotation, storeAnnonatedFields);
	}
	
	void addToValueDbStore(String beanName, String name, ValueDb annotation, StoreValues<HashMap<String,ValueDb>> store) {
		store.createAndGetValue(beanName, new StoreValue<HashMap<String,ValueDb>>() {
			@Override
			public HashMap<String,ValueDb> createValue(String key) {
				return new HashMap<>();
			}
		}).put(name, annotation);
	}
	
	String getKeyInAnnonatedFields(String beanName, Field field){
		return beanName + "." + field.getName(); 
	}  

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if(storeAnnonatedInnerFields.isContainKey(beanName)){
			for (Entry<String,ValueDb> propInfo  : storeAnnonatedInnerFields.get(beanName).entrySet()) {
				try {
					setValueFromDb(null, bean, propInfo.getKey(), propInfo.getValue());
				} catch (Exception e) {
					throw new BeanDefinitionStoreException(String.format("Ошибка установки значения для %s", propInfo.getKey()), e);
				}
			}
		}
		return bean;
	}
	
	
	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, ValueDb annotation) throws DataSourceNotFoundException {
		if(!NOT_SET.equals(annotation.dataSourceAnnotation())){
			return getTemplateAsInnerBeanName(annotation.dataSourceAnnotation()); 
		}else{
			return getTemplateAsBeanName(context, annotation.dataSourceBean());
		}
	}

	@Autowired
	ConfigurableListableBeanFactory factoryBean;
	
	StoreValues<DataSource> storeDataSource = new StoreValues<>();
	
	private DataSource createInnerDataSource(ValueDbDataSourceBean dataSourceAnotation) {
		return storeDataSource.createAndGetValue(dataSourceAnotation.name(), new StoreValue<DataSource>() {
			@Override
			public DataSource createValue(String key) {
				DriverManagerDataSource dataSource = new DriverManagerDataSource();
				dataSource.setDriverClassName( evaluateProperty(dataSourceAnotation.driverClassName(), dataSourceAnotation.propertyPrefix()) );
				dataSource.setUrl( evaluateProperty(dataSourceAnotation.url(), dataSourceAnotation.propertyPrefix()));
				dataSource.setUsername(evaluateProperty(dataSourceAnotation.username(), dataSourceAnotation.propertyPrefix()));
				dataSource.setPassword(evaluateProperty(dataSourceAnotation.pw(), dataSourceAnotation.propertyPrefix()));
				if(dataSourceAnotation.registerToContext()){
					factoryBean.registerSingleton(dataSourceAnotation.name(), dataSource);
				}
				if(!NOT_SET.equals(dataSourceAnotation.importSql())) {
					importScript(dataSourceAnotation.importSql(), dataSource);
				}
				return dataSource;
			}
		});
	}

	void importScript(String fileSql, DataSource dataSource) {
		try(Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(
					connection, 
					new EncodedResource(new ClassPathResource(fileSql)), 
					false, false,
					ScriptUtils.DEFAULT_COMMENT_PREFIX,
					ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
					ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
					ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		} catch (Exception e) {
			log.error(ERROR,e);
		}
	}
	
	private Pattern varPattern = Pattern.compile("^\\$\\{(.*)\\}$");
	String evaluateProperty(String name, String prefix){
		Matcher m = varPattern.matcher(name);
		if(m.find()){
			final String prop = prefix + (prefix.equals(NOT_SET)?NOT_SET:ValueDbConstants.DOT) + m.group(1);
			return  environment.getProperty(prop);
		}
		return name;
	}

	StoreValues<NamedParameterJdbcTemplate> storeTemplates = new StoreValues<>();
	
	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, String beanDataSourceName) {
		final String key = BEAN_PREFIX + beanDataSourceName;
		return storeTemplates.createAndGetValue(key, new StoreValue<NamedParameterJdbcTemplate>() {
			@Override
			public NamedParameterJdbcTemplate createValue(String key) {
				return createJdbcTemplate(context, beanDataSourceName);
			}
		});
	}

	private NamedParameterJdbcTemplate getTemplateAsInnerBeanName(String dataSourceAnnotation) {
		final String key = INNER_PREFIX + dataSourceAnnotation;
		return storeTemplates.createAndGetValue(key, new StoreValue<NamedParameterJdbcTemplate>() {
			@Override
			public NamedParameterJdbcTemplate createValue(String key) {
				ValueDbDataSourceBean dataSourceAnotation =  storeAnnonatedDataSource.get(dataSourceAnnotation);
				return new NamedParameterJdbcTemplate(createInnerDataSource(dataSourceAnotation));
			}
		});
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

	void setValueFromDb(ApplicationContext context, Object bean, String fieldName, ValueDb annotation) {
		NamedParameterJdbcTemplate template = getTemplateAsBeanName(context, annotation);
		Map<String, Object> pars = new HashMap<>();
		pars.put(annotation.argPropertyName(), fieldName);
		List<Map<String, Object>> list = template.queryForList(annotation.valueSql(), pars);
		if(!list.isEmpty()) {
			Field field = ReflectionUtils.findField(bean.getClass(), fieldName);
			ReflectionUtils.makeAccessible(field);
			Object value = getValue(field, list, annotation);
			if(value!=null) {
				ReflectionUtils.setField(field, bean, value);
			}
		}
	}
	
	Object getValue(Field field, List<Map<String, Object>> list, ValueDb annotation) {
		Class<?> type = field.getType();
		if(list.isEmpty()) return null;
		if (type.isArray()) {
			return toArray(getListColumValue(list, annotation),(Class)type.getComponentType());
		} else if (type.equals(List.class)) {
			if(field.getGenericType()!=null && ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0].getTypeName().startsWith(Map.class.getName())) {
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
		final int len = (list==null)? 0 : list.size();
		@SuppressWarnings("unchecked")
		T[] blank = (T[]) Array.newInstance(clazz, len);
		if (list != null && len > 0) {
			return list.toArray(blank);
		}
		return blank;
	}
	
	List<Object> getListColumValue(List<Map<String, Object>> list, ValueDb annotation){
		List<Object> result = new LinkedList<>();
		for (Map<String, Object> cols : list) {
			result.add( getOneColumnValue(cols, annotation) );
		}
		return result;
	}
	
	Object getOneColumnValue(Map<String, Object> cols, ValueDb annotation){
		if(!cols.isEmpty()) {
			if(!NOT_SET.equals(annotation.valueColumnName())) {
				return cols.get(annotation.valueColumnName());
			}else {
				int i = 1;
				for (Object value: cols.values()) {
					if(i++ == annotation.valueColumnNumber()) {
						return value;
					}
				}
			}
		}
		return null;
	}

	
}
