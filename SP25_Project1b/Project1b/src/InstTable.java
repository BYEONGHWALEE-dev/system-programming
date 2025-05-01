import java.io.*;
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
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
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

	protected String mnemonic;
	private int format;
	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	private int opcode;

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
		String[] token = line.split("\t");
		this.mnemonic = token[0];
		this.format = Integer.parseInt(token[1]);
		this.opcode = Integer.parseInt(token[2].trim());
	}
	//�� �� �Լ� ���� ����
	public int getFormat() {return this.format;}
	public int getOpcode() {return this.opcode;}
}
