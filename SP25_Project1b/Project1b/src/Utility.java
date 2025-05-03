import java.util.ArrayList;
import java.util.HashMap;

public class Utility {

    // 같은 것이 있으면 true 반환 없으면 false 반환
    public static final boolean searchInTable(String value, ArrayList<String> table) {
        for(int i = 0; i < table.size(); i++){
            if(table.get(i).equals(value)){
                return true;
            }
        }
        return false;
    }

    // InstTable에서 찾기, 있으면 format 반환 없으면 0 반환
    public static final int returnFormat(String value, InstTable instTable){
        HashMap<String, Instruction> instMap = instTable.getInstMap();
        for(Instruction inst : instMap.values()){
            if(inst.mnemonic.equals(value)){
                return inst.getFormat();
            }
        }
        return 0;
    }

    // 변수 메모리 크기 계산하기
    public static final int calVariable(String value, String[] operand){
        if(value.equals("RESB")){return (Integer.parseInt(operand[0]));}
        if(value.equals("RESW")){return (Integer.parseInt(operand[0])) * 3;}
        return 0;
    }

    // 상수 메모리 크기 계산하기
    public static final int calConstant(String value){
        if(value.equals("WORD")) return 3;
        if(value.equals("BYTE")) return 1;
        return 0;
    }

    // Operand의 개수 구하기
    public static int countOperand(String[] operand){
        int count = 0;
        for(String s : operand){
            if(s != null) count++;
        }
        return count;
    }
}
