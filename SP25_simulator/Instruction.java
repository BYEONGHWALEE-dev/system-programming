package SP25_simulator;

/**
 * 명령어 하나하나의 구체적인 정보는 Instruction 클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
public class Instruction {
    private final String mnemonic;
    private final int format;         // format: 1, 2, 3
    private final int opcode;         // 16진수 정수
    private final int operandCount;   // 0, 1, 2

    /**
     * 한 줄 (예: LDA 3 00 2)을 파싱해서 Instruction 객체로 만든다.
     */
    public Instruction(String line) {
        String[] token = line.trim().split("[,\\s+]");
        this.mnemonic = token[0];
        this.format = Integer.parseInt(token[1]);
        this.opcode = Integer.parseInt(token[2], 16);  // 16진수
        this.operandCount = Integer.parseInt(token[3]);
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public int getFormat() {
        return format;
    }

    public int getOpcode() {
        return opcode;
    }

    public int getOperandCount() {
        return operandCount;
    }
}
