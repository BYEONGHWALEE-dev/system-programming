package SP25_simulator;

import SP25_simulator.section.SectionInfo;
import SP25_simulator.section.SymbolTable;
import SP25_simulator.section.TextRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SicLoader {
	private ResourceManager rMgr;
	private SicSimulator sicSimulator;

	public SicLoader(ResourceManager resourceManager, SicSimulator sicSimulator) {
		this.rMgr = resourceManager;
		this.sicSimulator = sicSimulator;
	}

	public void load(File objectFile) {
		// Resource initializing
		rMgr.initializeResource();

		try {
			List<SectionInfo> sections = runPass1(objectFile);
			runPass2(sections, rMgr, sicSimulator);
			// 완료 디버깅
			System.out.printf("✔ 로드 완료: 명령어 %d개%n",
					sicSimulator.getInstructionQueue().size());
		} catch (IOException e) {
			throw new RuntimeException("Object 파일 로드 중 오류 발생", e);
		}
	}


	private List<SectionInfo> runPass1(File objectFile) throws IOException {
		List<SectionInfo> sectionInfoList = new ArrayList<>();
		SectionInfo currentSectionInfo = null;
		String lastExecAddr = null;

		try (BufferedReader br = new BufferedReader(new FileReader(objectFile))) {
			String line;
			int programBase = Integer.parseInt(rMgr.getStartAddress(), 16);
			int base = programBase;

			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) continue;
				char type = line.charAt(0);

				switch (type) {
					case 'H' -> {
						String name = line.substring(1, 7).trim();
						int relOff = Integer.parseInt(line.substring(7, 13), 16);
						int length = Integer.parseInt(line.substring(13, 19), 16);

						// 헤더 정보 저장
						rMgr.setProgramInfo(name, relOff, length);

						currentSectionInfo = new SectionInfo(name, /*abs*/ base + relOff, length);
						sectionInfoList.add(currentSectionInfo);

						base += length;
					}
					case 'D' -> {
						String body = line.substring(1);
						for (int i = 0; i + 12 <= body.length(); i += 12) {
							String sym = body.substring(i, i + 6).trim();
							int offset = Integer.parseInt(body.substring(i + 6, i + 12), 16);
							currentSectionInfo.getSymbolTable().addDefinition(sym, offset);
						}
					}
					case 'R' -> {
						String body = line.substring(1);
						for (int i = 0; i + 6 <= body.length(); i += 6) {
							String sym = body.substring(i, i + 6).trim();
							currentSectionInfo.getSymbolTable().addReference(sym);
						}
					}
					case 'T' -> {
						String startHex = line.substring(1, 7);
						String lenHex = line.substring(7, 9);
						String codes = line.substring(9);
						int relOff = Integer.parseInt(startHex, 16);
						int length = Integer.parseInt(lenHex, 16);
						currentSectionInfo.addTextRecord(
								new TextRecord(relOff, length, codes)
						);
					}
					case 'M' -> {
						currentSectionInfo.addModRecord(line);
					}
					case 'E' -> lastExecAddr = line.length() > 1 ? line.substring(1).trim() : rMgr.getStartAddress();
				}
			}
		}

		// 2) Pass1 끝난 뒤, all extref 해소
		for (SectionInfo sectionInfo : sectionInfoList) {
			for (String sym : sectionInfo.getSymbolTable().getAllReferences().keySet()) {
				Integer addr = resolveSymbolAddress(sym, sectionInfoList);
				if (addr == null) {
					throw new RuntimeException("Undefined extref");
				}
				sectionInfo.getSymbolTable().resolveReference(sym, addr);
			}
		}
		if (lastExecAddr != null) {
			rMgr.setEndInfo(lastExecAddr, rMgr.getStartAddress(), "");
		}
		return sectionInfoList;
	}

	/** sections 리스트에서 symbol 정의를 찾아 절대 주소 리턴 */
	private Integer resolveSymbolAddress(
			String symbol,
			List<SectionInfo> sections
	) {
		for (SectionInfo sec : sections) {
			int relOff = sec.getSymbolTable().searchDefinitionAddress(symbol);
			if (relOff >= 0) {
				return sec.getStartAddress() + relOff;
			}
		}
		return null;
	}

	private void runPass2(
			List<SectionInfo> sections,
			ResourceManager rMgr,
			SicSimulator sicSimulator
	) {
		// Text Record -> 메모리 로드
		for(SectionInfo sectionInfo : sections) {
			int sectionBase = sectionInfo.getStartAddress();
			for(TextRecord tr : sectionInfo.getTextRecords()) {
				int relOff = tr.getStartAddr();
				String objectCode = tr.getObjectCodes();
				for(int i = 0; i < objectCode.length(); i+=2) {
					int b = Integer.parseInt(objectCode.substring(i, i + 2), 16) & 0xFF;
					int byteOffset = i / 2;
					rMgr.memory[sectionBase + relOff + byteOffset] = (char) b; // -> 패킹 과정 두 글자를 하나의 char(1바이트)에 넣음
				}
			}
		}
		// 2) 재배치/링크 : M 레코드를 순회하며 실제 심볼 주소 넣음
		for(SectionInfo sectionInfo : sections) {
			int sectionBase = sectionInfo.getStartAddress();
			SymbolTable symbolTable = sectionInfo.getSymbolTable();

			for(String mline : sectionInfo.getModRecords()){
				int addrOff = Integer.parseInt(mline.substring(1, 7), 16);
				int halfN = Integer.parseInt(mline.substring(7, 9), 16);
				char sign = mline.charAt(9);
				String sym = mline.substring(10).trim();
				int addr = sectionBase + addrOff; // abs 주소
				int nbytes = (halfN + 1) / 2;
				int value = 0;
				// 메모리에서 기존의 값을 읽음
				for(int j = 0; j < nbytes; j++){
					value = (value << 8) | (rMgr.memory[addr + j] & 0xFF);
				}
				// 홀수일경우 오른쪽 바이트만 남김
				if(halfN % 2 != 0) {
					value &= (1 << (halfN*4)) - 1;
				}
				// Symbol 절대 주소 계산
				Integer symAddr = symbolTable.getReferenceAddress(sym);
				// relocate 계산
				int mask = (1 << (halfN*4)) - 1;
				int result = (sign == '+')
						? (value + symAddr) & mask
						: (value - symAddr) & mask;
				// 메모리에 다시 작성
				for(int j = 0; j < nbytes; j++){
					int shift = (nbytes - 1 - j) * 8;
					int byteVal = (result >> shift) & 0xFF;
					// 홀수 니블인 경우, 최상위 바이트의 상위 니블은 보존
					if (j == 0 && (halfN % 2 != 0)) {
						int orig = rMgr.memory[addr] & 0xFF;
						byteVal = (orig & 0xF0) | (byteVal & 0x0F);
					}
					rMgr.memory[addr + j] = (char) byteVal;
				}
			}
		}

		// 실행가능한 Instruction 모아놔야함
		List<ExecutableInstruction> queue = new ArrayList<>();
		for(SectionInfo sectionInfo : sections) {
			int sectionBase = sectionInfo.getStartAddress();
			for(TextRecord tr : sectionInfo.getTextRecords()) {
				parseInstructions(tr, sectionBase, queue);
			}
		}
		sicSimulator.setInstructionQueue(queue);
		int execStart = Integer.parseInt(rMgr.getExecutionStartAddress(), 16);
		rMgr.setRegister(8, execStart);
	}

	private void parseInstructions(
			TextRecord tr,
			int sectionBase,
			List<ExecutableInstruction> queue
	) {
		String codes = tr.getObjectCodes();
		int relOff = tr.getStartAddr();
		int i = 0;
		while (i + 2 <= codes.length()) {
			int currAddr = sectionBase + relOff + (i / 2);
			String opHex = codes.substring(i, i + 2);
			int raw = Integer.parseInt(opHex, 16);
			int opcode = raw & 0xFC;
			Instruction inst = sicSimulator.getInstTable().getByOpcode(opcode);
			if (inst == null) {
				i += 2;
				continue;
			}
			int format = inst.getFormat();
			boolean isFmt4 = false;
			if (format == 3 || format == 4) {
				if (i + 4 <= codes.length()) {
					int byte2 = Integer.parseInt(codes.substring(i + 2, i + 4), 16);
					isFmt4 = (byte2 & 0x10) != 0;
				}
			}
			int instLen = switch (format) {
				case 1 -> 2;
				case 2 -> 4;
				case 3, 4 -> isFmt4 ? 8 : 6;
				default -> 6;
			};
			if (i + instLen > codes.length()) break;
			String instCode = codes.substring(i, i + instLen);
			queue.add(new ExecutableInstruction(currAddr, instCode));
			i += instLen;
		}
	}
}
