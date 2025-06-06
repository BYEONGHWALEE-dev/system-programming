import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;


/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다. <br>
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {

	/**
	 * inst.data 파일을 불러와 저장하는 공간.
	 *  명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * 클래스 초기화. 파싱을 동시에 처리한다.
	 * @param instFile : instuction에 대한 명세가 저장된 파일 이름
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
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

	
	//get, set, search 등의 함수는 자유 구현
	public HashMap<String, Instruction> getInstMap() {
		return instMap;
	}

	public void setInstMap(HashMap<String, Instruction> instMap) {
		this.instMap = instMap;
	}

	public int getFormatOfInstruction(String mnemonic){
		if(mnemonic.startsWith("+")){ // + 로 시작하면 +를 제외한다.
			mnemonic = mnemonic.substring(1);
		}
		if(!instMap.containsKey(mnemonic)){
			return -1;
		}
		return instMap.get(mnemonic).getFormat();
	}

	public int getOpcode(String mnemonic){
		if(mnemonic.startsWith("+")){
			mnemonic = mnemonic.substring(1); //로 시작하면 +를 제외한다.
		}
		if(!instMap.containsKey(mnemonic)){
			return -1;
		}
		return instMap.get(mnemonic).getOpcode();
	}

	// map의 사이즈 반환 함수
	public int getInstMapLength(){
		return instMap.size();
	}

}
/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {

	String mnemonic;
	int format;
	/** instruction이 몇 바이트 명령어인지 저장. 이후 편의성을 위함 */
	int opcode;
	int countOperand;

	/**
	 * 클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * 일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
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
	//그 외 함수 자유 구현
	public int getFormat() {return this.format;}
	public int getOpcode() {return this.opcode;}
}
