import java.util.ArrayList;
import java.util.Arrays;

public class RefTable {

    ArrayList<String> defTable;
    ArrayList<String> refTable;

    public RefTable() {
        defTable = new ArrayList<>();
        refTable = new ArrayList<>();
    }

    public void putInDefTable(String[] operand, int countOperand){
        defTable.addAll(Arrays.asList(operand).subList(0, countOperand));
    }

    public void putInRefTable(String[] operand, int countOperand){
        refTable.addAll(Arrays.asList(operand).subList(0, countOperand));
    }

    // getter
    public ArrayList<String> getDefTable() {
        return defTable;
    }

    public ArrayList<String> getRefTable() {
        return refTable;
    }

}
