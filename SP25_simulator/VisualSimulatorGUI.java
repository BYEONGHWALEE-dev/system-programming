package SP25_simulator;

import SP25_simulator.section.SectionInfo;
import SP25_simulator.assembler.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.rmi.server.LogStream.log;

public class VisualSimulatorGUI extends JFrame{

    private final ResourceManager resourceManager = new ResourceManager();
    private final SicSimulator sicSimulator = new SicSimulator(resourceManager, "inst_table.txt", this);
    private final SicLoader sicLoader = new SicLoader(resourceManager, sicSimulator);
    private  Assembler assembler;

    // 상단
    private JTextField fileNameField;
    private JButton openBtn;
    private JComboBox<String> sectionCombo;
    private JLabel currentSectionLabel;

    // Header Record
    private JTextField progNameField, startAddrField, progLengthField;

    // End Record;
    private JTextField endAddrField, memoryStartField, targetAddrField;

    // 레지스터
    private JTextField[] regDecFields = new JTextField[10];
    private JTextField[] regHexFields = new JTextField[10];
    private static final String[] regNames = {"A", "X", "L", "B", "S", "T", "F", "", "PC", "SW"};
    private static final int[] regNums = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    // 명령어
    private JTextArea instructionArea;
    private JTextField deviceField;

    // 버튼
    private JButton stepBtn, allBtn, exitBtn, resetBtn;

    // 로그
    private JTextArea logArea;

