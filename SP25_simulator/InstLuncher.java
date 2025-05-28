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
    public void execute(Instruction inst, char[] bytes) {
        String mnemonic = inst.getMnemonic();

        switch (mnemonic) {
            case "COMPR" -> doCompr(bytes);
            case "ADDR"  -> doAddr(bytes);
            case "SUBR"  -> doSubr(bytes);
            case "LDA"   -> doLda(bytes);
            case "STA"   -> doSta(bytes);
            case "CLEAR" -> doClear(bytes);
            case "JSUB"  -> doJsub(bytes);
            case "RSUB"  -> doRsub();
            case "J"    -> doJump(bytes);
            case "JEQ"  -> doJeq(bytes);
            case "JGT"  -> doJgt(bytes);
            case "JLT"  -> doJlt(bytes);
            case "LDX" -> doLdx(bytes);
            case "STX" -> doStx(bytes);
            case "LDB" -> doLdb(bytes);
            case "STB" -> doStb(bytes);
            case "TD" -> doTd(bytes);
            case "RD" -> doRd(bytes);
            case "WD" -> doWd(bytes);
            default      -> System.out.println("⚠ 지원하지 않는 명령어: " + mnemonic);
        }
    }

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

    /**
     * Format 3: LDA target address
     */
    private void doLda(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

        int value = resolveTargetValue(disp, flags);
        rMgr.setRegister(disp, value);

        System.out.printf("✔ LDA 완료: A ← %06X\n", value);
    }


    private void doLdx(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

        int result = resolveTargetValue(disp, flags);
        rMgr.setRegister(1, result);

        System.out.printf("✔ LDX 완료: X ← %06X\n", result);
    }

    private void doLdb(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

        int value = resolveTargetValue(disp, flags);
        rMgr.setRegister(3, value); // B 레지스터

        System.out.printf("✔ LDB 완료: B ← %06X\n", value);
    }

    private void doSta(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

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

    private void doStx(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

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

    private void doStb(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

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

    private void doJsub(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

        int targetAddr = resolveTargetAddr(disp, flags);
        int pc = rMgr.getRegister(8);

        rMgr.setRegister(9, pc);        // L ← 현재 PC
        rMgr.setRegister(8, targetAddr); // PC ← 타겟 주소

        System.out.printf("✔ JSUB → PC ← 0x%04X (L = 0x%04X)\n", targetAddr, pc);
    }


    private void doRsub(){
        int retAddr = rMgr.getRegister(9);
        rMgr.setRegister(8, retAddr);
        System.out.printf("✔ RSUB → PC ← 0x%04X (return)\n", retAddr);
    }

    private void doJump(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);

        int targetAddr = resolveTargetAddr(disp, flags);
        rMgr.setRegister(8, targetAddr);

        System.out.printf("✔ JUMP → PC ← 0x%04X\n", targetAddr);
    }

    private void doJgt(char[] bytes) {
        if (rMgr.getRegister(9) > 0) {
            doJump(bytes);
            System.out.println("✔ JGT 조건 충족 → 점프");
        } else {
            System.out.println("✘ JGT 조건 불충족 → 점프 안 함");
        }
    }

    private void doJlt(char[] bytes) {
        if (rMgr.getRegister(9) < 0) {
            doJump(bytes);
            System.out.println("✔ JLT 조건 충족 → 점프");
        } else {
            System.out.println("✘ JLT 조건 불충족 → 점프 안 함");
        }
    }

    private void doJeq(char[] bytes) {
        if (rMgr.getRegister(9) == 0) {
            doJump(bytes);
            System.out.println("✔ JEQ 조건 충족 → 점프");
        } else {
            System.out.println("✘ JEQ 조건 불충족 → 점프 안 함");
        }
    }

    // ===================== Format 2 ========================
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
        rMgr.setRegister(r1, cmp);

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
        int r1 = bytes[1] & 0x0F;
        rMgr.setRegister(1, rMgr.getRegister(1) + 1); // X++
        int cmp = Integer.compare(rMgr.getRegister(1), rMgr.getRegister(r1));

        rMgr.setRegister(9, cmp);
        System.out.println("✔ TIXR → X와 R" + r1 + " 비교 후 SW = " + cmp);
    }

    private void doTd(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);
        int addr = resolveTargetAddr(disp, flags);

        String devKey = resolveDeviceKey(addr);
        boolean available = rMgr.testDevice(devKey);
        rMgr.setRegister(9, available ? 1 : 0);

        System.out.printf("✔ TD → Test Device %s → SW = %d\n", devKey, available ? 1 : 0);
    }

    private void doRd(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);
        int addr = resolveTargetAddr(disp, flags);

        String devKey = resolveDeviceKey(addr);
        char[] data = rMgr.readDevice(devKey, 1);
        int value = (data != null && data.length > 0) ? data[0] & 0xFF : 0;

        rMgr.setRegister(0, value);
        System.out.printf("✔ RD → A ← Device[%s] = %02X\n", devKey, value);
    }

    private void doWd(char[] bytes) {
        boolean[] flags = decodeNixbpe(bytes);
        int disp = fetchDisp(bytes, flags[5]);
        int addr = resolveTargetAddr(disp, flags);

        String devKey = resolveDeviceKey(addr);
        int a = rMgr.getRegister(0);
        char[] data = {(char)(a & 0xFF)};

        rMgr.writeDevice(devKey, data, 1);
        System.out.printf("✔ WD → Device[%s] ← A = %02X\n", devKey, a & 0xFF);
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
        if(format4) {
            return ((bytes[1] & 0x0F) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } else {
            int disp = ((bytes[1] & 0x0F) << 8) | (bytes[2] & 0xFF);
            if((disp & 0x800) != 0) {
                disp |= 0xFFFFF000;
            }
            return disp;
        }
    }

    private int resolveTargetValue(int disp, boolean[] flags) {
        int addr = resolveTargetAddr(disp, flags);
        boolean n = flags[0], i = flags[1];

        if(n && i) {
            // indirect
            char[] ptr = rMgr.getMemory(addr, 3);
            int indirectAddr = toInt(ptr);
            char[] data = rMgr.getMemory(indirectAddr, 3);
            return toInt(data);
        } else if (!n && i) {
            // immediate
            return addr;
        } else {
            // simple
            char[] data = rMgr.getMemory(addr, 3);
            return toInt(data);
        }
    }

    private String resolveDeviceKey(int addr) {
        return String.format("D%02X", addr & 0xFF);
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
}
