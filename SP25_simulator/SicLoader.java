package SP25_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다.
 *
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다. - program code를 메모리에 적재시키기 - 주어진 공간만큼 메모리에 빈
 * 공간 할당하기 - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;

	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
	}

	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 *
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr = resourceManager;
	}

	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록
	 * 한다. load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 *
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode) {
		try(BufferedReader br = new BufferedReader(new FileReader(objectCode))) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.isEmpty()) continue;
				char recordType = line.charAt(0);

				switch(recordType) {
					case 'H' : {
						// Header Record
						String name = line.substring(1, 7).trim();
						String startAddr = line.substring(7,13).trim();
						String length = line.substring(13, 19).trim().trim();
						rMgr.setProgramInfo(name, startAddr, length);
						break;
					}

					case 'T' : {
						// Text Record : T + 시작주소(6) + 길이(2) + object code
						String startAddrStr = line.substring(1, 7);
						String lengthStr = line.substring(7, 9);
						String objectCodes = line.substring(9);

						int staratAddr = Integer.parseInt(startAddrStr);
						int length = Integer.parseInt(lengthStr, 16);

						for(int i = 0; i < length * 2; i+=2){
							String byteStr = objectCodes.substring(i, i + 2);
							char hexChar = (char) Integer.parseInt(byteStr, 16);;
							rMgr.memory[staratAddr + (i / 2)] = hexChar;
						}
						break;
					}

					case 'E' : {
						// END Record
						String execAddr = line.substring(1).trim();
						rMgr.setEndInfo(execAddr, rMgr.getStartAddress(), "1033"); // Target
						break;
					}

					default :
						// 무시 또는 예외처리 가능
						break;
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	};

}
