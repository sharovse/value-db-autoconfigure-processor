package ru.sharovse.spring.utils.db.values;

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
	
	Map<String, DataSource> mapDataSource = new HashMap<>();
	
	Map<String, ValueDbDataSourceBean> annonatedDataSource = new HashMap<>();
	Map<String, Map<String,ValueDb>> annonatedFields = new HashMap<>();
	Map<String, Map<String,ValueDb>> annonatedInnerFields = new HashMap<>();

	Map<String, NamedParameterJdbcTemplate> templates = new HashMap<>();
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext context = event.getApplicationContext();
		for (Entry<String, Map<String,ValueDb>> beanInfo : annonatedFields.entrySet()) {
			Object bean = context.getBean(beanInfo.getKey());
			for (Entry<String,ValueDb> propInfo  : beanInfo.getValue().entrySet()) {
				try {
					setValueFromDb(context, bean, propInfo.getKey(), propInfo.getValue());
				} catch (Exception e) {
					log.error(ERROR,e);
				}
			}
		}
	}
	static final String ERROR = "";
	
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
				if("".equals(valueDb.dataSourceAnnotation())){
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
	
	void addToValueDbDataSourceStore(ValueDbDataSourceBean annotation) {
		if(!annonatedDataSource.containsKey(annotation.name())){
			annonatedDataSource.put(annotation.name(), annotation);
		}
		annonatedDataSource.put(annotation.name(), annotation);
	}

	void addToInnerValueDbStore(String beanName, String name, ValueDb annotation) {
		if(!annonatedInnerFields.containsKey(beanName)){
			annonatedInnerFields.put(beanName, new HashMap<String,ValueDb>());
		}
		annonatedInnerFields.get(beanName).put(name, annotation);
	}

	
	void addToValueDbStore(String beanName, String name, ValueDb annotation) {
		if(!annonatedFields.containsKey(beanName)){
			annonatedFields.put(beanName, new HashMap<String,ValueDb>());
		}
		annonatedFields.get(beanName).put(name, annotation);
	}
	
	String getKeyInAnnonatedFields(String beanName, Field field){
		return beanName + "." + field.getName(); 
	}  

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (annonatedInnerFields.containsKey(beanName)) {
			for (Entry<String, Map<String,ValueDb>> beanInfo : annonatedInnerFields.entrySet()) {
				for (Entry<String,ValueDb> propInfo  : beanInfo.getValue().entrySet()) {
					try {
						setValueFromDb(null, bean, propInfo.getKey(), propInfo.getValue());
					} catch (Exception e) {
						throw new BeanDefinitionStoreException(String.format("Ошибка установки значения для %s", propInfo.getKey()), e);
					}
				}
			}
		}
		return bean;
	}
	
	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, ValueDb annotation) throws DataSourceNotFoundException {
		if(!"".equals(annotation.dataSourceAnnotation())){
			return getTemplateAsInnerBeanName(annotation.dataSourceAnnotation()); 
		}else{
			return getTemplateAsBeanName(context, annotation.dataSourceBean());
		}
	}

	private NamedParameterJdbcTemplate getTemplateAsInnerBeanName(String dataSourceAnnotation) {
		final String key = INNER_PREFIX + dataSourceAnnotation;
		if (!templates.containsKey(key)){
			ValueDbDataSourceBean dataSourceAnotation = annonatedDataSource.get(dataSourceAnnotation);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(createInnerDataSource(dataSourceAnotation));
			templates.put(key, namedParameterJdbcTemplate);	
		}
		return templates.get(key);
	}

	@Autowired
	ConfigurableListableBeanFactory factoryBean;
	
	private DataSource createInnerDataSource(ValueDbDataSourceBean dataSourceAnotation) {
		if(!mapDataSource.containsKey(dataSourceAnotation.name())){
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName( evaluateProperty(dataSourceAnotation.driverClassName(), dataSourceAnotation.propertyPrefix()) );
			dataSource.setUrl( evaluateProperty(dataSourceAnotation.url(), dataSourceAnotation.propertyPrefix()));
			dataSource.setUsername(evaluateProperty(dataSourceAnotation.username(), dataSourceAnotation.propertyPrefix()));
			dataSource.setPassword(evaluateProperty(dataSourceAnotation.pw(), dataSourceAnotation.propertyPrefix()));
			mapDataSource.put(dataSourceAnotation.name(), dataSource);
			if(dataSourceAnotation.registerToContext()){
				factoryBean.registerSingleton(dataSourceAnotation.name(), dataSource);
			}
			if(!"".equals(dataSourceAnotation.importSql())) {
				importScript(dataSourceAnotation.importSql(), dataSource);
			}
		}
		return mapDataSource.get(dataSourceAnotation.name());
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
			final String prop = prefix + (prefix.equals("")?"":ValueDbConstants.DOT) + m.group(1);
			return  environment.getProperty(prop);
		}
		return name;
	}
	
	NamedParameterJdbcTemplate getTemplateAsBeanName(ApplicationContext context, String beanDataSourceName) throws DataSourceNotFoundException {
		final String key = BEAN_PREFIX + beanDataSourceName;
		if (!templates.containsKey(key)){
			templates.put(key, createJdbcTemplate(context, beanDataSourceName));	
		}
		return templates.get(key);
	}

	public static final String BEAN_PREFIX = "bean.";
	public static final String INNER_PREFIX = "inner.";
	
	NamedParameterJdbcTemplate createJdbcTemplate(ApplicationContext context, String beanDataSourceName) throws DataSourceNotFoundException {
		try {
			final DataSource dataSource = context.getBean(beanDataSourceName, DataSource.class);
			return new NamedParameterJdbcTemplate(dataSource);
		} catch (Exception e) {
			throw new DataSourceNotFoundException();
		}
	}

	void setValueFromDb(ApplicationContext context, Object bean, String fieldName, ValueDb annotation) throws DataSourceNotFoundException {
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
	
	@SuppressWarnings("unchecked")
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
			if(!"".equals(annotation.valueColumnName())) {
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
