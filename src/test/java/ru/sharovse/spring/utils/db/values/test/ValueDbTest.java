package ru.sharovse.spring.utils.db.values.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit4.SpringRunner;

import ru.sharovse.spring.utils.db.values.annotations.ValueDb;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSource;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDriverManagerDataSource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ValueDbTest {

	public static final String DATASOURCE_NAME = "DS";
	public static final String DATASOURCE_NAME1 = "DS2";
	
	@Value("${dev.datasource.driverClassName}")
	String propertyValue;

	@Value("${value.CODE.ID}")
	String propIdValue;
	@Value("${value.CODE.SERVICE_ID}")
	String propServiceIdValue;

	String propIdCode = "ID";
	String propServiceIdCode = "SERVICE_ID";

	
	@Test
	public void testPropertiesRead() {
		assertEquals("org.hsqldb.jdbcDriver", propertyValue);
	}

	@ValueDbDriverManagerDataSource(
			name=DATASOURCE_NAME, 
			propertyPrefix="dev.datasource", 
			registerToContext=true, 
			importSql="import.sql" )
	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'", dataSourceAnnotation=DATASOURCE_NAME)
	String dbServiceIdValue;
	
	@Test
	public void testDbServiceIdValue() {
		assertEquals(propServiceIdValue, dbServiceIdValue);
	}

	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE=upper(:name)", dataSourceAnnotation=DATASOURCE_NAME)
	String id;
	
	@Test
	public void testDefautPropertyName() {
		assertEquals(propIdValue, id);
	}

	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE=upper(:xxx)", argPropertyName="xxx", dataSourceAnnotation=DATASOURCE_NAME)
	String service_id;
	
	@Test
	public void testPropertyNameAndAnotherArgPropertyName() {
		assertEquals(propServiceIdValue, service_id);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'", 
			dataSourceAnnotation=DATASOURCE_NAME)
	String serviceCode;
	@Test
	public void testDefaultFirstColumn() {
		assertEquals(propServiceIdCode, serviceCode);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, valueColumnNumber = 2
			, dataSourceAnnotation=DATASOURCE_NAME)
	String serviceValue;
	@Test
	public void testSecondColumn() {
		assertEquals(propServiceIdValue, serviceValue);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, valueColumnNumber = 3
			, dataSourceAnnotation=DATASOURCE_NAME)
	String serviceValue1;
	@Test
	public void testNonExistColumn() {
		assertNull(serviceValue1);
	}
	
	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, valueColumnName = "VALUE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	String serviceValueAsColumnValue;
	@Test
	public void testValueAsColumnValue() {
		assertEquals(propServiceIdValue, serviceValueAsColumnValue);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, valueColumnName = "CODE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	String serviceValueAsColumnCode;
	@Test
	public void testValueAsColumnCode() {
		assertEquals(propServiceIdCode, serviceValueAsColumnCode);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, valueColumnName = "XXXX"
			, dataSourceAnnotation=DATASOURCE_NAME)
	String serviceValueAsColumnNonExist;
	@Test
	public void testValueAsColumnNonExist() {
		assertNull(serviceValueAsColumnNonExist);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING order by CODE"
			, valueColumnNumber = 2
			, dataSourceAnnotation=DATASOURCE_NAME)
	String[] serviceValueArray;
	@Test
	public void testValueArray() {
		assertTrue(serviceValueArray.length==2);
		assertEquals(propIdValue, serviceValueArray[0]);
		assertEquals(propServiceIdValue, serviceValueArray[1]);
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING order by CODE"
			, valueColumnNumber = 2
			, dataSourceAnnotation=DATASOURCE_NAME)
	List<String> serviceValueList;
	@Test
	public void testValueList() {
		assertTrue(serviceValueList.size()==2);
		assertEquals(propIdValue, serviceValueList.get(0));
		assertEquals(propServiceIdValue, serviceValueList.get(1));
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING where CODE='SERVICE_ID'"
			, dataSourceAnnotation=DATASOURCE_NAME)
	Map<String,String> serviceMap;
	@Test
	public void testServiceMap() {
		assertTrue(serviceMap.size()==2);
		assertEquals(propServiceIdCode, serviceMap.get("CODE"));
		assertEquals(propServiceIdValue, serviceMap.get("VALUE"));
	}

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING order by CODE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	List<Map<String,String>> serviceListMap;
	@Test
	public void testServiceListMap() {
		assertTrue(serviceListMap.size()==2);
		assertEquals(propIdCode, serviceListMap.get(0).get("CODE"));
		assertEquals(propServiceIdCode, serviceListMap.get(1).get("CODE"));

	}

	@ValueDbDataSource(className="org.springframework.jdbc.datasource.DriverManagerDataSource",
			name=DATASOURCE_NAME1, propertyPrefix="dev.datasource")
	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'", dataSourceAnnotation=DATASOURCE_NAME1)
	String dbServiceIdValue2;

	@Test
	public void testDbServiceIdValue2() {
		assertEquals(propServiceIdValue, dbServiceIdValue2);
	}
	
}
