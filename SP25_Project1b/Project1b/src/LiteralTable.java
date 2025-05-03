import java.util.ArrayList;


public class LiteralTable {

    ArrayList<String> literalList;
    ArrayList<Integer> locationList;
    int checkIndexForPass1;

    // constructor
    public LiteralTable() {
        literalList = new ArrayList<>();
        locationList = new ArrayList<>();
        checkIndexForPass1 = 0;
    }

    public void putLiteral(String literal) {
        if(literalList != null) {
            literalList.add(literal);
        }
    }

    // 필요 메서드 추가 구현

    // location 넣는 메서드
    public void putLocation(int location) {
        locationList.add(location);
        checkIndexForPass1 += 1;
    }

    // literal 가져오는 함수
    public String getLiteral(int index) {
        return literalList.get(index);
    }

    // literal 투입 전 중복 확인 메서드
    public boolean checkRedundancy(String literal) {
        return literalList.contains(literal);
    }

    // 존재하는 Literal print
    public void printLiteral(){
        for(int i = 0; i < literalList.size(); i++){
            System.out.println(literalList.get(i));
        }
    }
}
