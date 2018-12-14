package ru.sharovse.spring.utils.db.values;

public interface StoreItem <V> {
	void item(String key, V value);
}
