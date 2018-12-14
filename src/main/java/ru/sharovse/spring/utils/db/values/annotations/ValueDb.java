package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static ru.sharovse.spring.utils.db.values.ValueDbConstants.*;

/**
 * Read value to field from DB.
 * 
 * Mandatory properties:
 * <ol>
 * <li> JDBC DataSource for fetching values. 
 * 	<ul>
 * 		<li>{@link #dataSourceBean()} or</li> 
 * 		<li>{@link #dataSourceAnnotation()}</li>
 * 	</ul>
 * </li>
 * <li> {@link #valueSql()} - Script SQL.</li>
 * </ol>
 * 
 * Fetching value strategy:
 * <ol>
 * <li>Single value into single object, array or list</li>
 * <li>Map columns and values - one row</li>
 * <li>Map columns and values - list rows</li>
 * </ol>
 * 
 * <h2>Single value into single object, array or list<//h2>
 * <p>Property {@link #valueColumnNumber()} or {@link #valueColumnName()} answer on question, what column of RecordSet will fetch. 
 * By default used {@link #valueColumnNumber()}=1. First column.
 * {@link #valueColumnName()} set name column RecordSet and have priority over {@link #valueColumnNumber()}.  
 * </p>
 * <p>
 * Examble,
 * <pre>
 * &#64;ValueDb(valueSql = "select VALUE from AP_USERSETTING where CODE='ID'", dataSourceBean = Config.DB_SMFF)
 * private String id;
 * 
 * &#64;ValueDb(valueSql = "select VALUE from AP_USERSETTING where CODE=upper(:name)", dataSourceBean = Config.DB_SMFF)
 * String service_id;
 * 
 * &#64;ValueDb(valueSql = "select * from AP_USERSETTING where CODE='SERVICE_ID'", dataSourceBean = Config.DB_SMFF)
 * String serviceId;
 * 
 * &#64;ValueDb(valueSql = "select value from AP_USERSETTING", dataSourceBean = Config.DB_SMFF)
 * Object[] values;
 * 
 * &#64;ValueDb(valueSql = "select code, value from AP_USERSETTING", dataSourceBean = Config.DB_SMFF, valueColumnName = "VALUE")
 * List<Object> list;
 * </pre>
 * </p>
 * 
 * <h2>Map columns and values - one row</h2>
 * <p>First (single) row of RecordSet will write as Map<Column,Object>, where Column is String uppercase name column of RecordSet.</p> 
 * <p>Example.
 * 
 * <pre>
	&#64;ValueDb(valueSql="select * from AP_USERSETTING where CODE='SERVICE_ID'"
			, dataSourceBean=Config.DB_SMFF)
	private Map<String, Object> colums;
 * </p>
 * 
 * <h2>Map columns and values - list rows</h2>
 * 
   <p>List rows of RecorSet fetch into List<Map<Columns, Object>>.</p>
 * <p>Exsample,
 * <pre>
	&#64;ValueDb(valueSql="select code, value from AP_USERSETTING"
			, dataSourceBean=Config.DB_SMFF)
	List<Map<String, Object>> maps; *
 * </pre>
 * </p>
 * 
 * @author sharov1-se
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDb {
	
	/**
	 * Script SQL for fetching values.
	 * <p>
	 * Used DataSource {@link #dataSourceBean()} or {@link #dataSourceAnnotation()}.
	 * This is {@link NamedParameterJdbcTemplate} with one argument - name field. 
	 * Default name argument watch in {@link #argContextName()} (name).  
	 * </p>
	 * <p>
	 * Пример:
	 * <pre>
	 * select VALUE from AP_USERSETTING where CODE=upper(:<b>name</b>)
	 * </pre>
	 * </p>
	 * @return
	 */
	String valueSql();

	/**
	 * Set symbolic link onto bean DataSource. 
	 * Fetched value is not available in @Value Spring-EL.
	 * Otherwise use {@link #dataSourceAnnotation()}. 
	 * 
	 * @return DataSource link to bean name.
	 */
	String dataSourceBean() default NOT_SET;

	/**
	 * Set symbolic link onto name embedded Datasource created annotation {@link ValueDbDataSourceBean}.
	 * @return DataSource link to name annotation {@link ValueDbDataSourceBean}).
	 */
	String dataSourceAnnotation() default NOT_SET;

	/**
	 * Set name argument in {@link #valueSql()}.
	 * Default "name". Example ":name". 
	 * @return name argument with name annotited property.
	 */
	String argPropertyName() default "name";

	/**
	 * Number column that will fetch from RecortSet.
	 * Default 1 - first column.
	 * @return number.
	 */
	int valueColumnNumber() default 1;

	/**
	 * Name column RecorSet.
	 * This property overwrite {@link #valueColumnNumber()}.
	 * @return characters.
	 */
	String valueColumnName() default NOT_SET;

}
