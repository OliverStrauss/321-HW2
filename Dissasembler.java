import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Dissasembler {

    // Maps for opcode lookup by bit width
    private static final Map<Integer, String> OPCODE_11 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_10 = new HashMap<>();
    private static final Map<Integer, String> OPCODE_8  = new HashMap<>();
    private static final Map<Integer, String> OPCODE_6  = new HashMap<>();
    private static final Map<Integer,String> COND = new HashMap<>(); 
    private static final Map<Integer, String> labels = new HashMap<>();

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
        OPCODE_8.put( 0b01010100 , "B.cond");

        COND.put(0b0000, "EQ");
        COND.put(0b0001, "NE");
        COND.put(0b0010, "HS");
        COND.put(0b0011, "LO");
        COND.put(0b0100, "MI");
        COND.put(0b0101, "PL");
        COND.put(0b0110, "VS");
        COND.put(0b0111, "VC");
        COND.put(0b1000, "HI");
        COND.put(0b1001, "LS");
        COND.put(0b1010, "GE");
        COND.put(0b1011, "LT");
        COND.put(0b1100, "GT");
        COND.put(0b1101, "LE");
    }



    public static void decode(int inst,int index) {
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

        if (labels.containsKey(index)) {
            System.out.println(labels.get(index) + ":");
        }
        


        // Lookup phase
        if (OPCODE_11.containsKey(opcode11))
            printRType(OPCODE_11.get(opcode11), rd, rn, rm, shamt);
        else if (OPCODE_10.containsKey(opcode10))
            printIType(OPCODE_10.get(opcode10), rd, rn, imm12);
        else if (OPCODE_6.containsKey(opcode6))
            printBType(OPCODE_6.get(opcode6), imm26, index);
        else if (OPCODE_8.containsKey(opcode8))
             if (opcode8 == 0b01010100) { // B.cond
                 printBCond(index, rt, condImm19);
            } else {
                printCBType(OPCODE_8.get(opcode8), rt, condImm19);
            }
        
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


    private static void printCBType(String op, int rt, int imm) {
        
        System.out.printf("%s X%d, #%d\n", op, rt, imm);
    }

   private static void printBCond(int currentIndex, int rt, int imm19) {
        String condition = COND.get(rt);
        String condOutput = "B." + condition;

        int offset = imm19;
        if ((offset & (1 << 18)) != 0) offset |= ~0x7FFFF; // sign extend

        int target = currentIndex + offset;
        String label = labels.getOrDefault(target, "#" + offset);

        System.out.printf("%s %s\n", condOutput, label);
    }

    private static void printBType(String op, int imm26, int currentIndex) {
        int offset = imm26;
        if ((offset & (1 << 25)) != 0) offset |= ~0x3FFFFFF;  // sign extend

        int target = currentIndex + offset;

        String label = labels.getOrDefault(target, "#" + offset);

        System.out.printf("%s %s\n", op, label);
    }

public static void main(String[] args) {
    initDicts();
       
    String fileName = args[0];
    byte[] data;

    try {
        data = Files.readAllBytes(Paths.get(fileName));
    } catch (Exception e) {
        System.err.println("Error reading file: " + e.getMessage());
        return;
    }
    int instructionCount = data.length / 4;

    for (int i = 0; i < instructionCount; i++) {
        int b0 = data[i*4] & 0xFF;
        int b1 = data[i*4 + 1] & 0xFF;
        int b2 = data[i*4 + 2] & 0xFF;
        int b3 = data[i*4 + 3] & 0xFF;
        int inst = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;

        int opcode6  = (inst >> 26) & 0x3F;
        int opcode8  = (inst >> 24) & 0xFF;

        int imm26 = inst & 0x3FFFFFF;
        int condImm19 = (inst >> 5) & 0x7FFFF;

        int target = -1;

        if (OPCODE_6.containsKey(opcode6)) { // B / BL
        
            int offset = imm26;
            if ((offset & (1 << 25)) != 0) {  // check sign bit
                offset |= ~0x3FFFFFF;         // sign-extend
            }
            target = i + offset;

        } else if (opcode8 == 0b01010100) { // B.cond
            // For B.cond (19-bit signed immediate)
            int offset = condImm19;
            if ((offset & (1 << 18)) != 0) {  // check sign bit
                offset |= ~0x7FFFF;           // sign-extend
            }
            target = i + offset;           // target instruction index
        }
    
        if (target >= 0 && !labels.containsKey(target)) {
            labels.put(target, "label_" + labels.size());
        }
    }

    for (int i = 0; i < instructionCount; i++) {

        int b0 = data[i*4]     & 0xFF;
        int b1 = data[i*4 + 1] & 0xFF;
        int b2 = data[i*4 + 2] & 0xFF;
        int b3 = data[i*4 + 3] & 0xFF;

        // Big-endian 32-bit instruction
        int inst = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        

  

        decode(inst,i);
    }
  
}



}