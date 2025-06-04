package SP25_simulator.assembler;

import java.util.ArrayList;

/**
 * symbol�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class SymbolTable {
	ArrayList<String> symbolList = new ArrayList<>();
	ArrayList<Integer> locationList = new ArrayList<>();
	
	/**
	 * ���ο� Symbol�� table�� �߰��Ѵ�.
	 * @param symbol : ���� �߰��Ǵ� symbol�� label
	 * @param location : �ش� symbol�� ������ �ּҰ�
	 * <br><br>
	 * ���� : ���� �ߺ��� symbol�� putSymbol�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifySymbol()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putSymbol(String symbol, int location) {
		if(symbolList != null) {
			symbolList.add(symbol);
			locationList.add(location);
		}
	}
	
	/**
	 * ������ �����ϴ� symbol ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param symbol : ������ ���ϴ� symbol�� label
	 * @param newLocation : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifySymbol(String symbol, int newLocation) {
		
	}
	
	/**
	 * ���ڷ� ���޵� symbol�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param symbol : �˻��� ���ϴ� symbol�� label
	 * @return symbol�� ������ �ִ� �ּҰ�. �ش� symbol�� ���� ��� -1 ����
	 */
	public int searchSymbol(String symbol) {
		if(symbol.startsWith("@")) {
			symbol = symbol.substring(1);
		}

		for(int i = 0; i < symbolList.size(); i++) {
			if(symbolList.get(i).equals(symbol)) {
				return locationList.get(i);
			}
		}
		return -1;
	}

	/**
	 * operand �迭���� syMbol�� �ִ��� ������ Ȯ��
	 */
	public boolean isitSymbol(String[] operand){
		for(int i = 0; i < operand.length; i++) {
			if(operand[i].equals(symbolList.get(i))) {
				return true;
			}
		}
		return false;
	}
	
}
