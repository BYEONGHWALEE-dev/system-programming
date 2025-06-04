package SP25_simulator;

import java.awt.EventQueue;
import java.io.File;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다. 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트
 * 하는 역할을 수행한다.
 * 
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator {
	ResourceManager resourceManager = new ResourceManager();
	String instFilePath = "inst_table.txt";
	SicSimulator sicSimulator = new SicSimulator(resourceManager, instFilePath, new VisualSimulatorGUI());
	SicLoader sicLoader = new SicLoader(resourceManager, sicSimulator);

	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
//	public void load(File program) {
//		// ...
//		sicLoader.load(program);
//		sicSimulator.load(program);
//	};

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep(VisualSimulatorGUI gui) {
		sicSimulator.oneStep(gui);
	};

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep(VisualSimulatorGUI gui) {
		sicSimulator.allStep(gui);
	};

	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	public void update(VisualSimulatorGUI gui) {
		// 레지스터
		for(int i =0; i <= 9; i++) {
			int val = resourceManager.getRegister(i);
			gui.setRegisterField(i, val);
		}

		// Header 정보
		gui.setHeaderInfo(
				resourceManager.getProgramName(),
				resourceManager.getStartAddress(),
				resourceManager.getProgramLength()
		);

		// End 정보
		gui.setEndInfo(
				resourceManager.getExecutionStartAddress(),
				resourceManager.getMemoryStartAddress(),
				resourceManager.getTargetAddress()
		);
	};
}
