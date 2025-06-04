package SP25_simulator.assembler;

import SP25_simulator.assembler.InstTable;
import SP25_simulator.assembler.SymbolTable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	public static int locationCounter = 0;
	public int tokenTableIndex;
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	InstTable instTab;
	ArrayList<String> directiveTable;
	LiteralTable literalTable;
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList = new ArrayList<>();
	/** ���̺��� ���� */
	public int tableLength;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab, ArrayList<String> directiveTable) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
		this.directiveTable = directiveTable;
		this.tokenTableIndex = 0;
		tableLength = 0;
	}

	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line, instTab, symTab));
	}

	public void putTokenRest(Token token) {
		tokenList.add(token);

	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table, token table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index) {
		//...
		Token token = tokenList.get(index);
		String operator = token.operator;
		int nixbpe = token.nixbpe;
		int targetAddress = makeTargetAddress(index);
		int format = instTab.getFormatOfInstruction(operator);
		int opcode = instTab.getOpcode(operator);

		String objCode = "";

		if(format == 2){
			objCode = String.format("%02X%02X", opcode, targetAddress);
		}
		else if(format == 3 || format == 4){
			int op_high6 = opcode & 0xFC;
			int op_with_flags = op_high6 | ((nixbpe >> 4) & 0x03);

			if((nixbpe & TokenTable.eFlag) != 0){
				// format 4
				objCode = String.format("%02X%01X%05X", op_with_flags, (nixbpe & 0x0F), targetAddress & 0xFFFFF);
			}
			else{
				objCode = String.format("%02X%01X%03X", op_with_flags, (nixbpe & 0x0F), targetAddress & 0xFFF);
			}
		}
		else if(directiveTable.contains(operator)){
			if(operator.equals("BYTE")){
				objCode = token.operand[0].substring(2, token.operand[0].length() - 1).toUpperCase();
			}
			else if(operator.equals("WORD")){
				if(!symTab.isitSymbol(token.operand)){
					objCode = "000000";
				}
			}

		}
		else if(token.operator.equals("LTORG")){
			for(int i = 0; i < literalTable.checkIndexForPass2; i++){
				objCode = Utility.convertLiteralToObjectCode(literalTable.getLiteral(i));
			}
		}
		token.objectCode = objCode;
	}

	// literal�� Ascii�� ��ȯ

	
	/** 
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}

	// getter
	public InstTable getInsttable(){
		return instTab;
	}

	public SymbolTable getSymbolTable(){
		return symTab;
	}

	/**
	 * Calculate Target address
	 */
	public int makeTargetAddress(int index) {
		Token token = tokenList.get(index);
		Token nextToken = tokenList.get(index+1);
		int nixbpe = token.nixbpe;
		String[] operand = token.operand;
		int targetAddress = 0b000;

		// nixbpe -> format 3/4
		if((nixbpe & TokenTable.eFlag) != 0){
			int count = 0;
			for(int i = 0; i < operand.length; i++){
				if(symTab.searchSymbol(operand[i]) != -1) { // ���� symTab�� ������ EXTREF Table�� �ִٴ� �� --> �ּҸ� �� �� ����.
					count += 1;
				}
			}
			if(count == 0) {targetAddress = 0b00000;}
		}
		else if(((nixbpe & TokenTable.nFlag) == 0) && ((nixbpe & TokenTable.iFlag) == TokenTable.iFlag)){ // n = 1, i = 0 -> Immediate
			String theOperand = operand[0].substring(1); // # ����
			targetAddress = Integer.parseInt(theOperand);
		}
		else if((nixbpe & TokenTable.pFlag) == 2){
			int pcCounter = nextToken.location;
			int tempAddress;
			if(literalTable.literalList.contains(operand[0])){ // literal�� ��쿡 ���ϴ� ��
				tempAddress = literalTable.getLocationByLiteral(operand[0]);
				targetAddress = tempAddress - pcCounter;
			}
			else{
				tempAddress = symTab.searchSymbol(operand[0]);
				targetAddress = tempAddress - pcCounter;
			}
		}

		// format2
		if(instTab.getFormatOfInstruction(token.operator) == 2){
			int r1 = 0;
			int r2 = 0;

			for(int i = 0; i < operand.length; i++){
				switch(token.operand[i]){
					case "A":
						if (i == 0) r1 = 0x0;
						else r2 = 0x0;
						break;
					case "X":
						if (i == 0) r1 = 0x1;
						else r2 = 0x1;
						break;
					case "S":
						if (i == 0) r1 = 0x4;
						else r2 = 0x4;
						break;
					case "T":
						if(i == 0) r1 = 0x5;
						else r2 = 0x5;
						break;
					default:
						break;
				}
			}
		targetAddress = (r1 << 4) | r2;
		}

		return targetAddress;
	}
	
}

