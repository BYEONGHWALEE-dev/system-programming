package SP25_simulator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다.
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {

    /**
     * inst.data 파일을 불러와 저장하는 공간.
     * 명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
     */
    private HashMap<String, Instruction> instMap;
    private HashMap<Integer, Instruction> opcodeMap; // ✅ 추가: opcode → Instruction 맵

    /**
     * 클래스 초기화. 파싱을 동시에 처리한다.
     * @param instFile : instruction에 대한 명세가 저장된 파일 이름
     */
    public InstTable(String instFile) {
        instMap = new HashMap<>();
        opcodeMap = new HashMap<>();
        openFile(instFile);
    }

    /**
     * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap 및 opcodeMap에 저장한다.
     */
    public void openFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                Instruction instruction = new Instruction(line);
                instMap.put(instruction.getMnemonic(), instruction);
                opcodeMap.put(instruction.getOpcode(), instruction); // ✅ opcode → Instruction 등록
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Instruction getByMnemonic(String mnemonic) {
        if (mnemonic.startsWith("+")) {
            mnemonic = mnemonic.substring(1); // format 4 대응
        }
        return instMap.get(mnemonic);
    }

    public Instruction getByOpcode(int opcode) {
        return opcodeMap.get(opcode);
    }

    public int getFormatOfInstruction(String mnemonic) {
        Instruction inst = getByMnemonic(mnemonic);
        return (inst != null) ? inst.getFormat() : -1;
    }

    public int getOpcode(String mnemonic) {
        Instruction inst = getByMnemonic(mnemonic);
        return (inst != null) ? inst.getOpcode() : -1;
    }

    public int getOperandCount(String mnemonic) {
        Instruction inst = getByMnemonic(mnemonic);
        return (inst != null) ? inst.getOperandCount() : -1;
    }

    public HashMap<String, Instruction> getInstMap() {
        return instMap;
    }

    public void setInstMap(HashMap<String, Instruction> instMap) {
        this.instMap = instMap;
    }

    public int getInstMapLength() {
        return instMap.size();
    }
}
