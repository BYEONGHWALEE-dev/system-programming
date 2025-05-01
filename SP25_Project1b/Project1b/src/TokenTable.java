import java.io.BufferedReader;
import java.io.StringReader;
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
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	InstTable instTab;
	
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
	}
	
	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line, instTab, symTab));
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
	 * instruction table, symbol table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){
		//...
	}
	
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
	char nixbpe;

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
		putInSymbolTable(symTab);
		addLocationCounter(operator, operand, instTab);
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
		this.operand = divOperand(parts[2]);
		this.comment = parts[3];
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
		else {
			TokenTable.locationCounter += Utility.calConstant(value); // ��� locationCounter ���
			TokenTable.locationCounter += Utility.calVariable(value, operand); // ���� locationCounter ���
			this.byteSize = (Utility.calVariable(value, operand) == 0)
					? Utility.calConstant(value)
					: Utility.calVariable(value, operand);
			}
	}

	/**
	 * ��ū �����Ҷ� label Ȯ�� �� -> symbol Table�� �ٷ� �־����
	 */
	public void putInSymbolTable(SymbolTable symTab) {
		if(!this.label.isEmpty()){
			symTab.putSymbol(this.label, this.location);
		}
	}

}


