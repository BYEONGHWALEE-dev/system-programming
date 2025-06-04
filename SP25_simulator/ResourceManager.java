package SP25_simulator;

import SP25_simulator.section.SymbolTable;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다. 크게 네가지의 가상 자원 공간을 선언하고, 이를
 * 관리할 수 있는 함수들을 제공한다.
 * 
 * 
 * 1) 입출력을 위한 외부 장치 또는 device 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.
 * 3) 연산을 수행하는데 사용하는 레지스터 공간. 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들.
 * 
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면, 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서
 * 차이가 있다.
 */
public class ResourceManager {
	/**
	 * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다. 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의
	 * 파일을 의미한다. deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을
	 * 넣음으로서 이를 관리할 수 있다.
	 * 
	 * 변형된 형태로 사용하는 것 역시 허용한다. 예를 들면 key값으로 String대신 Integer를 사용할 수 있다. 파일 입출력을 위해
	 * 사용하는 stream 역시 자유로이 선택, 구현한다.
	 * 
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String, Object> deviceManager = new HashMap<String, Object>();
	// 디바이스

	char[] memory = new char[65536]; // String으로 수정해서 사용하여도 무방함.
	int[] register = new int[10];
	double register_F;

	SymbolTable symtabList;
	// 이외에도 필요한 변수 선언해서 사용할 것.

	// Metadata
	private String programName;
	private String programLength;
	private String startAddress = "1000";
	private String executionStartAddress = "1000";
	private String memoryStartAddress = "1000";
	private String targetAddress = "1033";

	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
	 */
	public void initializeResource() {
		for(int i = 0; i < register.length; i++) register[i] = 0;
		for(int i = 0; i < memory.length; i++) memory[i] = 0;

		deviceManager.clear();

		// 디바이스 추가
		try{
			// 입력 파일 디바이스 등록
			BufferedReader reader = new BufferedReader(new FileReader("F1.txt"));
			deviceManager.put("F1", reader);

			// 출력 파일 디바이스 등록
			PrintWriter outWriter = new PrintWriter(new FileWriter("05.txt"));
			deviceManager.put("05", outWriter);

			System.out.println("디바이스 등록완료");
		} catch (IOException e) {
			System.err.println("디바이스 초기화 중 오류:" + e.getMessage());
		}
	}

	/**
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할. 프로그램을 종료하거나 연결을 끊을 때
	 * 호출한다.
	 */
	public void closeDevice() {
		for(Object obj : deviceManager.values()) {
			if(obj instanceof Scanner scanner) {
				scanner.close();
			} else if (obj instanceof PrintWriter writer) {
				writer.close();
			}
		}
		deviceManager.clear();
	}

	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수. 입출력 stream을 열고 deviceManager를
	 * 통해 관리시킨다.
	 * 
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
	 */
	public boolean testDevice(String devName) {
		return deviceManager.containsKey(devName);
	}

	/**
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 * 
	 * @param devName 디바이스의 이름
	 * @return 가져온 데이터
	 */
	public char[] readDevice(String devName) {
		File file = new File(devName + ".txt");

		try {
			// 현재 파일 전체 읽기
			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				int ch;
				while ((ch = reader.read()) != -1) {
					sb.append((char) ch);
				}
			}

			// 내용이 비었으면 아무것도 반환하지 않음
			if (sb.length() == 0) {
				return new char[0];
			}

			// 맨 앞 글자를 반환
			char firstChar = sb.charAt(0);
			String newContent = sb.substring(1); // 나머지 내용

			// 파일 덮어쓰기
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
				writer.write(newContent);
			}

			return new char[]{firstChar};

		} catch (IOException e) {
			System.err.println("⚠ readDevice 오류: " + e.getMessage());
			return new char[0];
		}
	}


	/**
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 * 
	 * @param devName 디바이스의 이름
	 * @param data    보내는 데이터
	 * @param num     보내는 글자의 개수
	 */
	public void writeDevice(String devName, char[] data, int num) {
		try{
			PrintWriter writer;

			// 이미 열림
			if(deviceManager.containsKey(devName)) {
				writer = (PrintWriter) deviceManager.get(devName);
			} else {
				File file = new File(devName + "_out.txt");
				writer = new PrintWriter(new FileWriter(file, true));
				deviceManager.put(devName, writer);
			}

			for(int i = 0; i < num && i < data.length; i++) {
				writer.print(data[i]);
			}

			writer.flush();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 * 
	 * @param location 메모리 접근 위치 인덱스
	 * @param num      데이터 개수
	 * @return 가져오는 데이터
	 */
	public char[] getMemory(int location, int num) {
		if(location  < 0 || location + num > memory.length) return null;
		char[] result = new char[num];
		System.arraycopy(memory, location, result, 0, num);
		return result;

	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다.
	 * 
	 * @param locate 접근 위치 인덱스
	 * @param data   저장하려는 데이터
	 * @param num    저장하는 데이터의 개수
	 */
	public void setMemory(int locate, char[] data, int num) {
		if(locate >= 0 && locate + num <= memory.length) {
			System.arraycopy(data, 0, memory, locate, num);
		}
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * 
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum) {
		if(regNum < 0 || regNum >= register.length) return 0;
		return register[regNum];
	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * 
	 * @param regNum 레지스터의 분류번호
	 * @param value  레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value) {
		if(regNum >=0 && regNum < register.length) register[regNum] = value;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 * 
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data) {
		return Integer.toHexString(data).toCharArray();
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 * 
	 * @param data
	 * @return
	 */
	public int byteToInt(byte[] data) {
		int result = 0;
		for(byte b : data) {
			result = (result << 8) | (b & 0xff);
		}
		return result;
	}

	public void storeWord(int addr, int value){
		memory[addr]     = (char)((value >> 16) & 0xFF);
		memory[addr + 1] = (char)((value >> 8) & 0xFF);
		memory[addr + 2] = (char)(value & 0xFF);
	}

	// ===================== 프로그램 정보 Getter ======================
	public String getStartAddress() {
		return startAddress;
	}

	public int getStartAddressInt() {
		return Integer.parseInt(startAddress, 16);
	}

	public String getExecutionStartAddress() {
		return executionStartAddress;
	}

	public String getMemoryStartAddress() {
		return memoryStartAddress;
	}

	public String getTargetAddress() {
		return targetAddress;
	}

	// Setter for loader use
	public void setProgramInfo(String name, int start, int length) {
		this.programName = name;
		this.startAddress = String.format("%06X", start);
		this.programLength = String.format("%06X", length);
	}

	public void setEndInfo(String execStart, String memStart, String target){
		this.executionStartAddress = execStart;
		this.memoryStartAddress = memStart;
		this.targetAddress = target;
	}

	public String getProgramName() {
		return programName;
	}

	public String getProgramLength() {
		return programLength;
	}

	public void showMemory() {
		int start = -1;
		int end = -1;

		// 시작 주소와 끝 주소 계산
		for (int i = 0; i < memory.length; i++) {
			if (memory[i] != 0) {
				if (start == -1) start = i;
				end = i;
			}
		}

		// 메모리에 아무 값도 없을 경우
		if (start == -1) {
			System.out.println("📭 메모리에 로드된 값이 없습니다.");
			return;
		}

		// 시작 ~ 끝 범위 출력
		for (int i = start; i <= end; i++) {
			System.out.printf("0x%04X : 0x%02X\n", i, (int) memory[i]);
		}
	}
}