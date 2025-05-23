package SP25_simulator;


public class ExecutableInstruction {
    public final String objectCode;
    public final int address;
    public final String mnmonic;

    public ExecutableInstruction(String objectCode, int address, String mnmonic) {
        this.objectCode = objectCode;
        this.address = address;
        this.mnmonic = mnmonic;
    }


}
