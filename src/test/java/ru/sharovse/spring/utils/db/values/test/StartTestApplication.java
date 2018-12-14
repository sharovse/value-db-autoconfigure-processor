package ru.sharovse.spring.utils.db.values.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

import ru.sharovse.spring.utils.db.values.annotations.ValueDb;
import ru.sharovse.spring.utils.db.values.annotations.ValueDbDataSourceBean;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class StartTestApplication implements CommandLineRunner {

	@Value("${dev.datasource.driverClassName}")
	String propertyValue;

	@ValueDbDataSourceBean(
			name=ValueDbTest.DATASOURCE_NAME, 
			propertyPrefix="dev.datasource", 
			registerToContext=true, 
			importSql="import.sql" )
	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE=upper(:name)", dataSourceAnnotation=ValueDbTest.DATASOURCE_NAME)
	String id;

	@ValueDb(valueSql="select CODE from AP_USERSETTING where CODE='ID'", dataSourceAnnotation=ValueDbTest.DATASOURCE_NAME)
	String code;

	@ValueDb(valueSql="select VALUE from AP_USERSETTING where CODE=upper(:name)", dataSourceBean=ValueDbTest.DATASOURCE_NAME)
	String beanDataSourceId;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("@Value="+propertyValue);
		System.out.println("@ValueDb id="+id);
		System.out.println("@ValueDb code="+code);
		System.out.println("@ValueDb beanDataSourceId="+beanDataSourceId);
	}

	public static void main(String[] args) {
		SpringApplication.run(StartTestApplication.class, args);
	}

}
