package SP25_simulator;

import java.util.FormattableFlags;

public class InstLuncher {
    private final ResourceManager rMgr;

    public InstLuncher(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
    }

    /**
     * 주어진 Instruction 정보와 object code 바이트 배열을 받아서 명령어 실행
     */
    public int execute(Instruction inst, char[] bytes, int currentAddr) {
        String mnemonic = inst.getMnemonic();
        int format = inst.getFormat();

        switch (format) {
            case 1 -> {
                // Format 1은 1바이트 명령어 -> 필요없음
                System.out.printf("✔ Format 1 명령어 [%s] 실행 (아직 구현되지 않음)\n", mnemonic);
                return currentAddr + 1;
            }

            case 2 -> {
                setPcRegister2Format(currentAddr);
                // Format 2는 2바이트, 대부분 레지스터 연산
                switch (mnemonic) {
                    case "COMPR" -> doCompr(bytes);
                    case "ADDR"  -> doAddr(bytes);
                    case "TIXR" -> doTixr(bytes);
                    case "SUBR"  -> doSubr(bytes);
                    case "CLEAR" -> doClear(bytes);
                    default      -> System.out.println("⚠ Format 2 명령어 중 지원하지 않는 명령어: " + mnemonic);
                }
                return currentAddr + 2;
            }

            case 3, 4 -> {
                boolean[] flags = decodeNixbpe(bytes);
                int formatLen = flags[5] ? 4 : 3;
                int disp = fetchDisp(bytes, flags[5]); // 추출해낼때 10진법으로 나옴
                setPcRegister(currentAddr, flags[5]);

                int nextExecuteAddr = 0;
                // Format 3/4는 기존처럼 object code를 메모리에서 fetch해서 실행
                switch (mnemonic) {
                    case "LDA"   -> {doLda(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "STA"   -> {doSta(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "LDX"   -> {doLdx(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "STX"   -> {doStx(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "LDB"   -> {doLdb(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "LDCH"  -> {doLdch(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "LDT"   -> {doLdt(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "STB"   -> {doStb(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "STL" -> {doStl(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "STCH" -> {doStch(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "COMP" -> {doComp(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "J"     -> nextExecuteAddr = doJump(flags, disp);
                    case "JEQ"   -> nextExecuteAddr = doJeq(flags, disp, currentAddr);
                    case "JSUB"  -> nextExecuteAddr = doJsub(flags, disp, currentAddr);
                    case "JGT"   -> nextExecuteAddr = doJgt(flags, disp, currentAddr);
                    case "JLT"   -> nextExecuteAddr = doJlt(flags, disp, currentAddr);
                    case "RSUB"  -> nextExecuteAddr = doRsub();
                    case "TD"    -> {doTd(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "RD"    -> {doRd(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    case "WD"    -> {doWd(flags, disp); nextExecuteAddr = currentAddr + formatLen;}
                    default      -> {System.out.println("⚠ Format 3/4 명령어 중 지원하지 않는 명령어: " + mnemonic); nextExecuteAddr = currentAddr + 1;}
                }
                return nextExecuteAddr;
            }


        }
        return currentAddr + 1;
    }



    /**
     * Format 3: LDA target address
     */
    private void doLda(boolean[] flags, int disp) {
        int value = resolveTargetValue(disp, flags);
        rMgr.setRegister(0, value);

        System.out.printf("✔ LDA 완료: A ← %06X\n", value);
    }


    private void doLdx(boolean[] flags, int disp) {
        int result = resolveTargetValue(disp, flags);
        rMgr.setRegister(1, result);

        System.out.printf("✔ LDX 완료: X ← %06X\n", result);
    }

    private void doLdch(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);

        char memByte =  rMgr.getMemory(addr, 1)[0];
        int byteVal = memByte & 0xFF;

        int a = rMgr.getRegister(0);
        int newA = (a & 0xFFFF00) | byteVal;
        rMgr.setRegister(0, newA);

        System.out.printf("✔ LDCH → A ← M[0x%04X] = %02X → A = %06X\n", addr, byteVal, newA);
    }

    private void doLdb(boolean[] flags, int disp) {
        int value = resolveTargetValue(disp, flags);
        rMgr.setRegister(3, value); // B 레지스터

        System.out.printf("✔ LDB 완료: B ← %06X\n", value);
    }

    private void doLdt(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);

        // 메모리에서 3바이트 읽음
        char[] data = rMgr.getMemory(addr, 3);
        int value = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);

        rMgr.setRegister(5, value);

        System.out.printf("✔ LDT → T ← M[0x%04X] = %06X\n", addr, value);
    }

    private void doSta(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);
        int a = rMgr.getRegister(0);

        char[] data = {
                (char)((a >> 16) & 0xFF),
                (char)((a >> 8) & 0xFF),
                (char)(a & 0xFF)
        };
        rMgr.setMemory(addr, data, 3);
        System.out.printf("✔ STA → M[0x%04X] ← A = %06X\n", addr, a);
    }

    private void doStx(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);
        int x = rMgr.getRegister(1);

        char[] data = {
                (char)((x >> 16) & 0xFF),
                (char)((x >> 8) & 0xFF),
                (char)(x & 0xFF)
        };
        rMgr.setMemory(addr, data, 3);
        System.out.printf("✔ STX → M[0x%04X] ← X = %06X\n", addr, x);
    }

    private void doStb(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);
        int b = rMgr.getRegister(3);

        char[] data = {
                (char)((b >> 16) & 0xFF),
                (char)((b >> 8) & 0xFF),
                (char)(b & 0xFF)
        };
        rMgr.setMemory(addr, data, 3);
        System.out.printf("✔ STB → M[0x%04X] ← B = %06X\n", addr, b);
    }

    private void doStl(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);
        int l = rMgr.getRegister(2);
        rMgr.storeWord(addr, l);
        System.out.printf("✔ STL → [0x%04X] ← L = %06X\n", addr, l);
    }

    private void doStch(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);

        int a = rMgr.getRegister(0);
        char byteVal = (char)(a & 0xFF);

        rMgr.setMemory(addr, new char[]{byteVal}, 1);

        System.out.printf("✔ STCH → M[0x%04X] ← A의 하위 바이트 = %02X\n", addr, (int)byteVal);
    }

    private void doComp(boolean[] flags, int disp) {
        char[] data;
        if (flags[1]) {
            // immediate 모드: disp 값을 char[]로 감싸기
            data = new char[] {
                    (char)((disp >> 16) & 0xFF),
                    (char)((disp >> 8) & 0xFF),
                    (char)(disp & 0xFF)
            };
        } else {
            // 메모리에서 3바이트 로드
            int addr = resolveTargetAddr(disp, flags);
            data = rMgr.getMemory(addr, 3);
        }
        int memVal = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);

        int regA = rMgr.getRegister(0);
        int cmp = Integer.compare(regA, memVal);

        rMgr.setRegister(9, cmp);

        System.out.printf("✔ COMP → A(%06X) vs (%06X) → SW = %d\n", regA, memVal, cmp);
    }

    private int doJsub(boolean[] flags, int disp, int currentAddr) {
        // return address -> linkage register에 삽입
        int targetAddr = resolveTargetAddr(disp, flags);
        int linkageStoredAddr = currentAddr + (flags[5] ? 4 : 3);
        rMgr.setRegister(2, linkageStoredAddr);        // L ← 현재 주소
        System.out.printf("✔ JSUB → PC ← 0x%04X (L = 0x%04X)\n", targetAddr, currentAddr);

        return targetAddr;
    }


    private int doRsub(){
        int retAddr = rMgr.getRegister(2);
        System.out.printf("✔ RSUB → PC ← 0x%04X (return)\n", retAddr);

        return retAddr;
    }

    private int doJump(boolean[] flags, int disp) {
        int targetAddr = resolveTargetAddr(disp, flags);
        System.out.printf("✔ JUMP → PC ← 0x%04X\n", targetAddr);

        return targetAddr;
    }

    private int doJgt(boolean[] flags, int disp, int currentAddr) {
        if (rMgr.getRegister(9) > 0) {
            System.out.println("✔ JGT 조건 충족 → 점프");
            return doJump(flags, disp);
        } else {
            System.out.println("✘ JGT 조건 불충족 → 점프 안 함");
        }
        return currentAddr + (flags[5] ? 4 : 3);
    }

    private int doJlt(boolean[] flags, int disp, int currentAddr) {
        if (rMgr.getRegister(9) < 0) {
            System.out.println("✔ JLT 조건 충족 → 점프");

            return doJump(flags, disp);
        } else {
            System.out.println("✘ JLT 조건 불충족 → 점프 안 함");
        }
        return currentAddr + (flags[5] ? 4 : 3);
    }

    private int doJeq(boolean[] flags, int disp, int currentAddr) {
        if (rMgr.getRegister(9) == 0) {
            System.out.println("✔ JEQ 조건 충족 → 점프");
            return doJump(flags, disp);
        } else {
            System.out.println("✘ JEQ 조건 불충족 → 점프 안 함");
        }
        return currentAddr + (flags[5] ? 4 : 3);
    }

    // ===================== Device ========================

    private void doTd(boolean[] flags, int disp) {
        int addr = resolveTargetAddr(disp, flags);

        String devKey = resolveDeviceKey(addr);
        boolean available = rMgr.testDevice(devKey);
        rMgr.setRegister(9, available ? 1 : 0);

        System.out.printf("✔ TD → Test Device %s → SW = %d\n", devKey, available ? 1 : 0);
    }

    private void doRd(boolean[] flags, int disp) {
        int offset = rMgr.getRegister(1);
        int addr = resolveTargetAddr(disp, flags);
        String devKey = resolveDeviceKey(addr);
        char[] data = rMgr.readDevice(devKey);
        System.out.println(data);
        int value = (data != null && data.length > 0) ? data[0] & 0xFF : 0;

        rMgr.setRegister(0, value);
        System.out.printf("✔ RD → A ← Device[%s] = %02X\n", devKey, value);
    }

    private void doWd(boolean[] flags, int disp) {
        int offset = rMgr.getRegister(1);
        int addr = resolveTargetAddr(disp, flags);
        String devKey = resolveDeviceKey(addr);
        int a = rMgr.getRegister(0);
        char[] data = {(char)(a & 0xFF)};

        rMgr.writeDevice(devKey, data, 1);
        System.out.printf("✔ WD → Device[%s] ← A = %02X\n", devKey, a & 0xFF);
    }

    // ===================== Format 2 ========================
    /**
     * Format 2: CLEAR r1
     * ex) CLEAR A → register[0] = 0
     */
    private void doClear(char[] bytes) {
        if (bytes.length < 2) return;
        int r1 = (bytes[1] >> 4) & 0x0F;
        rMgr.setRegister(r1, 0);
        System.out.println("✔ CLEAR R" + r1);
    }

    private void doAddr(char[] bytes) {
        int r1 = getR1(bytes);
        int r2 = getR2(bytes);
        int sum = rMgr.getRegister(r1) + rMgr.getRegister(r2);;
        rMgr.setRegister(r1, sum);

        System.out.printf("✔ ADDR → R%d ← R%d + R%d = %d\n", r1, r1, r2, sum);
    }

    private void doCompr(char[] bytes) {
        int r1 = getR1(bytes);
        int r2 = getR2(bytes);
        int val1 = rMgr.getRegister(r1);
        int val2 = rMgr.getRegister(r2);

        int cmp = Integer.compare(val1, val2);
        rMgr.setRegister(9, cmp);

        System.out.printf("✔ COMPR R%d vs R%d → SW = %d\n", r1, r2, cmp);
    }

    private void doSubr(char[] bytes) {
        int r1 = getR1(bytes);
        int r2 = getR2(bytes);
        int diff = rMgr.getRegister(r1) - rMgr.getRegister(r2);
        rMgr.setRegister(r1, diff);

        System.out.printf("✔ SUBR → R%d ← R%d - R%d = %d\n", r1, r1, r2, diff);
    }

    private void doTixr(char[] bytes) {
        int r1 = (bytes[1] & 0xF0) >> 4;
        rMgr.setRegister(1, rMgr.getRegister(1) + 1); // X++
        int cmp = Integer.compare(rMgr.getRegister(1), rMgr.getRegister(r1));

        rMgr.setRegister(9, cmp);
        System.out.println("✔ TIXR → X와 R" + r1 + " 비교 후 SW = " + cmp);
    }



    // ============================ Utility =========================

    private int resolveTargetAddr(int disp, boolean[] flags) {
        boolean x = flags[2], b = flags[3], p = flags[4];
        int targetAddr;

        if (p) {
            targetAddr = rMgr.getRegister(8) + disp; // PC-relative
        } else if (b) {
            targetAddr = rMgr.getRegister(3) + disp; // Base-relative
        } else {
            targetAddr = disp; // Absolute
        }

        if (x) {
            targetAddr += rMgr.getRegister(1); // X 레지스터 indexing
        }

        return targetAddr;
    }

    private boolean[] decodeNixbpe(char[] bytes) {
        int ni = bytes[0] & 0x03;
        int xbpe = (bytes[1] & 0xF0) >> 4;

        boolean n = (ni & 0x2) != 0;
        boolean i = (ni & 0x1) != 0;
        boolean x = (xbpe & 0x8) != 0;
        boolean b = (xbpe & 0x4) != 0;
        boolean p = (xbpe & 0x2) != 0;
        boolean e = (xbpe & 0x1) != 0;

        return new boolean[]{n,i,x,b,p,e};
    }

    private int fetchDisp(char[] bytes, boolean format4) {
        if (bytes == null || bytes.length < 3) {
            throw new IllegalArgumentException("Instruction bytes are too short.");
        }

        if (format4) {
            if (bytes.length < 4) {
                throw new IllegalArgumentException("Format 4 instruction requires at least 4 bytes.");
            }
            return ((bytes[1] & 0x0F) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } else {
            int disp = ((bytes[1] & 0x0F) << 8) | (bytes[2] & 0xFF);
            // Sign-extend 12-bit displacement to 32-bit integer (for negative PC-relative)
            if ((disp & 0x800) != 0) {
                disp |= 0xFFFFF000;
            }
            return disp;
        }
    }

    private int resolveTargetValue(int disp, boolean[] flags) {
        int addr = resolveTargetAddr(disp, flags);
        boolean n = flags[0], i = flags[1];

        if(n && !i) {
            // indirect
            char[] ptr = rMgr.getMemory(addr, 3);
            int indirectAddr = toInt(ptr);
            char[] data = rMgr.getMemory(indirectAddr, 3);
            return toInt(data);
        } else if (!n && i) {
            // immediate
            return addr;
        } else{
            // simple
            char[] data = rMgr.getMemory(addr, 3);
            return toInt(data);
        }
    }

    private String resolveDeviceKey(int addr) {
        char rawByte = rMgr.memory[addr];
        int byteVal = rawByte & 0xFF;
        return String.format("%02X", byteVal);
    }

    private int getR1(char[] bytes) {
        return (bytes[1] >> 4) & 0x0F;
    }

    private int getR2(char[] bytes) {
        return bytes[1] & 0x0F;
    }

    private int toInt(char[] bytes) {
        if(bytes == null || bytes.length != 3) return 0;
        return ((bytes[0] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
    }

    private void setPcRegister(int currentAddr, boolean isFormat4) {
        int nextPc = currentAddr + (isFormat4 ? 4 : 3);
        rMgr.setRegister(8, nextPc);
    }

    private void setPcRegister2Format(int currentAddr){
        rMgr.setRegister(8, currentAddr + 2);
    }
}