/**
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char arithmetic;
	int nixbpe = 0; // �÷��� �ʱ�ȭ

	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	String objectCode;
	int byteSize;
	
	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line, InstTable instTab, SymbolTable symTab) {
		//initialize �߰�
		parsing(line, instTab);
		addLocationCounter(operator, operand, instTab);
	}

	public Token(String objectCode){
		this.objectCode = objectCode;
	}
	
	/**
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line, InstTable instTab) {
		String[] parts = line.split("\t");
		this.location = TokenTable.locationCounter;
		this.label = parts[0];
		this.operator = parts[1];

		// operand�� ���� �� �ִ�.
		try{
			this.operand = divOperand(parts[2]);
		}catch (ArrayIndexOutOfBoundsException e){this.operand = new String[0];} // operand�� ���ٸ� ���̰� 1�� ""�� �ִ�.

		// comment�� ���� ���� �ִ�.
		try{
			this.comment = parts[3];
		}catch (ArrayIndexOutOfBoundsException e){this.comment = "";}
	}
	
	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. <br><br>
	 * 
	 * ��� �� : setFlag(nFlag, 1); <br>
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
	 */
	public void setFlag(int flag, int value) {
		//...
		if(value == 1){
			nixbpe |= flag;
		} else{
			nixbpe &= ~flag;
		}
	}
	
	/**
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� <br><br>
	 * 
	 * ��� �� : getFlag(nFlag) <br>
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}

	/** ���������� �ʿ��ؼ� ���� �Լ� */
	// Operand �и� method -> Operand�� ������ tempArray�� ���̸� ��ȯ, null�� ��� 0�� ��ȯ
	public String[] divOperand(String operand){
		if(operand != null){
			String[] tempArray = operand.split("[,+-]");
			// �����鼭 ��Ģ ������ ������ setArithmetic
			setArithmetic(operand);
			return tempArray;
		}
		return new String[0];
	}

	// ��Ģ�����ڰ� �ִٸ� ����
	public void setArithmetic(String operand){
		for(char c : operand.toCharArray()){
			if(c == '+' || c == '-'){
				this.arithmetic = c;
			}
		}
	}

	// location counter ����ϴ� method with Instruction
	public void addLocationCounter(String value, String[] operand, InstTable instTable) {
		int format = Utility.returnFormat(value, instTable);
		if(format > 0){ // Instruction table�� ���� ��
			TokenTable.locationCounter += format;
			this.byteSize = format;
		}
		else if(value.startsWith("+")){
			TokenTable.locationCounter += 4;
		}
		else {
			TokenTable.locationCounter += Utility.calConstant(value); // ��� locationCounter ���
			TokenTable.locationCounter += Utility.calVariable(value, operand); // ���� locationCounter ���
			this.byteSize = (Utility.calVariable(value, operand) == 0)
					? Utility.calConstant(value)
					: Utility.calVariable(value, operand);
			}
	}

	/**
	 * nixbpe�� �����Ҷ� x�� ����
	 */
	public boolean checkXRegister(String[] operand) {
        return operand.length > 1 && "X".equals(operand[1]);
    }

	/**
	 * ��ū �����Ҷ� label Ȯ�� �� -> symbol Table�� �ٷ� �־����
	 */
	public void putInSymbolTable(SymbolTable symTab) {
		if(!this.label.isEmpty()){
			symTab.putSymbol(this.label, this.location);
		}
	}

	/**
	 * Token location ����
	 */
	public void setLocation(int newLocation){
		this.location = newLocation;
	}


}


