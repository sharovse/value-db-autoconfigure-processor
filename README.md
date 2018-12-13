# Value-db-autoconfigure-processor
Append to Spring Autowired @ValueDb Annotation

h2 Example 1,

Java class:

	@ValueDbDataSourceBean(name=DATASOURCE_NAME, propertyPrefix="dev.datasource")
	@ValueDb(dataSourceAnnotation=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;
	
application.properties:
	dev.datasource.driverClassName=org.hsqldb.jdbcDriver
	dev.datasource.url=jdbc:hsqldb:mem:testdb
	dev.datasource.username=SA
	dev.datasource.password=

**Value dbValue is available @Value Spring-EL.**

h2 Example 2,

@Configuration
Java Class:

  	@ConfigurationProperties(prefix="dev.datasource")
  	@Bean(name=DATASOURCE_NAME)
  	DataSource createDataSource(){
  		return new BasicDataSource();
  	}

Java Class:

	@ValueDb(dataSourceBean=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;
 
**Value dbValue is not available @Value Spring-EL.**


h2 Annotation @ValueDb acceptable for field type:

	* Object.
	* Array<Object>.
	* List<Object>.
	* Map<Column,Object>.
	* List<Column,Object>.

See example in test file ru.sharovse.spring.utils.db.values.test.ValueDbTest.

Example,

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING"
			, valueColumnName = "VALUE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	List<String> serviceValueAsColumnValue;

