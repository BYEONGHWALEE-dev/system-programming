package SP25_simulator;

import java.io.File;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라 ResourceManager에 접근하여
 * 작업을 수행한다.
 * 
 * 작성중의 유의사항 : 1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나
 * 완전히 대체하는 것은 지양할 것. 2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨. 3) 모든 void 타입의 리턴값은
 * 유저의 필요에 따라 다른 리턴 타입으로 변경 가능. 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된
 * 한글은 상관 없음)
 * 
 * 
 * 
 * + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수
 * 있습니다.
 */
public class SicSimulator {
	ResourceManager rMgr;
	InstLuncher instLuncher;
	InstTable instTable;

	List<ExecutableInstruction> instructionQueue = new ArrayList<>();
	int currentInstructionIndex = 0;

	public SicSimulator(ResourceManager resourceManager, String instFile) {
		// 필요하다면 초기화 과정 추가
		this.rMgr = resourceManager;
		this.instLuncher = new InstLuncher(resourceManager);
		this.instTable = new InstTable(instFile);
	}

	/**
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행. 단, object code의 메모리 적재 및 해석은
	 * SicLoader에서 수행하도록 한다.
	 */
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등 */
		rMgr.initializeResource();
		currentInstructionIndex = 0;
		instructionQueue.clear();
		// 실제 instructionQueue는 SicLoader에서 채운다
	}

	/**
	 * 1개의 instruction이 수행된 모습을 보인다.
	 */
	public void oneStep(VisualSimulatorGUI gui) {
		if(currentInstructionIndex >= instructionQueue.size()) return;

		ExecutableInstruction inst = instructionQueue.get(currentInstructionIndex);
		char[] bytes = hexStringToCharArray(inst.objectCode);

		Instruction info = instTable.getByMnemonic(inst.mnmonic);
		instLuncher.execute(info, bytes);

		gui.appendLog(String.format("실행 : %s (%s) at 0x04X", inst.mnmonic, inst.objectCode, inst.address));
		gui.update(this);

		currentInstructionIndex++;
	}

	/**
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep(VisualSimulatorGUI gui) {
		while(currentInstructionIndex < instructionQueue.size()) {
			oneStep(gui);
		}
	}

	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
	 */
	public void addLog(String log) {
	}

/**
 * object  code (hex String_ -> char[]로 변환
 */
	private char[] hexStringToCharArray(String hex) {
		int len = hex.length();
		char[] result = new char[len / 2];
		for(int i = 0; i < len; i += 2) {
			result[i / 2] = (char) Integer.parseInt(hex.substring(i, i + 2), 16);
		}
		return result;
	}

	/**
	 * 외부에서 instruction list를 받아올 수 있게
	 */
	public void setInstructionQueue(List<ExecutableInstruction> list) {
		instructionQueue.clear();
		instructionQueue.addAll(list);
	}

	public int getRegister(int regNum){
		return rMgr.getRegister(regNum);
	}

}

