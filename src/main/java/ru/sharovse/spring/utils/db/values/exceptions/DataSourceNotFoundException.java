package ru.sharovse.spring.utils.db.values.exceptions;

public class DataSourceNotFoundException extends Exception {
	private static final long serialVersionUID = 5417557723907793643L;

	public DataSourceNotFoundException() {
		super();
	}

	public DataSourceNotFoundException(String message) {
		super(message);
	}

}
