# Value-db-autoconfigure-processor
Append to Spring Autowired @ValueDb Annotation

Example 1,

	@ValueDbDataSourceBean(name=DATASOURCE_NAME, propertyPrefix="dev.datasource")
	@ValueDb(dataSourceAnnotation=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;
	

Value dbValue is available @Value Spring-EL.

Example 2,
	
  	@ConfigurationProperties(prefix="dev.datasource")
  	@Bean(name=DATASOURCE_NAME)
  	DataSource createDataSource(){
  		return new BasicDataSource();
  	}
	@ValueDb(dataSourceAnnotation=DATASOURCE_NAME,
			valueSql="select VALUE from AP_USERSETTING where CODE='SERVICE_ID'")
	private String dbValue;
 
Value dbValue is not available @Value Spring-EL.

Annotation @ValueDb acceptable for field type:

	1) Object.

	2) Array<Object>.

	2) List<Object>.

	3) Map<Column,Object>.

	4) List<Column,Object>.

See example in test file ru.sharovse.spring.utils.db.values.test.ValueDbTest.

Example,

	@ValueDb(valueSql="select CODE, VALUE from AP_USERSETTING"
			, valueColumnName = "VALUE"
			, dataSourceAnnotation=DATASOURCE_NAME)
	List<String> serviceValueAsColumnValue;

