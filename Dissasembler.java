import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Dissasembler {

    private static final Map<Integer, String> OPCODE_11 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_10 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_8  = new HashMap<>();
    private static final Map<Integer, String> OPCODE_6  = new HashMap<>();
    private static final Map<Integer,String> COND = new HashMap<>();
    private static final Map<Integer, String> labels = new HashMap<>();

    public static void initDicts() {
        // R-TYPE
        OPCODE_11.put(0b10001011000, "ADD");
        OPCODE_11.put(0b10101011000, "ADDS");
        OPCODE_11.put(0b10001010000, "AND");
        OPCODE_11.put(0b11101010000, "ANDS");
        OPCODE_11.put(0b11001010000, "EOR");
        OPCODE_11.put(0b10101010000, "ORR");
        OPCODE_11.put(0b11001011000, "SUB");
        OPCODE_11.put(0b11101011000, "SUBS");
        OPCODE_11.put(0b10011011000, "MUL");
        OPCODE_11.put(0b11010011011, "LSL");
        OPCODE_11.put(0b11010011010, "LSR");
        OPCODE_11.put(0b11010110000, "BR");
        OPCODE_11.put(0b11111000010, "LDUR");
        OPCODE_11.put(0b11111000000, "STUR");
        OPCODE_11.put(0b11111111101, "PRNT");
        OPCODE_11.put(0b11111111100, "PRNL");
        OPCODE_11.put(0b11111111110, "DUMP");
        OPCODE_11.put(0b11111111111, "HALT");

        // I-TYPE
        OPCODE_10.put(0b1001000100, "ADDI");
        OPCODE_10.put(0b1001001000, "ANDI");
        OPCODE_10.put(0b1011001000, "ORRI");
        OPCODE_10.put(0b1101001000, "EORI");
        OPCODE_10.put(0b1101000100, "SUBI");
        OPCODE_10.put(0b1111000100, "SUBIS");

        // B-TYPE
        OPCODE_6.put(0b000101, "B");
        OPCODE_6.put(0b100101, "BL");

        // CB-TYPE
        OPCODE_8.put(0b10110100, "CBZ");
        OPCODE_8.put(0b10110101, "CBNZ");
        OPCODE_8.put(0b01010100, "B.cond");

        // Conditions
        COND.put(0b0000, "EQ"); COND.put(0b0001, "NE");
        COND.put(0b0010, "HS"); COND.put(0b0011, "LO");
        COND.put(0b0100, "MI"); COND.put(0b0101, "PL");
        COND.put(0b0110, "VS"); COND.put(0b0111, "VC");
        COND.put(0b1000, "HI"); COND.put(0b1001, "LS");
        COND.put(0b1010, "GE"); COND.put(0b1011, "LT");
        COND.put(0b1100, "GT"); COND.put(0b1101, "LE");
    }

    private static int signExtend(int value, int bits) {
        int shift = 32 - bits;
        return (value << shift) >> shift;
    }

    private static void decode(int inst, int index) {
        int opcode11 = (inst >> 21) & 0x7FF;
        int opcode10 = (inst >> 22) & 0x3FF;
        int opcode8  = (inst >> 24) & 0xFF;
        int opcode6  = (inst >> 26) & 0x3F;

        int rm = (inst >> 16) & 0x1F;
        int shamt = (inst >> 10) & 0x3F;
        int rn = (inst >> 5) & 0x1F;
        int rd = inst & 0x1F;
        int imm12 = (inst >> 10) & 0xFFF;
        int imm26 = inst & 0x3FFFFFF;
        int condImm19 = (inst >> 5) & 0x7FFFF;
        int rt = inst & 0x1F;
        int cond = inst & 0xF;

        if (labels.containsKey(index)) System.out.println(labels.get(index) + ":");

        if (OPCODE_11.containsKey(opcode11)) {
            String op = OPCODE_11.get(opcode11);
            if (op.equals("LDUR") || op.equals("STUR")) {
                printDType(op, rd, rn, inst);
            } else {
                printRType(op, rd, rn, rm, shamt);
            }
        }else if (OPCODE_10.containsKey(opcode10)) {
            printIType(OPCODE_10.get(opcode10), rd, rn, imm12);
        }
        else if (OPCODE_6.containsKey(opcode6)){
            printBType(OPCODE_6.get(opcode6), imm26, index);
        } 
        else if (OPCODE_8.containsKey(opcode8)) {
            if (opcode8 == 0b01010100){
                printBCond(index, cond, condImm19);
            }
            else {
                printCBType(OPCODE_8.get(opcode8), rt, condImm19, index);
            }
        }
        else {
            System.out.printf("Unknown instruction: 0x%08X\n", inst);
        }
    }

    private static void printRType(String op, int rd, int rn, int rm, int shamt) {
        switch(op){
            case "LSL": case "LSR":
                System.out.printf("%s X%d, X%d, #%d\n", op, rd, rn, shamt); break;
            case "BR": System.out.printf("BR X%d\n", rn); break;
            case "PRNT": case "PRNL": case "DUMP": case "HALT": System.out.println(op); break;
            default: System.out.printf("%s X%d, X%d, X%d\n", op, rd, rn, rm);
        }
    }

    private static void printIType(String op, int rd, int rn, int imm) {
        System.out.printf("%s X%d, X%d, #%d\n", op, rd, rn, imm);
    }

    private static void printCBType(String op, int rt, int imm19, int currentIndex) {
        int target = currentIndex + signExtend(imm19, 19); // offset in instructions
        String label = labels.getOrDefault(target, "#" + signExtend(imm19, 19));
        System.out.printf("%s X%d, %s\n", op, rt, label);
    }

    private static void printBCond(int currentIndex, int cond, int imm19) {
        String condition = COND.getOrDefault(cond & 0xF, "??");
        String op = "B." + condition;
        int target = currentIndex + signExtend(imm19, 19);
        String label = labels.getOrDefault(target, "#" + signExtend(imm19, 19));
        System.out.printf("%s %s\n", op, label);
    }

    private static void printBType(String op, int imm26, int currentIndex) {
        int target = currentIndex + signExtend(imm26, 26);
        String label = labels.getOrDefault(target, "#" + signExtend(imm26, 26));
        System.out.printf("%s %s\n", op, label);
    }

    private static void printDType(String op, int rd, int rn, int inst) {
        // D-type immediate is bits [20:12] (9 bits)
        int imm9 = (inst >> 12) & 0x1FF;
        // sign-extend it as 9 bits (if you want signed immediates)
        int imm = signExtend(imm9, 9);
        System.out.printf("%s X%d, [X%d, #%d]\n", op, rd, rn, imm);
    }

    public static void main(String[] args) throws Exception {
        initDicts();
        if(args.length==0){System.err.println("Usage: java Dissasembler <file>"); return;}

        byte[] data = Files.readAllBytes(Paths.get(args[0]));
        if(data.length%4 !=0){System.err.println("Error: input length not multiple of 4"); return;}
        int instructionCount = data.length / 4;

        // FIRST PASS: collect branch targets
        for(int i=0;i<instructionCount;i++){
            int inst = ((data[i*4]&0xFF)<<24)|((data[i*4+1]&0xFF)<<16)|((data[i*4+2]&0xFF)<<8)|(data[i*4+3]&0xFF);
            int opcode6 = (inst >>26) &0x3F;
            int opcode8 = (inst >>24) &0xFF;
            int imm26 = inst &0x3FFFFFF;
            int condImm19 = (inst >>5) &0x7FFFF;
            int target=-1;

            if(OPCODE_6.containsKey(opcode6)) target = i + signExtend(imm26,26);
            else if(opcode8==0b01010100 || opcode8==0b10110100 || opcode8==0b10110101) target = i + signExtend(condImm19,19);

            if(target>=0 && target<instructionCount && !labels.containsKey(target)) labels.put(target,"label_"+labels.size());
        }

        // SECOND PASS: decode & print
        for(int i=0;i<instructionCount;i++){
            int inst = ((data[i*4]&0xFF)<<24)|((data[i*4+1]&0xFF)<<16)|((data[i*4+2]&0xFF)<<8)|(data[i*4+3]&0xFF);
            decode(inst,i);
        }
    }
}