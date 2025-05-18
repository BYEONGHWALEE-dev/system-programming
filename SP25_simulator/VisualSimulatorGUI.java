package SP25_simulator;

import javax.swing.*;
import java.awt.*;

public class VisualSimulatorGUI extends JFrame{

    // 상단
    private JTextField fileNameField;
    private JButton openBtn;

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
    private JButton stepBtn, allBtn,  exitBtn;

    // 로그
    private JTextArea logArea;

    public VisualSimulatorGUI() {
        setTitle("SIC/XE Visual Simulator");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 - 파일 로드
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.add(new JLabel("FileName : "));
        fileNameField = new JTextField(10);
        openBtn = new JButton("open");
        filePanel.add(fileNameField);
        filePanel.add(openBtn);
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

        JPanel endPanel = new JPanel(new GridLayout(3,1));
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
        JPanel btnPanel = new JPanel(new BorderLayout());
        stepBtn = new JButton("실행(1step");
        allBtn = new JButton("실행(all)");
        exitBtn = new JButton("종료");
        btnPanel.add(stepBtn);
        btnPanel.add(allBtn);
        btnPanel.add(exitBtn);
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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VisualSimulatorGUI gui = new VisualSimulatorGUI();
            gui.setVisible(true);
        });
    }


}
