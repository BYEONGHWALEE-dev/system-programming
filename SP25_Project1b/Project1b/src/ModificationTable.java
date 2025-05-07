import java.util.ArrayList;

public class ModificationTable{

    ArrayList<ModificationRecord> modificationTable;

    public ModificationTable(){
        modificationTable = new ArrayList<>();
    }

    // put method
    public void putModifcationRecord(int address, int length, char sign, String symbol){
        modificationTable.add(new ModificationRecord(address, length, sign, symbol));
    }
}


class ModificationRecord {
    public int address;
    public int length;
    public char sign;
    public String symbol;

    public ModificationRecord(int address, int length, char sign, String symbol) {
        this.address = address;
        this.length = length;
        this.sign = sign;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return String.format("M%06X%02X%c%s", address, length, sign, symbol);
    }
}
