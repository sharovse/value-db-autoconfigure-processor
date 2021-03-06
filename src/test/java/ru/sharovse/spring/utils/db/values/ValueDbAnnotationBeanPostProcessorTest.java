package ru.sharovse.spring.utils.db.values;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.sharovse.spring.utils.db.values.ValueDbConstants.NOT_SET;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ru.sharovse.spring.utils.db.values.annotations.ValueDb;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSource;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDriverManagerDataSource;
import ru.sharovse.spring.utils.db.values.exceptions.DataSourceNotFoundException;
import ru.sharovse.spring.utils.db.values.test.StartTestApplication;

/**
 * @author sharov
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ValueDbAnnotationBeanPostProcessorTest {

	@Mock
	ConfigurableEnvironment environment;

	ValueDbAnnotationBeanPostProcessor service;

	@Before
	public void setUp() throws Exception {
		service = spy(new ValueDbAnnotationBeanPostProcessor());
		service.environment = environment;
		service.factoryBean = factoryBean;
	}

	@Test
	public void testEvaluateProperty() {
		String value = "value";
		String var = "var";
		String name = "${" + var + "}";
		String prefix = "prefix";
		doReturn(value).when(environment).getProperty(eq(prefix + ValueDbConstants.DOT + var));
		assertEquals(value, service.evaluateProperty(name, prefix));
	}

	@Mock
	ContextRefreshedEvent event;
	@Mock
	ApplicationContext context;

	StoreValues<Map<String, ValueDb>> storeAnnonatedFields = new StoreValues<>();

	@Mock
	ValueDb valueDb;

	@Test
	public void testOnApplicationEvent() {
		doReturn(context).when(event).getApplicationContext();
		Map<String, ValueDb> map = new HashMap<>();
		map.put(key, valueDb);
		StoreValue<Map<String, ValueDb>> createValue = () -> map;
		storeAnnonatedFields.createAndGetValue(key, createValue);
		doReturn(bean).when(context).getBean(eq(key));
		service.storeAnnonatedFields = storeAnnonatedFields;
		doNothing().when(service).setValueFromDb(eq(context), eq(bean), eq(key), eq(valueDb));
		service.onApplicationEvent(event);
		doThrow(new DataSourceNotFoundException()).when(service).setValueFromDb(eq(context), eq(bean), eq(key),
				eq(valueDb));
		service.onApplicationEvent(event);

	}

	StartTestApplication bean = new StartTestApplication();
	String key = "key";

	@Test
	public void testGetOrder() {
		assertTrue(service.getOrder() != 0);
	}

	String beanName = "startTestApplication";

	@Test
	public void testPostProcessBeforeInitialization() {

		doNothing().when(service).addToValueDbStore(eq(beanName), eq("beanDataSourceId"), any(ValueDb.class));
		doNothing().when(service).addToInnerValueDbStore(eq(beanName), eq("code"), any(ValueDb.class));
		doNothing().when(service).addToInnerValueDbStore(eq(beanName), eq("id"), any(ValueDb.class));
		doNothing().when(service).addToValueDbDataSourceStore(any(ValueDbDriverManagerDataSource.class));

		service.postProcessBeforeInitialization(bean, beanName);

		verify(service, times(1)).addToValueDbStore(eq(beanName), eq("beanDataSourceId"), any(ValueDb.class));
		verify(service, times(1)).addToInnerValueDbStore(eq(beanName), eq("code"), any(ValueDb.class));
		verify(service, times(1)).addToInnerValueDbStore(eq(beanName), eq("id"), any(ValueDb.class));
		verify(service, times(1)).addToValueDbDataSourceStore(any(ValueDbDriverManagerDataSource.class));
	}

	@Mock
	ValueDbDriverManagerDataSource valueDbDataSourceBean;

	@Test
	public void testAddToValueDbDataSourceStore() {
		doReturn(key).when(valueDbDataSourceBean).name();
		service.addToValueDbDataSourceStore(valueDbDataSourceBean);
		assertEquals(valueDbDataSourceBean, service.storeDriverManagerAnnonatedDataSource.get(key));
	}

	String property = "id";

	@Test
	public void testAddToInnerValueDbStore() {
		doNothing().when(service).addToValueDbStore(eq(beanName), eq(property), eq(valueDb),
				eq(service.storeAnnonatedInnerFields));
		service.addToInnerValueDbStore(beanName, property, valueDb);
		verify(service).addToValueDbStore(eq(beanName), eq(property), eq(valueDb),
				eq(service.storeAnnonatedInnerFields));
	}

	@Test
	public void testAddToValueDbStoreStringStringValueDb() {
		doNothing().when(service).addToValueDbStore(eq(beanName), eq(property), eq(valueDb),
				eq(service.storeAnnonatedFields));
		service.addToValueDbStore(beanName, property, valueDb);
		verify(service).addToValueDbStore(eq(beanName), eq(property), eq(valueDb), eq(service.storeAnnonatedFields));
	}

	@Test
	public void testAddToValueDbStoreStringStringValueDbStoreValuesOfMapOfStringValueDb() {
		StoreValues<Map<String, ValueDb>> store = new StoreValues<>();
		service.addToValueDbStore(beanName, property, valueDb, store);
		assertEquals(valueDb, store.get(beanName).get(property));
	}

	@Test
	public void testGetKeyInAnnonatedFields() throws NoSuchFieldException, SecurityException {
		String name = "id";
		Field field = StartTestApplication.class.getDeclaredField(name);
		assertEquals(beanName + ValueDbConstants.DOT + name, service.getKeyInAnnonatedFields(beanName, field));
	}

	@Test
	public void testPostProcessAfterInitialization() {
		Map<String, ValueDb> map = new HashMap<>();
		map.put(property, valueDb);
		StoreValue<Map<String, ValueDb>> createValue = () -> map;
		service.storeAnnonatedInnerFields.createAndGetValue(beanName, createValue);
		doNothing().when(service).setValueFromDb((ApplicationContext) isNull(), eq(bean), eq(property), eq(valueDb));

		service.postProcessAfterInitialization(bean, beanName);
		verify(service).setValueFromDb((ApplicationContext) isNull(), eq(bean), eq(property), eq(valueDb));
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testPostProcessAfterInitializationException() {
		Map<String, ValueDb> map = new HashMap<>();
		map.put(property, valueDb);
		StoreValue<Map<String, ValueDb>> createValue = () -> map;
		service.storeAnnonatedInnerFields.createAndGetValue(beanName, createValue);
		doNothing().when(service).setValueFromDb((ApplicationContext) isNull(), eq(bean), eq(property), eq(valueDb));

		doThrow(new DataSourceNotFoundException()).when(service).setValueFromDb((ApplicationContext) isNull(), eq(bean),
				eq(property), eq(valueDb));
		service.postProcessAfterInitialization(bean, beanName);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testGetTemplateAsBeanNameApplicationContextValueDbException() {
		doReturn(ValueDbConstants.NOT_SET).when(valueDb).dataSourceAnnotation();
		doReturn(ValueDbConstants.NOT_SET).when(valueDb).dataSourceBean();
		service.getTemplateAsBeanName(context, valueDb);
	}

	@Test
	public void testGetTemplateAsBeanNameApplicationContextValueDb() {
		doReturn(beanName).when(valueDb).dataSourceAnnotation();
		doReturn(ValueDbConstants.NOT_SET).when(valueDb).dataSourceBean();

		doReturn(null).when(service).getTemplateAsInnerBeanName(eq(beanName));
		doReturn(null).when(service).getTemplateAsBeanName(eq(context), eq(beanName));

		service.getTemplateAsBeanName(context, valueDb);

		verify(service, times(1)).getTemplateAsInnerBeanName(eq(beanName));

		doReturn(ValueDbConstants.NOT_SET).when(valueDb).dataSourceAnnotation();
		doReturn(beanName).when(valueDb).dataSourceBean();

		service.getTemplateAsBeanName(context, valueDb);

		verify(service, times(1)).getTemplateAsBeanName(eq(context), eq(beanName));
	}

	@Mock
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Test
	public void testGetTemplateAsBeanNameApplicationContextString() {
		final String key = service.BEAN_PREFIX + beanName;
		doReturn(namedParameterJdbcTemplate).when(service).createJdbcTemplate(eq(context), eq(beanName));
		assertEquals(namedParameterJdbcTemplate, service.getTemplateAsBeanName(context, beanName));
		assertEquals(namedParameterJdbcTemplate, service.storeTemplates.get(key));

	}

	@Mock
	DataSource dataSource;

	@Test(expected = DataSourceNotFoundException.class)
	public void testCreateJdbcTemplate() {
		// TODO
		doReturn(dataSource).when(context).getBean(eq(beanName));
		assertNotNull(service.createJdbcTemplate(context, beanName));
	}

	@Test
	public void testSetValueFromDb() {
		String sql = "sql";
		Map<String, Object> row = new HashMap<>();
		List<Map<String, Object>> list = Arrays.asList(row);
		String value = "value";
		doReturn(sql).when(valueDb).valueSql();

		doReturn(property).when(valueDb).argPropertyName();

		ArgumentCaptor<Map> a = ArgumentCaptor.forClass(Map.class);
		doReturn(namedParameterJdbcTemplate).when(service).getTemplateAsBeanName(eq(context), eq(valueDb));
		doReturn(list).when(namedParameterJdbcTemplate).queryForList(eq(sql), a.capture());
		doReturn(value).when(service).getValue(any(Field.class), eq(list), eq(valueDb));
		service.setValueFromDb(context, bean, property, valueDb);
		assertEquals(value, bean.getId());
		assertEquals(property, a.getValue().get(property));
	}

	@Test
	public void testGetValueListIsEmpty() throws NoSuchFieldException, SecurityException {
		String name = "id";
		Field field = StartTestApplication.class.getDeclaredField(name);
		List<Map<String, Object>> list = Arrays.asList();
		assertNull(service.getValue(field, list, valueDb));
	}

	String[] array;

	@Test
	public void testGetValueIsArray() throws NoSuchFieldException, SecurityException {
		String name = "array";
		Field field = this.getClass().getDeclaredField(name);
		Map<String, Object> row = new HashMap<>();
		List<Map<String, Object>> list = Arrays.asList(row);
		String value = "value";
		List<Object> retList = Arrays.asList(value);
		doReturn(retList).when(service).getListColumValue(eq(list), eq(valueDb));
		array = (String[]) service.getValue(field, list, valueDb);
		assertNotNull(array);
		assertEquals(value, array[0]);
	}

	List<String> list;

	@Test
	public void testGetValueIsList() throws NoSuchFieldException, SecurityException {
		String name = "list";
		Field field = this.getClass().getDeclaredField(name);
		Map<String, Object> row = new HashMap<>();
		List<Map<String, Object>> listRows = Arrays.asList(row);
		String value = "value";
		List<Object> retList = Arrays.asList(value);
		doReturn(retList).when(service).getListColumValue(eq(listRows), eq(valueDb));
		list = (List<String>) service.getValue(field, listRows, valueDb);
		assertNotNull(list);
		assertEquals(value, list.get(0));
	}

	List<Map<String, Object>> listRowMap;

	@Test
	public void testGetValueIsListMap() throws NoSuchFieldException, SecurityException {
		String name = "listRowMap";
		Field field = this.getClass().getDeclaredField(name);
		Map<String, Object> row = new HashMap<>();
		List<Map<String, Object>> listRows = Arrays.asList(row);
		String value = "value";
		List<Object> retList = Arrays.asList(value);
		doReturn(retList).when(service).getListColumValue(eq(listRows), eq(valueDb));
		listRowMap = (List<Map<String, Object>>) service.getValue(field, listRows, valueDb);
		assertNotNull(listRowMap);
		assertEquals(listRows, listRowMap);
	}

	Map<String, Object> rowMap;

	@Test
	public void testGetValueIsMap() throws NoSuchFieldException, SecurityException {
		String name = "rowMap";
		Field field = this.getClass().getDeclaredField(name);
		Map<String, Object> row = new HashMap<>();
		List<Map<String, Object>> listRows = Arrays.asList(row);
		String value = "value";
		List<Object> retList = Arrays.asList(value);
		doReturn(retList).when(service).getListColumValue(eq(listRows), eq(valueDb));
		rowMap = (Map<String, Object>) service.getValue(field, listRows, valueDb);
		assertNotNull(rowMap);
		assertEquals(row, rowMap);
	}

	String id;

	@Test
	public void testGetValueIsSingle() throws NoSuchFieldException, SecurityException {
		String name = "id";
		Field field = this.getClass().getDeclaredField(name);
		Map<String, Object> row = new HashMap<>();
		String value = "value";
		String column = "code";
		row.put(column, value);
		List<Map<String, Object>> listRows = Arrays.asList(row);
		List<Object> retList = Arrays.asList(value);
		doReturn(retList).when(service).getListColumValue(eq(listRows), eq(valueDb));
		doReturn(column).when(valueDb).valueColumnName();
		id = (String) service.getValue(field, listRows, valueDb);
		assertNotNull(id);
		assertEquals(value, id);
	}

	@Test
	public void testGetListColumValue() {
		Map<String, Object> row = new HashMap<>();
		String value = "value";
		String column = "code";
		row.put(column, value);
		List<Map<String, Object>> listRows = Arrays.asList(row);
		doReturn(value).when(service).getOneColumnValue(eq(row), eq(valueDb));
		List<Object> result = service.getListColumValue(listRows, valueDb);
		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(value, result.get(0));
	}

	@Test
	public void testGetOneColumnValue() {
		assertNull(service.getOneColumnValue(null, valueDb));
		Map<String, Object> row = new HashMap<>();
		assertNull(service.getOneColumnValue(row, valueDb));

		String value = "value1";
		String column = "code1";
		row.put(column, value);

		String value2 = "value2";
		String column2 = "code2";
		row.put(column2, value2);

		doReturn(column).when(valueDb).valueColumnName();
		assertEquals(value, service.getOneColumnValue(row, valueDb));

		doReturn(ValueDbConstants.NOT_SET).when(valueDb).valueColumnName();
		doReturn(1).when(valueDb).valueColumnNumber();

		assertEquals(value2, service.getOneColumnValue(row, valueDb));

		doReturn(2).when(valueDb).valueColumnNumber();
		assertEquals(value, service.getOneColumnValue(row, valueDb));
	}

	@Mock
	ConfigurableListableBeanFactory factoryBean;
	@Mock
	ValueDbDataSource dataSourceAnotation;

	@Test
	public void testTcreateInnerDataSourceValueDbDataSource() {
		doReturn(dataSource).when(service).createDataSourceFromClass(eq(dataSourceAnotation));
		String importSql = "importSql";
		String name = "name";
		doReturn(name).when(dataSourceAnotation).name();
		doReturn(importSql).when(dataSourceAnotation).importSql();
		doReturn(true).when(dataSourceAnotation).registerToContext();

		doNothing().when(factoryBean).registerSingleton(eq(name), eq(dataSource));
		doNothing().when(service).importScript(eq(importSql), eq(dataSource));

		assertEquals(dataSource, service.createInnerDataSource(dataSourceAnotation));

		verify(service, times(1)).importScript(eq(importSql), eq(dataSource));
		verify(factoryBean, times(1)).registerSingleton(eq(name), eq(dataSource));

		service.storeDataSource.map.clear();
		doReturn(NOT_SET).when(dataSourceAnotation).importSql();
		doReturn(false).when(dataSourceAnotation).registerToContext();

		assertEquals(dataSource, service.createInnerDataSource(dataSourceAnotation));

		verify(service, times(1)).importScript(eq(importSql), eq(dataSource));
		verify(factoryBean, times(1)).registerSingleton(eq(name), eq(dataSource));
	}

	@Mock
	ValueDbDriverManagerDataSource valueDbDriverManagerDataSource;

	String prefix = "prefix";

	@Test
	public void testTcreateInnerDataSourceValueDbDriverManagerDataSourceNotImportAndNotRegisterBean() {
		String importSql = "importSql";
		String name = "name";
		doReturn(name).when(valueDbDriverManagerDataSource).name();
		doReturn(importSql).when(valueDbDriverManagerDataSource).importSql();
		doReturn(true).when(valueDbDriverManagerDataSource).registerToContext();
		doReturn(prefix).when(valueDbDriverManagerDataSource).propertyPrefix();

		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_PW).when(valueDbDriverManagerDataSource)
				.pw();
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_URL).when(valueDbDriverManagerDataSource)
		.url();
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_USERNAME).when(valueDbDriverManagerDataSource)
		.username();
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_DRIVER_CLASS_NAME).when(valueDbDriverManagerDataSource)
		.driverClassName();

		doNothing().when(factoryBean).registerSingleton(eq(name), eq(dataSource));
		doNothing().when(service).importScript(eq(importSql), eq(dataSource));

		String driver = "org.hsqldb.jdbcDriver";
		doReturn(driver).when(service)
		.evaluateProperty(eq(ValueDbConstants.DATASOURCE_PROPERTY_DRIVER_CLASS_NAME), eq(prefix));
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_PW).when(service)
		.evaluateProperty(eq(ValueDbConstants.DATASOURCE_PROPERTY_PW), eq(prefix));
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_URL).when(service)
		.evaluateProperty(eq(ValueDbConstants.DATASOURCE_PROPERTY_URL), eq(prefix));
		doReturn(ValueDbConstants.DATASOURCE_PROPERTY_USERNAME).when(service)
		.evaluateProperty(eq(ValueDbConstants.DATASOURCE_PROPERTY_USERNAME), eq(prefix));
		
		DriverManagerDataSource ds = (DriverManagerDataSource) service
				.createInnerDataSource(valueDbDriverManagerDataSource);
		assertNotNull(ds);
		assertEquals(ValueDbConstants.DATASOURCE_PROPERTY_PW, ds.getPassword());
		assertEquals(ValueDbConstants.DATASOURCE_PROPERTY_URL, ds.getUrl());
		assertEquals(ValueDbConstants.DATASOURCE_PROPERTY_USERNAME, ds.getUsername());

		verify(service, times(1)).importScript(eq(importSql), eq(ds));
		verify(factoryBean, times(1)).registerSingleton(eq(name), eq(ds));
		
		service.storeDataSource.map.clear();
		doReturn(NOT_SET).when(valueDbDriverManagerDataSource).importSql();
		doReturn(false).when(valueDbDriverManagerDataSource).registerToContext();

		service.createInnerDataSource(valueDbDriverManagerDataSource);

		verify(service, times(1)).importScript(eq(importSql), eq(ds));
		verify(factoryBean, times(1)).registerSingleton(eq(name), eq(ds));
	}

	public static class TestBean implements DataSource {
		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}
		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}
		@Override
		public Connection getConnection() throws SQLException {
			return null;
		}
		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return null;
		}
		private String name;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return null;
		}
		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
		}
		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
		}
		@Override
		public int getLoginTimeout() throws SQLException {
			return 0;
		}
		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
		}
	}
	
	@Test 
	public void testCreateDataSourceFromClass() {
		doReturn(TestBean.class.getName()).when(dataSourceAnotation).className();
		doReturn(prefix).when(dataSourceAnotation).propertyPrefix();

		String value = "value";
		doReturn(value).when(service).evaluateProperty(eq("${name}"), eq(prefix));

		TestBean t = (TestBean)service.createDataSourceFromClass(dataSourceAnotation);
		assertEquals(value, t.getName());
	}

}
