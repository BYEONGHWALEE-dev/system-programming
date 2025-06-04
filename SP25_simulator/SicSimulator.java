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
	private int currentInstructionIndex;

	// 새로 추가: 섹션 리스트와 현재 섹션 이름 프로퍼티
	private List<SectionInfo> sections = new ArrayList<>();
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private String currentSectionName = "";

	// 실행해야 할 주소
	private int currentExecuteAddress = 0;

	public SicSimulator(ResourceManager resourceManager, String instFile, VisualSimulatorGUI guiRef) {
		this.rMgr = resourceManager;
		this.currentExecuteAddress = rMgr.getStartAddressInt();
		this.instLuncher = new InstLuncher(resourceManager);
		this.instTable = new InstTable(instFile);
		this.guiRef = guiRef;
	}

	/** 모델에 섹션 리스트를 설정하고, 기본 현재 섹션을 첫 섹션으로 초기화 */
	public void setSections(List<SectionInfo> sections) {
		this.sections.clear();
		this.sections.addAll(sections);
		if (!sections.isEmpty()) {
			setCurrentSection(sections.getFirst());
		}
	}

	public List<SectionInfo> getSections() {
		return sections;
	}

	/** 현재 섹션 변경 시 이벤트 발행 */
	private void setCurrentSection(SectionInfo section) {
		String sectionName = section.getSectionName();
		int startAddress = section.getStartAddress();
		int length = section.getLength();

		rMgr.setProgramInfo(sectionName, startAddress, length);
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

	}

	/** 한 단계 실행 및 GUI 업데이트 */
	public boolean oneStep(VisualSimulatorGUI gui) {
		int execAddr = currentExecuteAddress;

		// 메모리에서 object code fetch
		char b1 = rMgr.memory[execAddr];
		char b2 = rMgr.memory[execAddr + 1];

		int rawOpcode = b1 & 0xFC;
		Instruction info = instTable.getByOpcode(rawOpcode);

		if(info == null) {
			String warn = String.format("⚠ 알 수 없는 명령어 at 0x%04X: %02X%02X", execAddr, (int)b1, (int)b2);
			gui.appendLog(warn);
			addLog(warn);
		}

		boolean format4 = ((b2 & 0x10) == 0x10 && info.getFormat() == 3);
		int instLen = switch (info.getFormat()) {
			case 1 -> 1;
			case 2 -> 2;
			case 3 -> format4 ? 4 : 3;
			default -> 0;
		};

		char[] bytes = new char[instLen];
		for(int i = 0; i < instLen; i++) {
			bytes[i] = rMgr.memory[execAddr + i];
		}

		// 현재 섹션 업데이트
		for(SectionInfo sec : sections) {
			int s = sec.getStartAddress();
			if(execAddr >= s && execAddr < s + sec.getLength()) {
				if (!sec.getSectionName().equals(currentSectionName)) {
					setCurrentSection(sec);
				}
				break;
			}
		}

		// 명령 실행 -> InstLuncher에서 다음 주소 반환
		int nextExecAddr = instLuncher.execute(info, bytes, execAddr);
		currentExecuteAddress = nextExecAddr;

		String logMsg = String.format("실행 : %s (%s) at 0x%04X", info.getMnemonic(), charArrayToHex(bytes), execAddr);
		gui.update(rMgr);
		addLog(logMsg);
		gui.showCurrentInstruction(String.format("%04X", execAddr), charArrayToHex(bytes), info.getMnemonic());

		// 종료 감지
		if(charArrayToHex(bytes).equalsIgnoreCase("3E2000")) {
			String msg = String.format("🛑 종료 명령 감지 (3E2000) at 0x%04X", execAddr);
			addLog(msg);
			gui.showCurrentInstruction(String.format("%04X", execAddr), charArrayToHex(bytes), info.getMnemonic());
			gui.disableExecutionButton();
			return true;
		}
		return false;
	}

	/** 남은 모든 명령어 연속 실행 */
	public void allStep(VisualSimulatorGUI gui) {
		while(true) {
			boolean isEnd = oneStep(gui);
			if(isEnd) {
				gui.disableExecutionButton();
				break;
			}
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

	public int getRegister(int regNum) {
		return rMgr.getRegister(regNum);
	}

	public void setGuiRef(VisualSimulatorGUI guiRef) {
		this.guiRef = guiRef;
	}

	public InstTable getInstTable() {
		return instTable;
	}

	public ResourceManager getResourceManager() {
		return rMgr;
	}

	public String charArrayToHex(char[] chars) {
		StringBuilder sb = new StringBuilder();
		for(char b : chars) {
			sb.append(String.format("%02X", (int)b));
		}

		return sb.toString();
	}
}
