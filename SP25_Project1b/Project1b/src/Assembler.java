import jdk.jshell.execution.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Assembler : 
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. <br>
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. <br>
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. <br>
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) <br>
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) <br>
 * 
 * <br><br>
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.<br>
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 * 
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	/** instruction 명세를 저장한 공간 */
	InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<TokenTable> tokenList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<RefTable> refList;
	/** 
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간. <br>
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	ArrayList<String> codeList;

	/**
	 * Literal을 저장할 공간, SECTION 별로 나누지 않을 것
	 */
	LiteralTable literalTable = new LiteralTable();
	
	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 * 
	 * @param instFile : instruction 명세를 작성한 파일 이름. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		tokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
		refList = new ArrayList<RefTable>();
	}

	/** 
	 * 어셐블러의 메인 루틴
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler("assets/inst_table.txt");
		assembler.loadInputFile("assets/input.txt");

		assembler.pass1();
		assembler.printSymbolTable("output_symtab.txt");
		assembler.printLiteralTable("output_littab.txt");
		/*
		assembler.pass2();
		assembler.printObjectCode("output_objectcode.txt");

		 */
	}


	/**
	 * inputFile을 읽어들여서 lineList에 저장한다.<br>
	 * @param inputFile : input 파일 이름.
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
	 * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printSymbolTable(String fileName) {
		// TODO Auto-generated method stub
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))){
			for(int i = 0; i < symtabList.size(); i++) {
				for(int j = 0; j < symtabList.get(i).symbolList.size(); j++) {
					writer.write(String.format("%-10s 0x%04X\n", symtabList.get(i).symbolList.get(j), symtabList.get(i).locationList.get(j)));
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
	 * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
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
	 * pass1 과정을 수행한다.<br>
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성<br>
	 *   2) label을 symbolTable에 정리<br>
	 *   <br><br>
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private void pass1() {
		// TODO Auto-generated method stub
		SymbolTable symbolTable = new SymbolTable();
		TokenTable tokenTable = new TokenTable(symbolTable, instTable);
		RefTable refTable = new RefTable();

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

				token.setLocation(0); // 토큰의 location을 0으로 set한다.

				symbolTable = new SymbolTable();
				tokenTable = new TokenTable(symbolTable, instTable);
				refTable = new RefTable();

				if(token.operator.equals("CSECT")) {
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
			System.out.println(literal);
			int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1));
			TokenTable.locationCounter += sizeLiteral;
		}
	}
	
	/**
	 * pass2 과정을 수행한다.<br>
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub

	}
}
