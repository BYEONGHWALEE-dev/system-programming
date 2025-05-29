package SP25_simulator.section;

public class TextRecord {
    private final int startAddr;
    private final int length;
    private final String objectCodes;

    public TextRecord(int startAddr, int length, String objectCodes) {
        this.startAddr = startAddr;
        this.length = length;
        this.objectCodes = objectCodes;
    }

    public int getStartAddr() {return this.startAddr;}
    public int getLength() {return this.length;}
    public String getObjectCodes() {return this.objectCodes;}
}
