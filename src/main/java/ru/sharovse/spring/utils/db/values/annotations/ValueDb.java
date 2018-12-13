package ru.sharovse.spring.utils.db.values.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Прочитать значение из СУБД в поле.
 * 
 * Значения поля устанавливаются после поднятия контекста. Обязательно должны
 * быть заполнено одно из двух полей, определяющих DataSource:
 * {@link #dataSourceAnnotation()} или {@link #dataSourceBean()}.
 * <br/>
 * В общем случае работает 3 стратегии заполнения полей:
 * <ol>
 * <li>Одно значение в единичный объект или массив.
 * <p>
 * Свойства {@link #valueColumnNumber()} и {@link #valueColumnName()} указывает
 * какое поле из RecordSet-а записывать в поле. Если не заполнено имя колонки
 * {@link #valueColumnName()}, то используется порядковый номер колонки
 * указанный в {@link #valueColumnNumber()}. По умолчанию
 * {@link #valueColumnNumber()} = 1 - берётся первое поле.
 * </p>
 * <p>
 * Например,
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
 * </li>
 * <li>Первая запись в Map<String,Object>, где String- имя колонки recordSet в
 * uppercase, а Object - значение.
 * <p>
 * В Map пишутся все поля recordset первой строки.
 * </p>
 * <p>
 * Например,
 * 
 * <pre>
	&#64;ValueDb(valueSql="select * from AP_USERSETTING where CODE='SERVICE_ID'"
			, dataSourceBean=Config.DB_SMFF)
	Map<String, Object> colums;
 * </p>
 * </li>
   <li>Все записи в List<Map<String, Object>>, где String-имя колонки recordSet в uppercase, а Object - значение.  
 * <p>
 * В Map пишутся все поля recordset первой строки.
 * </p>
 * <p> Например,
 * <pre>
	&#64;ValueDb(valueSql="select code, value from AP_USERSETTING"
			, dataSourceBean=Config.DB_SMFF)
	List<Map<String, Object>> maps; *
 * </pre>
 * </p>
 * </li>
 * </ol>
 * 
 * @author sharov1-se
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDb {
	/**
	 * SQL скрипт получения значения для поля. При вызове запроса используется
	 * DataSource указанный в {@link #dataSourceBean()}, а также передаётся 2
	 * параметра:
	 * <ol>
	 * <li>Контекст spring в параметре под именем, указанным в
	 * {@link #argContextName()}</li>
	 * <li>Имя поля в параметре под именем, указанным в
	 * {@link #argPropertyName()}</li>
	 * </ol>
	 * Пример:
	 * 
	 * <pre>
	 * select VALUE from AP_USERSETTING where CODE=upper(:name)
	 * </pre>
	 * 
	 * Использование параметров не обязательно.
	 * 
	 * @return
	 */
	String valueSql();

	/**
	 * Имя bean DataSource. Если DataSource задан здесь, то значения полей
	 * заполняются после создания контекста и не могут использоваться 
	 * в других autowired переменных. Если требуется, чтобы значения заполнялись
	 * раньше, используйте {@link #dataSourceAnnotation()}.
	 * 
	 * @return DataSource Источник данных для значений полей.
	 */
	String dataSourceBean() default "";

	/**
	 * Встроенные данные для DataSource. DataSource создаётся до того как создан
	 * spring context.
	 * 
	 * @return DataSource Источник данных для значений полей.
	 */
	String dataSourceAnnotation() default "";

	/**
	 * Имя sql параметра - имя аннотированного поля.
	 * @return имя поля.
	 */
	String argPropertyName() default "name";

	/**
	 * Номер поля, которое используется для значения поля. Используется если не
	 * задано {@link #valueColumnName()}. Если указана позиция больше чем
	 * возвращается столбцов в recordSet значение не меняется.
	 * 
	 * @return число.
	 */
	int valueColumnNumber() default 1;

	/**
	 * Имя слобца в recordSet для выбора значения.
	 * 
	 * @return имя толбца.
	 */
	String valueColumnName() default "";

}
