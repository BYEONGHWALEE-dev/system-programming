import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;


/**
 * ��� instruction�� ������ �����ϴ� Ŭ����. instruction data���� �����Ѵ�. <br>
 * ���� instruction ���� ����, ���� ��� ����� �����ϴ� �Լ�, ���� ������ �����ϴ� �Լ� ���� ���� �Ѵ�.
 */
public class InstTable {

	/**
	 * inst.data ������ �ҷ��� �����ϴ� ����.
	 *  ��ɾ��� �̸��� ��������� �ش��ϴ� Instruction�� �������� ������ �� �ִ�.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * Ŭ���� �ʱ�ȭ. �Ľ��� ���ÿ� ó���Ѵ�.
	 * @param instFile : instuction�� ���� ���� ����� ���� �̸�
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * �Է¹��� �̸��� ������ ���� �ش� ������ �Ľ��Ͽ� instMap�� �����Ѵ�.
	 */
	public void openFile(String fileName) {
		//...
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				Instruction instruction= new Instruction(line);
				instMap.put(instruction.mnemonic, instruction);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	//get, set, search ���� �Լ��� ���� ����
	public HashMap<String, Instruction> getInstMap() {
		return instMap;
	}

	public void setInstMap(HashMap<String, Instruction> instMap) {
		this.instMap = instMap;
	}

	public int getFormatOfInstruction(String mnemonic){
		return instMap.get(mnemonic).getFormat();
	}

	public int getOpcode(String mnemonic){
		return instMap.get(mnemonic).getOpcode();
	}

	// map�� ������ ��ȯ �Լ�
	public int getInstMapLength(){
		return instMap.size();
	}

}
/**
 * ��ɾ� �ϳ��ϳ��� ��ü���� ������ InstructionŬ������ ����.
 * instruction�� ���õ� �������� �����ϰ� �������� ������ �����Ѵ�.
 */
class Instruction {

	String mnemonic;
	int format;
	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	int opcode;
	int countOperand;

	/**
	 * Ŭ������ �����ϸ鼭 �Ϲݹ��ڿ��� ��� ������ �°� �Ľ��Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * �Ϲ� ���ڿ��� �Ľ��Ͽ� instruction ������ �ľ��ϰ� �����Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public void parsing(String line) {
		// TODO Auto-generated method stub
		line = line.trim();
		String[] token = line.split("[,\\s+]");
		this.mnemonic = token[0];
		this.format = Integer.parseInt(token[1]);
		this.opcode = Integer.parseInt(token[2], 16);
		this.countOperand = Integer.parseInt(token[3]);

	}
	//�� �� �Լ� ���� ����
	public int getFormat() {return this.format;}
	public int getOpcode() {return this.opcode;}
}
