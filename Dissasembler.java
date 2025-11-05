import java.util.HashMap;
import java.util.Map;

public class Dissasembler {

    // Maps for opcode lookup by bit width
    private static final Map<Integer, String> OPCODE_11 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_10 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_8  = new HashMap<>();
    private static final Map<Integer, String> OPCODE_6  = new HashMap<>();

    public static void initDicts() {
        // ===== R-TYPE (11-bit opcodes) =====
        OPCODE_11.put(0b10001011000, "ADD");
        OPCODE_11.put(0b10001010000, "AND");
        OPCODE_11.put(0b10101010000, "ORR");
        OPCODE_11.put(0b11001010000, "EOR");
        OPCODE_11.put(0b11001011000, "SUB");
        OPCODE_11.put(0b11101011000, "SUBS");
        OPCODE_11.put(0b10011011000, "MUL");
        OPCODE_11.put(0b11010011011, "LSL");
        OPCODE_11.put(0b11010011010, "LSR");
        OPCODE_11.put(0b11111000010, "LDUR");
        OPCODE_11.put(0b11111000000, "STUR");
        OPCODE_11.put(0b11010110000, "BR");

        // Custom emulator opcodes
        OPCODE_11.put(0b11111111101, "PRNT");
        OPCODE_11.put(0b11111111100, "PRNL");
        OPCODE_11.put(0b11111111110, "DUMP");
        OPCODE_11.put(0b11111111111, "HALT");

        // ===== I-TYPE (10-bit opcodes) =====
        OPCODE_10.put(0b1001000100, "ADDI");
        OPCODE_10.put(0b1001001000, "ANDI");
        OPCODE_10.put(0b1011001000, "ORRI");
        OPCODE_10.put(0b1101001000, "EORI");
        OPCODE_10.put(0b1101000100, "SUBI");
        OPCODE_10.put(0b1111000100, "SUBIS");

        // ===== B-TYPE (6-bit opcodes) =====
        OPCODE_6.put(0b000101, "B");
        OPCODE_6.put(0b100101, "BL");

        // ===== CB-TYPE (8-bit opcodes) =====
        OPCODE_8.put(0b10110100, "CBZ");
        OPCODE_8.put(0b10110101, "CBNZ");
    }



    public static void decode(int inst) {
        int opcode11 = (inst >> 21) & 0x7FF; //11 1 bits
        int opcode10 = (inst >> 22) & 0x3FF; //10 1 bits
        int opcode8  = (inst >> 24) & 0xFF; //8 1 bits
        int opcode6  = (inst >> 26) & 0x3F; //6 1 bits

        // Register and immediate fields
        int rm = (inst >> 16) & 0x1F;
        int shamt = (inst >> 10) & 0x3F;
        int rn = (inst >> 5) & 0x1F;
        int rd = inst & 0x1F;
        int imm12 = (inst >> 10) & 0xFFF;
        int imm26 = inst & 0x3FFFFFF;
        int condImm19 = (inst >> 5) & 0x7FFFF;
        int rt = inst & 0x1F;


        // Lookup phase
        if (OPCODE_11.containsKey(opcode11))
            printRType(OPCODE_11.get(opcode11), rd, rn, rm, shamt);
        else if (OPCODE_10.containsKey(opcode10))
            printIType(OPCODE_10.get(opcode10), rd, rn, imm12);
        else if (OPCODE_6.containsKey(opcode6))
            printBType(OPCODE_6.get(opcode6), imm26);
        else if (OPCODE_8.containsKey(opcode8))
            printCBType(OPCODE_8.get(opcode8), rt, condImm19);
        else
            System.out.printf("Unknown instruction: 0x%08X\n", inst);
    }


    private static void printRType(String op, int rd, int rn, int rm, int shamt) {
        switch (op) {
            case "LSL": case "LSR":
                System.out.printf("%s X%d, X%d, #%d\n", op, rd, rn, shamt);
                break;
            case "BR":
                System.out.printf("BR X%d\n", rn);
                break;
            case "PRNT": case "PRNL": case "DUMP": case "HALT":
                System.out.printf("%s\n", op);
                break;
            default:
                System.out.printf("%s X%d, X%d, X%d\n", op, rd, rn, rm);
        }
    }

    private static void printIType(String op, int rd, int rn, int imm) {
        System.out.printf("%s X%d, X%d, #%d\n", op, rd, rn, imm);
    }

    private static void printBType(String op, int imm) {
        System.out.printf("%s #%d\n", op, imm);
    }

    private static void printCBType(String op, int rt, int imm) {
        System.out.printf("%s X%d, #%d\n", op, rt, imm);
    }

    public static void main(String[] args) {
        initDicts();
        int inst = 0b10001011000000110000000001000001;
        decode(inst);
    }



}