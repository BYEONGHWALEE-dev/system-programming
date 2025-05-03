import jdk.jshell.execution.Util;

import java.io.*;
import java.util.ArrayList;
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
	/** ���α׷��� section���� ���α׷��� �����ϴ� ����*/
	ArrayList<TokenTable> tokenList;
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
	}

	/** 
	 * ��U���� ���� ��ƾ
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler("assets/inst_table.txt");
		assembler.loadInputFile("assets/input.txt");

		assembler.pass1();
		assembler.printSymbolTable("output_symtab.txt");
		/*
		assembler.printLiteralTable("output_littab.txt");
		
		assembler.pass2();
		assembler.printObjectCode("output_objectcode.txt");

		 */
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
				for(int j = 0; j < symtabList.get(i).symbolList.size(); j++) {
					System.out.println(symtabList.get(i).symbolList.get(j));
					System.out.printf("%04X\n", symtabList.get(i).locationList.get(j));
					writer.write(symtabList.get(i).symbolList.get(j));
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
		TokenTable tokenTable = new TokenTable(symbolTable, instTable);
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

				token.setLocation(0); // ��ū�� location�� 0���� set�Ѵ�.

				symbolTable = new SymbolTable();
				tokenTable = new TokenTable(symbolTable, instTable);

				if(token.operator.equals("CSECT")) {
					TokenTable.locationCounter = 0; // locationCounter ���� �ʱ�ȭ ��������Ѵ�.
				}
			}
			else if(token.operator.equals("LTORG")) {
				for(int j = literalTable.checkIndexForPass1; j < literalTable.literalList.size(); j++){
					String literal = literalTable.getLiteral(j);
					literalTable.putLocation(TokenTable.locationCounter); // �ּ� �Ҵ�

					int sizeLiteral = Utility.countLengthLiteral(literal, literal.charAt(1)); // literal�� ������ ��ȯ -> ��ȯ�� �����ŭ TokenTable�� locationCounter ����
					TokenTable.locationCounter += sizeLiteral;
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
		for(int i = literalTable.checkIndexForPass1; i < literalTable.literalList.size(); i++){
			literalTable.putLocation(TokenTable.locationCounter); // �ּ� �Ҵ�

			String literal = literalTable.getLiteral(i); // literal�� �����ŭ �ּ� ���ϱ�
			System.out.println(literal);
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
		
	}

	/**
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.<br>
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub

	}
}
