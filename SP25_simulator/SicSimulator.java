package SP25_simulator;

import SP25_simulator.section.SectionInfo;
import SP25_simulator.section.TextRecord;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class SicSimulator {
	private ResourceManager rMgr;
	private InstLuncher instLuncher;
	private InstTable instTable;
	private VisualSimulatorGUI guiRef;

	// 새로 추가: 섹션 리스트와 현재 섹션 이름 프로퍼티
	private List<SectionInfo> sections = new ArrayList<>();
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private String currentSectionName = "";

	// 실행할 명령어 큐
	private List<ExecutableInstruction> instructionQueue = new ArrayList<>();
	private int currentInstructionIndex = 0;

	public SicSimulator(ResourceManager resourceManager, String instFile) {
		this.rMgr = resourceManager;
		this.instLuncher = new InstLuncher(resourceManager);
		this.instTable = new InstTable(instFile);
	}

	/** 모델에 섹션 리스트를 설정하고, 기본 현재 섹션을 첫 섹션으로 초기화 */
	public void setSections(List<SectionInfo> sections) {
		this.sections.clear();
		this.sections.addAll(sections);
		if (!sections.isEmpty()) {
			setCurrentSection(sections.getFirst().getSectionName());
		}
	}

	public List<SectionInfo> getSections() {
		return sections;
	}

	/** 현재 섹션 변경 시 이벤트 발행 */
	private void setCurrentSection(String newSection) {
		String old = this.currentSectionName;
		this.currentSectionName = newSection;
		pcs.firePropertyChange("currentSection", old, newSection);
		// GUI에도 즉시 반영
		if (guiRef != null) {
			guiRef.showCurrentSection(newSection);
		}
	}

	public String getCurrentSection() {
		return this.currentSectionName;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	/** 메모리·레지스터 초기화 및 명령어 큐 초기화 */
	public void load(File program) {
		rMgr.initializeResource();
		currentInstructionIndex = 0;
		instructionQueue.clear();
	}

	/** 한 단계 실행 및 GUI 업데이트 */
	public void oneStep(VisualSimulatorGUI gui) {
		if (currentInstructionIndex >= instructionQueue.size()) return;

		ExecutableInstruction inst = instructionQueue.get(currentInstructionIndex);
		int addr = inst.getAddress();

		// 실행 전 현재 섹션 업데이트
		for (SectionInfo sec : sections) {
			int start = sec.getStartAddress();
			int end = start + sec.getLength();
			if (addr >= start && addr < end) {
				if (!sec.getSectionName().equals(currentSectionName)) {
					setCurrentSection(sec.getSectionName());
				}
				break;
			}
		}

		char[] bytes = hexStringToCharArray(inst.getObjectCode());
		if (bytes.length < 2) {
			String warn = String.format("⚠ 명령어 길이 부족: %s at 0x%04X", inst.getObjectCode(), addr);
			gui.appendLog(warn);
			addLog(warn);
			currentInstructionIndex++;
			return;
		}

		int rawOpcode = bytes[0] & 0xFC;
		Instruction info = instTable.getByOpcode(rawOpcode);
		String logMsg;

		if (info != null) {
			boolean format4 = (bytes[1] & 0x01) == 1;
			if (format4 && bytes.length < 4) {
				String warn = String.format(
						"⚠ Format 4 명령어인데 object code 길이가 부족합니다: %s at 0x%04X",
						inst.getObjectCode(), addr);
				gui.appendLog(warn);
				addLog(warn);
				currentInstructionIndex++;
				return;
			}

			instLuncher.execute(info, bytes);
			String mnemonic = info.getMnemonic();
			logMsg = String.format("실행 : %s (%s) at 0x%04X", mnemonic, inst.getObjectCode(), addr);

			gui.appendLog(logMsg);
			gui.showCurrentInstruction(
					String.format("%04X", addr),
					inst.getObjectCode(),
					mnemonic
			);

			if (mnemonic.equals("RSUB") && rMgr.getRegister(8) == 0) {
				gui.appendLog("RSUB 실행 후 종료 감지 : PC = 0 -> 프로그램 종료");
			}
		} else {
			logMsg = String.format("⚠ 알 수 없는 명령어: %s at 0x%04X", inst.getObjectCode(), addr);
			gui.appendLog(logMsg);
			gui.showCurrentInstruction(
					String.format("%04X", addr),
					inst.getObjectCode(),
					"UNKNOWN"
			);
		}

		addLog(logMsg);
		gui.update(rMgr);
		currentInstructionIndex++;
	}

	/** 남은 모든 명령어 연속 실행 */
	public void allStep(VisualSimulatorGUI gui) {
		while (currentInstructionIndex < instructionQueue.size()) {
			oneStep(gui);
		}
	}

	/** 내부 로그 추가 */
	public void addLog(String log) {
		if (guiRef != null) {
			guiRef.appendLog(log);
		}
	}

	private char[] hexStringToCharArray(String hex) {
		int len = hex.length();
		char[] result = new char[len / 2];
		for (int i = 0; i < len; i += 2) {
			result[i / 2] = (char) Integer.parseInt(hex.substring(i, i + 2), 16);
		}
		return result;
	}

	public void setInstructionQueue(List<ExecutableInstruction> list) {
		instructionQueue.clear();
		instructionQueue.addAll(list);
	}

	public List<ExecutableInstruction> getInstructionQueue() {
		return instructionQueue;
	}

	public int getRegister(int regNum) {
		return rMgr.getRegister(regNum);
	}

	public void setGuiRef(VisualSimulatorGUI guiRef) {
		this.guiRef = guiRef;
	}

	public InstTable getInstTable() {
		return instTable;
	}
}
