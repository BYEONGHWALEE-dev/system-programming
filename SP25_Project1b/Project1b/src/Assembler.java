import jdk.jshell.execution.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Assembler : 
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. <br>
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. <br>
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. <br>
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) <br>
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) <br>
 * 
 * <br><br>
 * �ۼ����� ���ǻ��� : <br>
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.<br>
 *  2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.<br>
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.<br>
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)<br>
 * 
 * <br><br>
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
	/** instruction ���� ������ ���� */
	InstTable instTable;
	/** �о���� input ������ ������ �� �� �� �����ϴ� ����. */
	ArrayList<String> lineList;
	/** ���α׷��� section���� symbol table�� �����ϴ� ����*/
	ArrayList<SymbolTable> symtabList;
	/** ���α׷��� section���� token table�� �����ϴ� ����*/
	ArrayList<TokenTable> tokenList;
	/** ���α׷��� section���� ref table�� �����ϴ� ����*/
	ArrayList<RefTable> refList;
	/** ���α׷��� section���� modifired list �� �����ϴ� ����*/
	ArrayList<ModificationTable> modList;

	/** Directive List */
	ArrayList<String> directiveList;
	/** 
	 * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����. <br>
	 * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
	 */
	ArrayList<String> codeList;

	/**
	 * Literal�� ������ ����, SECTION ���� ������ ���� ��
	 */
	LiteralTable literalTable = new LiteralTable();
	
	/**
	 * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
	 * 
	 * @param instFile : instruction ���� �ۼ��� ���� �̸�. 
	 */
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

	/** 
	 * ��U���� ���� ��ƾ
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler("assets/inst_table.txt");
		assembler.loadInputFile("assets/input.txt");

		assembler.pass1();
		assembler.printSymbolTable("output_symtab.txt");
		assembler.printLiteralTable("output_littab.txt");

		assembler.pass2();
		assembler.printObjectCode("output_objectcode.txt");


	}


	/**
	 * inputFile�� �о�鿩�� lineList�� �����Ѵ�.<br>
	 * @param inputFile : input ���� �̸�.
	 */
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

	/**
	 * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
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

	/**
	 * �ۼ��� LiteralTable���� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
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

	/** 
	 * pass1 ������ �����Ѵ�.<br>
	 *   1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����<br>
	 *   2) label�� symbolTable�� ����<br>
	 *   <br><br>
	 *    ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
	 */
	private void pass1() {
		// TODO Auto-generated method stub
		SymbolTable symbolTable = new SymbolTable();
		TokenTable tokenTable = new TokenTable(symbolTable, instTable, directiveList);
		RefTable refTable = new RefTable();
		int sectionNum = 0;
		// lineList���� �� �پ� ��ĵ�Ͽ� ��ū ������ �и��� �� ��ū ���̺� ����
        for (int i= 0; i < lineList.size(); i++) {
			// �о�ö� . �̸� pass
			String line = lineList.get(i);
			if(line.startsWith(".")){continue;} // .�� �����ϸ� Pass

            tokenTable.putToken(line); //token���� ����
			Token token = tokenTable.getToken(tokenTable.tokenTableIndex++); // ��ū�� �����´�. 1: Literal Table�� ���� 2: Section �и��� ����

			if(token.operator.equals("CSECT") || token.operator.equals("END")) {
				tokenList.add(tokenTable);
				symtabList.add(symbolTable);
				refList.add(refTable);

				tokenTable.tableLength = TokenTable.locationCounter;
				token.setLocation(0); // ��ū�� location�� 0���� set�Ѵ�.

				symbolTable = new SymbolTable();
				tokenTable = new TokenTable(symbolTable, instTable, directiveList);
				refTable = new RefTable();

				if(token.operator.equals("CSECT")) {
					sectionNum++;
					TokenTable.locationCounter = 0; // locationCounter ���� �ʱ�ȭ ��������Ѵ�.
				}
			}
			else if(token.operator.equals("EXTDEF")){ //EXTDEF�� ������ DEF ���̺� �ִ´�. ���� ������ DEF�� ����� 0�� �Ǵ� �� -> null�� �ƴϴ�. ������ �� ���̺��� �� ������
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
					literalTable.putLocation(TokenTable.locationCounter); // �ּ� �Ҵ�
					literalTable.putSection(sectionNum);

					int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1)); // literal�� ������ ��ȯ -> ��ȯ�� �����ŭ TokenTable�� locationCounter ����
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
			else if(token.operand[0] != null){ // literal table�� literal�� �������
				if(token.operand[0].startsWith("=")){
					if(!literalTable.checkRedundancy(token.operand[0])){
						literalTable.putLiteral(token.operand[0]);
					}
				}
			}

			token.putInSymbolTable(symbolTable); // token�� ������ ���� ��, symbolTable�� �ִ´�.
        }

		// ��� scan�� ���� �ڿ� �ؾ��� ���� LTORG�� ������ ���� literal�� �ּҸ� �ο����־�� �Ѵ�.
		int check = literalTable.checkIndexForPass1;
		for(int i = check; i < literalTable.literalList.size(); i++){
			literalTable.putLocation(TokenTable.locationCounter); // �ּ� �Ҵ�

			String literal = literalTable.getLiteral(i); // literal�� �����ŭ �ּ� ���ϱ�
			int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1));
			TokenTable.locationCounter += sizeLiteral;
		}
	}
	
	/**
	 * pass2 ������ �����Ѵ�.<br>
	 *   1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
	 */
	private void pass2() {
		// TODO Auto-generated method stub
		// 2��° Scan
		ModificationTable modificationTable = new ModificationTable();
		for (int i = 0; i < tokenList.size(); i++) { // ��ü section�� ����
			TokenTable tokenTable = tokenList.get(i);
			tokenTable.literalTable = literalTable;
			ArrayList<Token> tokenList = tokenTable.tokenList;
			SymbolTable symbolTable = tokenTable.getSymbolTable();

			for (int j = 0; j < tokenList.size(); j++) { // section���� ��ɾ� ����
				Token token = tokenList.get(j);
				String operator = token.operator;
				String[] operand = token.operand;

				/*
				  set nixbpe
				 */
				if (instTable.getOpcode(operator) != -1) {
					// + : format 4, # : immediate, @ : indirect�� ��������� ��
					if (operator.startsWith("+")) {
						token.setFlag(TokenTable.nFlag, 1);
						token.setFlag(TokenTable.iFlag, 1);
						token.setFlag(TokenTable.bFlag, 0);
						token.setFlag(TokenTable.pFlag, 0);
						token.setFlag(TokenTable.eFlag, 1);

						// Operand���� X Ȯ�� �� flag ����
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

						// b �� p �� �ϳ��� �����ؾ� ��.
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

						// Operand���� X Ȯ�� �� flag ����
						if (token.checkXRegister(operand)) {
							token.setFlag(TokenTable.xFlag, 1);
						} else {
							token.setFlag(TokenTable.xFlag, 0);
						}

						// b �� p �� �ϳ��� �����ؾ� ��. // �ؾ� �� �� : literal ���������� �� �߿� �ϳ� ���� �ؾ� ��
						Token nextToken = tokenList.get(j + 1);
						int targetAddress;
						if (operand[0].startsWith("=")) { // literal�� ���
							targetAddress = literalTable.getLocationByLiteral(operand[0]);
						} else {
							targetAddress = symbolTable.searchSymbol(nextToken.operand[0]);
						}

						int disp = targetAddress - nextToken.location;
						if (disp >= -2048 && disp <= 2047) {
							token.setFlag(TokenTable.pFlag, 1); // PC-relative
							token.setFlag(TokenTable.bFlag, 0);
						} else {
							token.setFlag(TokenTable.bFlag, 1); // Base-relative (����)
							token.setFlag(TokenTable.pFlag, 0);
						}
					}
				}
				/*
				Object code ����
				 */
				if ((instTable.getOpcode(token.operator) != -1) || (directiveList.contains(token.operator))) {
					tokenTable.makeObjectCode(j);
				} else if (token.operator.equals("LTORG")) {
					literalTable.checkIndexForPass2++;
					tokenTable.makeObjectCode(j);
				}

				/*
				Modified code ����
				 */
				if ("CSECT".equals(token.operator)) {
					modList.add(modificationTable);             // ���ݱ��� ���� ���̺� ����
					modificationTable = new ModificationTable(); // ���ο� Section�� ���̺� �ʱ�ȭ
				}

				if (token.operator.startsWith("+") && operand.length > 0) {
					// Format 4 ��ɾ��� ���, EXTREF symbol�̸� ���� ���ڵ� �߰�
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
		// LTORG�� ������ ���� Literal�� �������� �־�����Ѵ�.
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

		// ������ Section�� modificationTable�� �߰�
		modList.add(modificationTable);

		/*
		System.out.println("\n===== Modification Records by Section =====");

		for (int sectionIndex = 0; sectionIndex < modList.size(); sectionIndex++) {
			ModificationTable modTable = modList.get(sectionIndex);
			System.out.println("Section " + sectionIndex + ":");

			for (ModificationRecord rec : modTable.modificationTable) {
				System.out.println("  " + rec);  // rec.toString() �ڵ� ȣ�� �� M ���ڵ� ����
			}

			if (modTable.modificationTable.isEmpty()) {
				System.out.println("  (No modification records)");
			}
		}
		 */
	}

	/**
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printObjectCode(String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
			for (int i = 0; i < tokenList.size(); i++) {
				TokenTable tokenTable = tokenList.get(i);
				SymbolTable symTab = tokenTable.getSymbolTable();
				ArrayList<Token> tokens = tokenTable.tokenList;
				ModificationTable modTable = modList.get(i);
				ArrayList<String> extRefSymbols = refList.get(i).getRefTable();
				ArrayList<String> extDefSymbols = refList.get(i).getDefTable();  // ����� ���� EXTDEF ���̺�

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

				// flush ������ T record
				if (tLen > 0) {
					writer.write(String.format("T%06X%02X%s\n", tStart, tLen, tRecord.toString()));
				}

				// M record
				for (ModificationRecord m : modTable.modificationTable) {
					writer.write(m.toString() + "\n");
				}

				// E record
				if (i == 0) {
					writer.write(String.format("E%06X\n\n", startAddr));  // ù section�� entry ����
				} else {
					writer.write("E\n\n");  // �������� �׳� E
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
