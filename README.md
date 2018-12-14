# Value-db-autoconfigure-processor
Append to Spring Autowired @ValueDb Annotation. Your variable autowired value from BD over JDBC.

Add depepency your project:
	
	<dependency>
		<groupId>ru.sharovse.spring-utils</groupId>
		<artifactId>value-db-autoconfigure-processor</artifactId>
		<version>0.0.3</version>
	</dependency>

## Example 1. Recomended usage annotation @ValueDb.

Java class:

	@ValueDbDataSourceBean(name=DATASOURCE_NAME, propertyPrefix="dev.datasource")
	@ValueDb(dataSourceAnnotation=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;

	@ValueDb(dataSourceAnnotation=DATASOURCE_NAME,
			valueSql="select CODE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbCode;

application.properties:

	dev.datasource.driverClassName=org.hsqldb.jdbcDriver
	dev.datasource.url=jdbc:hsqldb:mem:testdb
	dev.datasource.username=SA
	dev.datasource.password=

Created DriverManagerDataSource.
Execute @ValueDb.valueSql.
Store value to local variable.
**Value dbValue is available @Value Spring-EL.**

## Example 2. Use exist @Bean DataSource.

@Configuration
Java Class:

  	@ConfigurationProperties(prefix="dev.datasource")
  	@Bean(DATASOURCE_NAME)
  	DataSource createDataSource(){
  		return new BasicDataSource();
  	}

Java Class:

	@ValueDb(dataSourceBean=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;


**Value dbValue is not available @Value Spring-EL.**
Receive application event REFRESH.
Execute @ValueDb.valueSql with DataSource fuond as symbolic link @ValueDb.dataSourceBean.
Write value to local variable.

---

## Annotation @ValueDb acceptable for field type:

	* Single Object.
	* Array<Object>.
	* List<Object>.
	* Map<Column,Object>.
	* List<Map<Column,Object>>.

See example in test file: value-db-autoconfigure-processor/src/test/java/ru/sharovse/spring/utils/db/values/test/ValueDbTest.java 

Example,

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING"
			, valueColumnName = "VALUE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	List<String> serviceValueAsColumnValue;

