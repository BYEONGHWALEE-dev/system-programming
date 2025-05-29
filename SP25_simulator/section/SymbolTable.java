package SP25_simulator.section;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * symbol과 관련된 데이터와 연산을 소유한다. section 별로 하나씩 인스턴스를 할당한다.
 */
public class SymbolTable {
	private final Map<String, Integer> definitions;
	private final Map<String, Integer> references;
	// 기타 literal, external 선언 및 처리방법을 구현한다.

	public SymbolTable() {
		this.definitions = new HashMap<>();
		this.references = new HashMap<>();
	}
	/**
	 * 새로운 Definition을 table에 추가한다.
	*/
	public void addDefinition(String symbol, int address) {
		if(definitions.containsKey(symbol)) {
			throw new IllegalArgumentException(
					"Definition for symbol '" + symbol + "' already exists.");
		}
		definitions.put(symbol, address);
	}
	/**
	 * 기존에 존재하는 symbol 값에 대해서 가리키는 주소값을 변경한다.
	 * 
	 * @param symbol     : 변경을 원하는 symbol의 label
	 * @param newaddress : 새로 바꾸고자 하는 주소값
	 */
	public void modifySymbol(String symbol, int newaddress) {

	}

	/**
	 * 인자로 전달된 symbol이 어떤 주소를 지칭하는지 알려준다.
	 * 
	 * @param symbol : 검색을 원하는 symbol의 label
	 * @return symbol이 가지고 있는 주소값. 해당 symbol이 없을 경우 -1 리턴
	 */
	public int searchDefinitionAddress(String symbol) {
		return definitions.getOrDefault(symbol, -1);
	}

	public Map<String, Integer> getAllDefinitions() {
		return new HashMap<>(definitions);
	}

	public void addReference(String symbol){
		// 일단 처음엔 -1을 넣어놓는다.
		references.putIfAbsent(symbol, -1);
	}

	public void resolveReference(String symbol, int address) {
		if (!references.containsKey(symbol)) {
			throw new IllegalArgumentException(
					"Reference for symbol '" + symbol + "' not found.");
		}
		references.put(symbol, address);
	}

	public int getReferenceAddress(String symbol) {
		return references.getOrDefault(symbol, -1);
	}

	public Map<String, Integer> getAllReferences() {
		return new HashMap<>(references);
	}

	public boolean isDefined(String symbol) {
		return definitions.containsKey(symbol);
	}

	public boolean isReferenced(String symbol) {
		return references.containsKey(symbol);
	}
}
