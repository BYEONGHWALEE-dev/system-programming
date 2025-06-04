package SP25_simulator.assembler;

import jdk.jshell.execution.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;



public class Assembler {
	InstTable instTable;
	ArrayList<String> lineList;
	ArrayList<SymbolTable> symtabList;
	ArrayList<TokenTable> tokenList;
	ArrayList<RefTable> refList;
	ArrayList<ModificationTable> modList;

	ArrayList<String> directiveList;
	ArrayList<String> codeList;

	LiteralTable literalTable = new LiteralTable();

	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		tokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
		refList = new ArrayList<RefTable>();
		modList = new ArrayList<ModificationTable>();
		directiveList = new ArrayList<>(Arrays.asList("RESW", "RESB", "BYTE", "WORD"));
	}


	public static void assemble(String filePath) {
		Assembler assembler = new Assembler("assembler/assets/inst_table.txt");
		assembler.loadInputFile(filePath);

		assembler.pass1();
		assembler.printSymbolTable("assembler/output_symtab.txt");
		assembler.printLiteralTable("assembler/output_littab.txt");

		assembler.pass2();
		assembler.printObjectCode("output_objectcode.txt");
	}


	private void loadInputFile(String inputFile) {
		// TODO Auto-generated method stub
		try(BufferedReader br = new BufferedReader(new FileReader(inputFile))){
			String line;
			while ((line = br.readLine()) != null) {
				lineList.add(line);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printSymbolTable(String fileName) {
		// TODO Auto-generated method stub
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))){

			for(int i = 0; i < symtabList.size(); i++) {
				String sectionName = "";
				for(int j = 0; j < symtabList.get(i).symbolList.size(); j++) {
					if(symtabList.get(i).locationList.get(j) == 0){
						sectionName = symtabList.get(i).symbolList.get(j);
						break;
					}
				}

				for(int j = 0; j < symtabList.get(i).symbolList.size(); j++) {
					if(j == 0){
						writer.write(String.format("%-10s 0x%04X\n", symtabList.get(i).symbolList.get(j), symtabList.get(i).locationList.get(j)));
					} else{
						writer.write(String.format("%-10s 0x%04X	%-8s\n", symtabList.get(i).symbolList.get(j), symtabList.get(i).locationList.get(j), sectionName));
					}
				}
				for(int j = 0; j < refList.get(i).getRefTable().size(); j++) {
					writer.write(String.format("%-10s %-8s\n", refList.get(i).getRefTable().get(j), "REF"));
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void printLiteralTable(String fileName) {
		// TODO Auto-generated method stub
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))){
			for(int i = 0; i < literalTable.literalList.size(); i++) {
				String literal = literalTable.literalList.get(i);
				int location = literalTable.locationList.get(i);

				writer.write(String.format("%-12s 0x%04X\n", literal, location));
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}


	private void pass1() {
		// TODO Auto-generated method stub
		SymbolTable symbolTable = new SymbolTable();
		TokenTable tokenTable = new TokenTable(symbolTable, instTable, directiveList);
		RefTable refTable = new RefTable();
		int sectionNum = 0;
		// lineList에서 한 줄씩 스캔하여 토큰 단위로 분리한 뒤 토큰 테이블 생성
        for (int i= 0; i < lineList.size(); i++) {
			// 읽어올때 . 이면 pass
			String line = lineList.get(i);
			if(line.startsWith(".")){continue;} // .로 시작하면 Pass

            tokenTable.putToken(line); //token으로 넣음
			Token token = tokenTable.getToken(tokenTable.tokenTableIndex++); // 토큰을 가져온다. 1: Literal Table을 위함 2: Section 분리를 위함

			if(token.operator.equals("CSECT") || token.operator.equals("END")) {
				tokenList.add(tokenTable);
				symtabList.add(symbolTable);
				refList.add(refTable);

				tokenTable.tableLength = TokenTable.locationCounter;
				token.setLocation(0); // 토큰의 location을 0으로 set한다.

				symbolTable = new SymbolTable();
				tokenTable = new TokenTable(symbolTable, instTable, directiveList);
				refTable = new RefTable();

				if(token.operator.equals("CSECT")) {
					sectionNum++;
					TokenTable.locationCounter = 0; // locationCounter 또한 초기화 시켜줘야한다.
				}
			}
			else if(token.operator.equals("EXTDEF")){ //EXTDEF를 만나면 DEF 테이블에 넣는다. 만약 없으면 DEF의 사이즈가 0이 되는 것 -> null이 아니다. 생성할 때 테이블을 다 생성함
				int countOperand = Utility.countOperand(token.operand);
				refTable.putInDefTable(token.operand, countOperand);
			}
			else if(token.operator.equals("EXTREF")){
				int countOperand = Utility.countOperand(token.operand);
				refTable.putInRefTable(token.operand, countOperand);
			}
			else if(token.operator.equals("LTORG")) {
				for(int j = literalTable.checkIndexForPass1; j < literalTable.literalList.size(); j++){
					String literal = literalTable.getLiteral(j);
					literalTable.putLocation(TokenTable.locationCounter); // 주소 할당
					literalTable.putSection(sectionNum);

					int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1)); // literal의 사이즈 반환 -> 반환된 사이즈만큼 TokenTable의 locationCounter 증가
					TokenTable.locationCounter += sizeLiteral;
				}
			}
			else if(token.operator.equals("EQU")) {
				if(token.operand.length > 1){
					int firstOperand = symbolTable.searchSymbol(token.operand[0]);
					int secondOperand = symbolTable.searchSymbol(token.operand[1]);

					token.location = firstOperand - secondOperand;
				}
			}
			else if(token.operand[0] != null){ // literal table에 literal을 집어넣음
				if(token.operand[0].startsWith("=")){
					if(!literalTable.checkRedundancy(token.operand[0])){
						literalTable.putLiteral(token.operand[0]);
					}
				}
			}

			token.putInSymbolTable(symbolTable); // token의 수정이 끝난 뒤, symbolTable에 넣는다.
        }

		// 모든 scan이 끝난 뒤에 해야할 것은 LTORG를 만나지 못한 literal에 주소를 부여해주어야 한다.
		int check = literalTable.checkIndexForPass1;
		for(int i = check; i < literalTable.literalList.size(); i++){
			literalTable.putLocation(TokenTable.locationCounter); // 주소 할당

			String literal = literalTable.getLiteral(i); // literal의 사이즈만큼 주소 더하기
			int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1));
			TokenTable.locationCounter += sizeLiteral;
		}
	}
	

	private void pass2() {
		// TODO Auto-generated method stub
		// 2번째 Scan
		ModificationTable modificationTable = new ModificationTable();
		for (int i = 0; i < tokenList.size(); i++) { // 전체 section의 개수
			TokenTable tokenTable = tokenList.get(i);
			tokenTable.literalTable = literalTable;
			ArrayList<Token> tokenList = tokenTable.tokenList;
			SymbolTable symbolTable = tokenTable.getSymbolTable();

			for (int j = 0; j < tokenList.size(); j++) { // section내의 명령어 개수
				Token token = tokenList.get(j);
				String operator = token.operator;
				String[] operand = token.operand;

				/*
				  set nixbpe
				 */
				if (instTable.getOpcode(operator) != -1) {
					// + : format 4, # : immediate, @ : indirect로 구분지어야 함
					if (operator.startsWith("+")) {
						token.setFlag(TokenTable.nFlag, 1);
						token.setFlag(TokenTable.iFlag, 1);
						token.setFlag(TokenTable.bFlag, 0);
						token.setFlag(TokenTable.pFlag, 0);
						token.setFlag(TokenTable.eFlag, 1);

						// Operand에서 X 확인 후 flag 설정
						if (token.checkXRegister(operand)) {
							token.setFlag(TokenTable.xFlag, 1);
						} else {
							token.setFlag(TokenTable.xFlag, 0);
						}
					} else if (operand[0].startsWith("#")) {
						token.setFlag(TokenTable.nFlag, 0);
						token.setFlag(TokenTable.iFlag, 1);
						token.setFlag(TokenTable.xFlag, 0);
						token.setFlag(TokenTable.bFlag, 0);
						token.setFlag(TokenTable.pFlag, 0);
						token.setFlag(TokenTable.eFlag, 0);
					} else if (operand[0].startsWith("@")) {
						token.setFlag(TokenTable.nFlag, 1);
						token.setFlag(TokenTable.iFlag, 0);
						token.setFlag(TokenTable.xFlag, 0);

						// b 와 p 중 하나를 선택해야 함.
						Token nextToken = tokenList.get(j + 1);
						int targetAddress = symbolTable.searchSymbol(nextToken.operand[0].substring(1));
						if (nextToken.location - targetAddress > 0) {
							token.setFlag(TokenTable.pFlag, 1);
						} else {
							token.setFlag(TokenTable.bFlag, 0);
						}

						token.setFlag(TokenTable.eFlag, 0);
					} else if (token.operand[0].isEmpty()) {
						token.setFlag(TokenTable.nFlag, 1);
						token.setFlag(TokenTable.iFlag, 1);
						token.setFlag(TokenTable.xFlag, 0);
						token.setFlag(TokenTable.bFlag, 0);
						token.setFlag(TokenTable.pFlag, 0);
						token.setFlag(TokenTable.eFlag, 0);
					} else {
						token.setFlag(TokenTable.nFlag, 1);
						token.setFlag(TokenTable.iFlag, 1);

						// Operand에서 X 확인 후 flag 설정
						if (token.checkXRegister(operand)) {
							token.setFlag(TokenTable.xFlag, 1);
						} else {
							token.setFlag(TokenTable.xFlag, 0);
						}

						// b 와 p 중 하나를 선택해야 함. // 해야 할 것 : literal 나왔을때도 둘 중에 하나 고르게 해야 함
						Token nextToken = tokenList.get(j + 1);
						int targetAddress;
						if (operand[0].startsWith("=")) { // literal일 경우
							targetAddress = literalTable.getLocationByLiteral(operand[0]);
						} else {
							targetAddress = symbolTable.searchSymbol(nextToken.operand[0]);
						}

						int disp = targetAddress - nextToken.location;
						if (disp >= -2048 && disp <= 2047) {
							token.setFlag(TokenTable.pFlag, 1); // PC-relative
							token.setFlag(TokenTable.bFlag, 0);
						} else {
							token.setFlag(TokenTable.bFlag, 1); // Base-relative (가정)
							token.setFlag(TokenTable.pFlag, 0);
						}
					}
				}
				/*
				Object code 생성
				 */
				if ((instTable.getOpcode(token.operator) != -1) || (directiveList.contains(token.operator))) {
					tokenTable.makeObjectCode(j);
				} else if (token.operator.equals("LTORG")) {
					literalTable.checkIndexForPass2++;
					tokenTable.makeObjectCode(j);
				}

				/*
				Modified code 생성
				 */
				if ("CSECT".equals(token.operator)) {
					modList.add(modificationTable);             // 지금까지 만든 테이블 저장
					modificationTable = new ModificationTable(); // 새로운 Section용 테이블 초기화
				}

				if (token.operator.startsWith("+") && operand.length > 0) {
					// Format 4 명령어인 경우, EXTREF symbol이면 수정 레코드 추가
					if (refList.get(i).getRefTable().contains(operand[0])) {
						modificationTable.putModifcationRecord(token.location + 1, 5, '+', operand[0]);
					}
				}

				if (token.operator.equals("WORD") && token.operand.length == 2) {
					String sym1 = token.operand[0];
					String sym2 = token.operand[1];

					if (refList.get(i).getRefTable().contains(sym1)) {
						modificationTable.putModifcationRecord(token.location, 6, '+', sym1);
					}
					if (refList.get(i).getRefTable().contains(sym2)) {
						modificationTable.putModifcationRecord(token.location, 6, '-', sym2);
					}
				}
			}
		}
		// LTORG를 만나지 못한 Literal을 마지막에 넣어줘야한다.
		TokenTable tokenTable = tokenList.getLast();
		for(int i = literalTable.checkIndexForPass2; i < literalTable.literalList.size(); i++) {
			String literal = literalTable.getLiteral(i);
			if(literal.charAt(1) == 'X'){
				String objCode = literal.substring(3, literal.length() - 1).toUpperCase();
				Token token = new Token(objCode);
				tokenTable.putTokenRest(token);
				tokenTable.tableLength++;
			}
		}

		// 마지막 Section의 modificationTable도 추가
		modList.add(modificationTable);
	}


	private void printObjectCode(String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
			for (int i = 0; i < tokenList.size(); i++) {
				TokenTable tokenTable = tokenList.get(i);
				SymbolTable symTab = tokenTable.getSymbolTable();
				ArrayList<Token> tokens = tokenTable.tokenList;
				ModificationTable modTable = modList.get(i);
				ArrayList<String> extRefSymbols = refList.get(i).getRefTable();
				ArrayList<String> extDefSymbols = refList.get(i).getDefTable();  // 사용자 정의 EXTDEF 테이블

				String sectionName = tokens.getFirst().label;
				if(sectionName.trim().isEmpty()){
					TokenTable preTokenTable = tokenList.get(i - 1);
					int listSize = preTokenTable.tokenList.size();
					sectionName = preTokenTable.getToken(listSize - 1).label;
				}

				int startAddr = tokens.get(0).location;
				int length = tokenTable.tableLength;

				// H record
				writer.write(String.format("H%-6s%06X%06X\n", sectionName, startAddr, length));

				// D record
				if (!extDefSymbols.isEmpty()) {
					writer.write("D");
					for (String sym : extDefSymbols) {
						int addr = symTab.searchSymbol(sym);
						writer.write(String.format("%-6s%06X", sym, addr));
					}
					writer.write("\n");
				}

				// R record
				if (!extRefSymbols.isEmpty()) {
					writer.write("R");
					for (String sym : extRefSymbols) {
						writer.write(String.format("%-6s", sym));
					}
					writer.write("\n");
				}

				// T record
				int tStart = -1;
				StringBuilder tRecord = new StringBuilder();
				int tLen = 0;

				for (Token token : tokens) {
					String objCode = token.objectCode;
					if (objCode == null || objCode.isEmpty()) continue;
					int loc = token.location;

					if (tStart == -1) {
						tStart = loc;
						tRecord = new StringBuilder();
						tLen = 0;
					}

					if (tLen + objCode.length() / 2 > 30 || loc > tStart + tLen) {
						// flush current T
						writer.write(String.format("T%06X%02X%s\n", tStart, tLen, tRecord.toString()));
						// start new record
						tStart = loc;
						tRecord = new StringBuilder();
						tLen = 0;
					}

					tRecord.append(objCode);
					tLen += objCode.length() / 2;
				}

				// flush 마지막 T record
				if (tLen > 0) {
					writer.write(String.format("T%06X%02X%s\n", tStart, tLen, tRecord.toString()));
				}

				// M record
				for (ModificationRecord m : modTable.modificationTable) {
					writer.write(m.toString() + "\n");
				}

				// E record
				if (i == 0) {
					writer.write(String.format("E%06X\n\n", startAddr));  // 첫 section만 entry 지정
				} else {
					writer.write("E\n\n");  // 나머지는 그냥 E
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
