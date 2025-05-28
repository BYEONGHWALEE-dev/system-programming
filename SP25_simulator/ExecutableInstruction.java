package SP25_simulator;


public class ExecutableInstruction {
    private final int address;
    private final String objectCode;

    public ExecutableInstruction(int address, String objectCode) {
        this.address = address;
        this.objectCode = objectCode;
    }

    public int getAddress() {
        return address;
    }

    public String getObjectCode() {
        return objectCode;
    }
}
