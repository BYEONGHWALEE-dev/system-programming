package SP25_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SicLoader {
	ResourceManager rMgr;
	SicSimulator sicSimulator;

	public SicLoader(ResourceManager resourceManager, SicSimulator sicSimulator) {
		setResourceManager(resourceManager);
		this.sicSimulator = sicSimulator;
	}

	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr = resourceManager;
	}

	public void load(File objectCode) {
		// 💡 리소스 초기화는 가장 먼저 수행
		rMgr.initializeResource();

		try (BufferedReader br = new BufferedReader(new FileReader(objectCode))) {
			String line;
			List<ExecutableInstruction> instructionQueue = new ArrayList<>();
			int programStartAddr = 0;

			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) continue;
				char recordType = line.charAt(0);

				switch (recordType) {
					case 'H' -> {
						String name = line.substring(1, 7).trim();
						String startAddr = line.substring(7, 13).trim();
						String length = line.substring(13, 19).trim();
						rMgr.setProgramInfo(name, startAddr, length);
						programStartAddr = Integer.parseInt(startAddr, 16);
					}

					case 'T' -> {
						String startAddrStr = line.substring(1, 7);
						String lengthStr = line.substring(7, 9);
						String objectCodes = line.substring(9);

						int startAddr = Integer.parseInt(startAddrStr, 16);
						int length = Integer.parseInt(lengthStr, 16);

						int byteIndex = 0;
						while (byteIndex < objectCodes.length()) {
							String byteStr = objectCodes.substring(byteIndex, byteIndex + 2);
							char hexChar = (char) Integer.parseInt(byteStr, 16);
							rMgr.memory[startAddr + (byteIndex / 2)] = hexChar;
							byteIndex += 2;
						}

						// 기본적으로 Format 3 기준 (3바이트 = 6자리)으로 instruction 추출
						int i = 0;
						while (i + 6 <= objectCodes.length()) {
							String instCode = objectCodes.substring(i, i + 6);
							instructionQueue.add(new ExecutableInstruction(startAddr + (i / 2), instCode));
							i += 6;
						}
					}

					case 'M' -> {
						String addrStr = line.substring(1, 7);
						String lenStr = line.substring(7, 9);

						int addr = Integer.parseInt(addrStr, 16);
						int len = Integer.parseInt(lenStr);

						char[] data = rMgr.getMemory(addr, 3);
						int value = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);

						int mask = (1 << (len * 4)) - 1;
						int modified = (value & mask) + programStartAddr;

						int newValue = (value & ~mask) | (modified & mask);
						char[] newData = {
								(char) ((newValue >> 16) & 0xFF),
								(char) ((newValue >> 8) & 0xFF),
								(char) (newValue & 0xFF)
						};

						rMgr.setMemory(addr, newData, 3);
						System.out.printf("✔ M 수정: [0x%06X] + startAddr(0x%06X) = 0x%06X\n", value & mask, programStartAddr, modified);
					}

					case 'E' -> {
						String execAddr = line.length() > 1 ? line.substring(1).trim() : rMgr.getStartAddress();
						rMgr.setEndInfo(execAddr, rMgr.getStartAddress(), "1033");

						int execAddrInt = Integer.parseInt(execAddr, 10);
						rMgr.setRegister(8, execAddrInt);
					}

					default -> {
						// 무시
					}
				}
			}

			sicSimulator.setInstructionQueue(instructionQueue);
			System.out.printf("✔ 로드 완료: 명령어 %d개\n", instructionQueue.size());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
