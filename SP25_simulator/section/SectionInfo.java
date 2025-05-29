package SP25_simulator.section;

import java.util.ArrayList;
import java.util.List;

public class SectionInfo {

    // 필요한 필드 1. Symbol Table. Definition, Reference
    // HName, T-Record(이거는 relocation 되지 않은 값이다), Modification Code -> 따로 저장
    // SymbolTable 객체도 여기 있어야 함. -> Extdef를 위해

    private String sectionName;
    private int startAddress;
    private int length;
    private SymbolTable symbolTable;

    private final List<TextRecord> textRecords;
    private final List<String> modRecords;

    public SectionInfo(String sectionName, int startAddress, int length) {
        this.sectionName = sectionName;
        this.startAddress = startAddress;
        this.length = length;
        this.symbolTable = new SymbolTable();
        this.textRecords = new ArrayList<>();
        this.modRecords = new ArrayList<>();
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void addTextRecord(TextRecord textRecord) {
        textRecords.add(textRecord);
    }

    public List<TextRecord> getTextRecords() {
        return textRecords;
    }

    public void addModRecord(String recordLine) {
        modRecords.add(recordLine);
    }

    public List<String> getModRecords() {
        return modRecords;
    }
}