    public VisualSimulatorGUI() {
        setTitle("SIC/XE Visual Simulator");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 - 파일 로드
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.add(new JLabel("File: "));
        fileNameField = new JTextField(20);
        openBtn = new JButton("Open");
        filePanel.add(fileNameField);
        filePanel.add(openBtn);
        filePanel.add(new JLabel("Section: "));
        sectionCombo = new JComboBox<>();
        filePanel.add(sectionCombo);
        filePanel.add(new JLabel("Current: "));
        currentSectionLabel = new JLabel("");
        filePanel.add(currentSectionLabel);
        add(filePanel, BorderLayout.NORTH);

        // 중단 - 주요 내용
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));

        // 좌측 - Haeder, Register
        JPanel leftPanel = new JPanel(new BorderLayout());

        // Header, Register Paenl
        JPanel headerRegPanel = new JPanel();
        headerRegPanel.setLayout(new BoxLayout(headerRegPanel, BoxLayout.Y_AXIS));

        // Header Record
        JPanel headerPanel = new JPanel(new GridLayout(3, 2));
        headerPanel.setBorder(BorderFactory.createTitledBorder("H (HeaderRecord)"));
        headerPanel.add(new JLabel("Program name : "));
        progNameField = new JTextField();
        startAddrField = new JTextField();;
        headerPanel.add(progNameField);
        headerPanel.add(startAddrField);

        headerPanel.add(new JLabel("Length of Program : "));
        progLengthField = new JTextField();
        headerPanel.add(progLengthField);
        headerRegPanel.add(headerPanel);

        // Register
        JPanel regPanel = new JPanel(new GridLayout(11 ,3 ));
        regPanel.setBorder(BorderFactory.createTitledBorder("Register"));
        regPanel.add(new JLabel("")); regPanel.add(new JLabel("Dec")); regPanel.add(new JLabel("HEX"));
        for(int i = 0; i < regNames.length; i++) {
            if(regNums[i] == -1) continue;
            regPanel.add(new JLabel(regNames[i] + " (#" + regNums[i] + ")"));
            regDecFields[i] = new JTextField();
            regHexFields[i] = new JTextField();
            regPanel.add(regDecFields[regNums[i]]);
            regPanel.add(regHexFields[regNums[i]]);
        }
        headerRegPanel.add(regPanel);
        leftPanel.add(headerRegPanel, BorderLayout.CENTER);
        centerPanel.add(leftPanel);

        // 우측 - End Record + Instructions
        JPanel rightPanel = new JPanel(new BorderLayout());

        JPanel endInstPanel = new JPanel(new GridLayout(3, 1));

        JPanel endPanel = new JPanel(new GridLayout(3,2));
        endPanel.setBorder(BorderFactory.createTitledBorder("E (End Record{"));
        endPanel.add(new JLabel("Address of First instruction of Object Program : "));
        endAddrField = new JTextField();
        endPanel.add(endAddrField);
        endPanel.add(new JLabel("Start Address in Memory : "));
        memoryStartField = new JTextField();
        endPanel.add(memoryStartField);
        endPanel.add(new JLabel("Target Address : "));
        targetAddrField = new JTextField();
        endPanel.add(targetAddrField);
        endInstPanel.add(endPanel);


        // Instructions
        JPanel instPanel = new JPanel(new BorderLayout());
        instPanel.setBorder(BorderFactory.createTitledBorder("Instructions : "));
        instructionArea = new JTextArea(8, 20);
        instPanel.add(new JScrollPane(instructionArea), BorderLayout.CENTER);
        endInstPanel.add(instPanel);

        // Device 사용
        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        deviceField = new JTextField("사용중인 장치", 10);
        devicePanel.add(deviceField);
        endInstPanel.add(devicePanel);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout());
        stepBtn = new JButton("실행(1step");
        allBtn = new JButton("실행(all)");
        exitBtn = new JButton("종료");
        resetBtn = new JButton("초기화");
        btnPanel.add(stepBtn);
        btnPanel.add(allBtn);
        btnPanel.add(exitBtn);
        btnPanel.add(resetBtn);
        endInstPanel.add(btnPanel);

        rightPanel.add(endInstPanel, BorderLayout.CENTER);
        centerPanel.add(rightPanel);

        add(centerPanel, BorderLayout.CENTER);

        // 하단 - Log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log(명령어 수행 관련 :)"));
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(logPanel, BorderLayout.SOUTH);

        // ============================ Listener ===============================

        // 파일 열기 버튼
        openBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                fileNameField.setText(file.getAbsolutePath());

                Assembler.assemble(file.getAbsolutePath());

                // 로드 순서
                sicLoader.load("output_objectcode.txt");
                updateSimulatorView(sicSimulator);
                log("파일 로드 완료: " + file.getName());

                stepBtn.setEnabled(true);
                allBtn.setEnabled(true);
            }

        });


        stepBtn.addActionListener(e -> {
            sicSimulator.oneStep(this);
        });

        allBtn.addActionListener(e -> {
            sicSimulator.allStep(this);
        });

        resetBtn.addActionListener(e -> {
            reset();
        });
    }

    // 로그 출력
    private void log(String msg) {
        logArea.append(msg + "\n");
    }

    // 레지스터 set
    public void setRegisterField(int regNum, int value){
        if(regNum < 0 || regNum >= regDecFields.length) return;

        regDecFields[regNum].setText(String.valueOf(value));
        regHexFields[regNum].setText(Integer.toHexString(value).toUpperCase());
    }

    // Header info
    public void setHeaderInfo(String name, String startAddr, String length) {
        progNameField.setText(name);
        startAddrField.setText(startAddr);
        progLengthField.setText(length);
    }

    // End info
    public void setEndInfo(String execAddr, String memsStart, String targetAddr) {
        endAddrField.setText(execAddr);
        memoryStartField.setText(memsStart);
        targetAddrField.setText(targetAddr);
    }

    // add log on Log area
    public void appendLog(String message) {
        logArea.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VisualSimulatorGUI gui = new VisualSimulatorGUI();
            gui.setVisible(true);
        });
    }

    public void updateSimulatorView(SicSimulator simulator) {
        sectionCombo.removeAllItems();
        for (SectionInfo sec : simulator.getSections()) {
            sectionCombo.addItem(sec.getSectionName());
        }
        sectionCombo.setSelectedItem(simulator.getCurrentSection());
    }

    public void update(ResourceManager resourceManager) {
        // 헤더 정보 최신화
        progNameField.setText(resourceManager.getProgramName());
        startAddrField.setText(resourceManager.getStartAddress());
        progLengthField.setText(resourceManager.getProgramLength());

        // End Record 정보가 있다면 갱신
        if(endAddrField != null) endAddrField.setText(resourceManager.getExecutionStartAddress());
        if(memoryStartField != null) memoryStartField.setText(resourceManager.getStartAddress());
        if(targetAddrField != null) targetAddrField.setText(resourceManager.getTargetAddress());

        // 레지스터 정보 갱신
        for(int i = 0; i < regNums.length; i++) {
            if (i == 6) {  // F 레지스터 (float)
                double fVal = resourceManager.register_F;
                if(regDecFields[i] != null) regDecFields[i].setText(String.format("%.2f", fVal));
                if(regHexFields[i] != null) regHexFields[i].setText(String.format("%06X", (int) fVal));
            } else {
                int value = resourceManager.getRegister(regNums[i]);
                if(regDecFields[i] != null) regDecFields[i].setText(String.valueOf(value));
                if(regHexFields[i] != null) regHexFields[i].setText(String.format("%06X", value));
            }
        }
    }

    public void showCurrentInstruction(String address, String objectCode, String mnemonic){
        String line = String.format("0x%04X :  %s (%s)", Integer.parseInt(address, 16), objectCode, mnemonic);
        instructionArea.append(line + "\n");
    }

    public void showCurrentSection(String sectionName) {
        currentSectionLabel.setText(sectionName);
    }

    private void reset() {
        resourceManager.initializeResource();
        update(resourceManager);
        logArea.setText("");
        instructionArea.setText("");
        log("시스템 초기화 완료");
    }
}
