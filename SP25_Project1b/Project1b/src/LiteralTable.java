import java.util.ArrayList;


public class LiteralTable {

    ArrayList<String> literalList;
    ArrayList<Integer> locationList;
    ArrayList<Integer> sectionLocation;
    int checkIndexForPass1;
    int checkIndexForPass2;

    // constructor
    public LiteralTable() {
        literalList = new ArrayList<>();
        locationList = new ArrayList<>();
        sectionLocation = new ArrayList<>();
        checkIndexForPass1 = 0;
        checkIndexForPass2 = 0;
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

    public void putSection(int section) {
        sectionLocation.add(section);
    }

    // literal 가져오는 함수
    public String getLiteral(int index) {
        return literalList.get(index);
    }

    public int getLocationByLiteral(String literal) {
        int index = literalList.indexOf(literal);
        if(index == -1) {
            return -1;
        }
        return locationList.get(literalList.indexOf(literal));
    }

    // literal 투입 전 중복 확인 메서드
    public boolean checkRedundancy(String literal) {
        return literalList.contains(literal);
    }

    // length
    public int getLength(int index) {
        return literalList.get(index).length() - 4;
    }

    // 존재하는 Literal print
    public void printLiteral(){
        for(int i = 0; i < literalList.size(); i++){
            System.out.println(literalList.get(i));
        }
    }
}
