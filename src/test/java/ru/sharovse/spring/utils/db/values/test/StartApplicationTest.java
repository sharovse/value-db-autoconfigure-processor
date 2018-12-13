package ru.sharovse.spring.utils.db.values.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class StartApplicationTest implements CommandLineRunner {

	@Value("${dev.datasource.driverClassName}")
	String propertyValue;

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Start "+propertyValue);
	}

	public static void main(String[] args) {
		SpringApplication.run(StartApplicationTest.class, args);
	}

}
