package SP25_simulator.assembler;

import SP25_simulator.assembler.InstTable;
import SP25_simulator.assembler.SymbolTable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	public static int locationCounter = 0;
	public int tokenTableIndex;
	
	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	InstTable instTab;
	ArrayList<String> directiveTable;
	LiteralTable literalTable;
	
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList = new ArrayList<>();
	/** 테이블의 길이 */
	public int tableLength;
	
	/**
	 * 초기화하면서 symTable과 instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param instTab : instruction 명세가 정의된 instTable
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
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line, instTab, symTab));
	}

	public void putTokenRest(Token token) {
		tokenList.add(token);

	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table, token table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
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

	// literal을 Ascii로 변환

	
	/** 
	 * index번호에 해당하는 object code를 리턴한다.
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
				if(symTab.searchSymbol(operand[i]) != -1) { // 만약 symTab에 없으면 EXTREF Table에 있다는 뜻 --> 주소를 알 수 없다.
					count += 1;
				}
			}
			if(count == 0) {targetAddress = 0b00000;}
		}
		else if(((nixbpe & TokenTable.nFlag) == 0) && ((nixbpe & TokenTable.iFlag) == TokenTable.iFlag)){ // n = 1, i = 0 -> Immediate
			String theOperand = operand[0].substring(1); // # 제거
			targetAddress = Integer.parseInt(theOperand);
		}
		else if((nixbpe & TokenTable.pFlag) == 2){
			int pcCounter = nextToken.location;
			int tempAddress;
			if(literalTable.literalList.contains(operand[0])){ // literal일 경우에 말하는 것
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
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char arithmetic;
	int nixbpe = 0; // 플래그 초기화

	// object code 생성 단계에서 사용되는 변수들 
	String objectCode;
	int byteSize;
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line, InstTable instTab, SymbolTable symTab) {
		//initialize 추가
		parsing(line, instTab);
		addLocationCounter(operator, operand, instTab);
	}

	public Token(String objectCode){
		this.objectCode = objectCode;
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * @param line 문장단위로 저장된 프로그램 코드.
	 */
	public void parsing(String line, InstTable instTab) {
		String[] parts = line.split("\t");
		this.location = TokenTable.locationCounter;
		this.label = parts[0];
		this.operator = parts[1];

		// operand가 없을 수 있다.
		try{
			this.operand = divOperand(parts[2]);
		}catch (ArrayIndexOutOfBoundsException e){this.operand = new String[0];} // operand가 없다면 길이가 1인 ""만 있다.

		// comment가 없을 수도 있다.
		try{
			this.comment = parts[3];
		}catch (ArrayIndexOutOfBoundsException e){this.comment = "";}
	}
	
	/** 
	 * n,i,x,b,p,e flag를 설정한다. <br><br>
	 * 
	 * 사용 예 : setFlag(nFlag, 1); <br>
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
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
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 <br><br>
	 * 
	 * 사용 예 : getFlag(nFlag) <br>
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}

	/** 개인적으로 필요해서 만든 함수 */
	// Operand 분리 method -> Operand가 있으면 tempArray의 길이를 반환, null인 경우 0을 반환
	public String[] divOperand(String operand){
		if(operand != null){
			String[] tempArray = operand.split("[,+-]");
			// 나누면서 사칙 연산자 있으면 setArithmetic
			setArithmetic(operand);
			return tempArray;
		}
		return new String[0];
	}

	// 사칙연산자가 있다면 넣음
	public void setArithmetic(String operand){
		for(char c : operand.toCharArray()){
			if(c == '+' || c == '-'){
				this.arithmetic = c;
			}
		}
	}

	// location counter 계산하는 method with Instruction
	public void addLocationCounter(String value, String[] operand, InstTable instTable) {
		int format = Utility.returnFormat(value, instTable);
		if(format > 0){ // Instruction table에 있을 때
			TokenTable.locationCounter += format;
			this.byteSize = format;
		}
		else if(value.startsWith("+")){
			TokenTable.locationCounter += 4;
		}
		else {
			TokenTable.locationCounter += Utility.calConstant(value); // 상수 locationCounter 계산
			TokenTable.locationCounter += Utility.calVariable(value, operand); // 변수 locationCounter 계산
			this.byteSize = (Utility.calVariable(value, operand) == 0)
					? Utility.calConstant(value)
					: Utility.calVariable(value, operand);
			}
	}

	/**
	 * nixbpe를 설정할때 x의 유무
	 */
	public boolean checkXRegister(String[] operand) {
        return operand.length > 1 && "X".equals(operand[1]);
    }

	/**
	 * 토큰 생성할때 label 확인 후 -> symbol Table에 바로 넣어버림
	 */
	public void putInSymbolTable(SymbolTable symTab) {
		if(!this.label.isEmpty()){
			symTab.putSymbol(this.label, this.location);
		}
	}

	/**
	 * Token location 수정
	 */
	public void setLocation(int newLocation){
		this.location = newLocation;
	}


}


