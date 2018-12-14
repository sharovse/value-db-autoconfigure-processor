package ru.sharovse.spring.utils.db.values;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class StoreValues<V> {
	Map<String, V> map = new HashMap<>();
	
	public StoreValues() {
		super();
	} 
	
	public V createAndGetValue(String key, StoreValue<V> callBack) {
		if(!map.containsKey(key)){
			map.put(key, callBack.createValue(key));
		} 
		return map.get(key);
	}
	
	public V get(String key){
		return map.get(key);
	}
	
	public boolean isContainKey(String key){
		return map.containsKey(key);
	}
	
	public void forEach(StoreItem<V> callBack){
		for (Entry<String, V> item: map.entrySet()) {
			callBack.item(item.getKey(), item.getValue());
		}
	}
}
