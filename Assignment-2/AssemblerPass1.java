import java.util.*;
import java.io.*;

class myException extends Exception {
    String errorMessage = null;

    public myException(String message) {
        errorMessage = message;
    }

    @Override
    public String toString() {
        return errorMessage;
    }
}

public class AssemblerPass1 {

    ArrayList<String[]> literalTable = new ArrayList<>();
    ArrayList<String[]> intermediateCode = new ArrayList<>();
    ArrayList<String> pendingLiterals = new ArrayList<>();
    ArrayList<Integer> poolTable = new ArrayList<>();

    public int lc = 0;
    public int lineNo = 0;
    public int literalIndex = 0;
    public static boolean displayTables = true;

    String[][] MOT_Table = {
        // Imperative Statements 
        {"ADD",      "01",     "2",    "IS" }, {"SUB",      "02",     "2",    "IS" }, {"MUL",      "03",     "2",    "IS" },
        {"MOVER",    "04",     "2",    "IS" }, {"MOVEM",    "05",     "2",    "IS" }, {"COMP",     "06",     "2",    "IS" },
        {"BC",       "07",     "2",    "IS" }, {"DIV",      "08",     "2",    "IS" }, {"READ",     "09",     "2",    "IS" },
        {"PRINT",    "10",     "2",    "IS" }, {"MOV",      "89",     "2",    "IS" }, {"PUSH",     "50",     "1",    "IS" },
        {"POP",      "58",     "1",    "IS" }, {"JUMP",     "88",     "2",    "IS" },
        // Declarative Statements
        {"DC",       "01",      "1",    "DS" }, {"DS",       "02",      "n",    "DS" },
        // Assembler Directives
        {"ORG",      "-",      "-",   "AD" }, {"EQU",      "-",      "-",   "AD" }, {"LTORG",    "-",      "-",   "AD" },
        {"START",    "-",      "-",   "AD" }, {"END",      "-",      "-",   "AD" }, {"ENDS",     "-",      "-",   "AD" },
    };

    Map<String, String> registerMap = new HashMap<>();

    public AssemblerPass1() {
        registerMap.put("AREG", "(R, 1)");
        registerMap.put("BREG", "(R, 2)");
        registerMap.put("CREG", "(R, 3)");
        registerMap.put("DREG", "(R, 4)");
    }

    public void updateLiteralTable(int idx, int locCtr){
        literalTable.get(idx-1)[1] = String.valueOf(locCtr);
    }

    public void assignLiterals() {    
        for(int n = 0; n < literalTable.size(); n++) {
            if(!pendingLiterals.isEmpty() && pendingLiterals.get(0).equals(literalTable.get(n)[0])) {
               literalIndex = n;
               poolTable.add(literalIndex + 1);
               break;
            }
        }
        pendingLiterals.clear();
        for(int i = literalIndex; i < literalTable.size(); i++){
            lc++;
            literalTable.get(i)[1] = String.valueOf(lc);
        }
    }

    public boolean checkMnemonic(String token) {
        return Arrays.stream(MOT_Table).anyMatch(row -> row[0].equals(token.toUpperCase()));
    }

    public String getOpcode(String token) {
        String opcode = null;
        for (int i = 0; i < MOT_Table.length; i++) {
            if (token.toUpperCase().equals(MOT_Table[i][0])) {
                if (MOT_Table[i][3].equals("IS")) {
                    opcode = "(IS, " + MOT_Table[i][1] + ")";
                    lc += Integer.parseInt(MOT_Table[i][2]);
                    break;
                } else if (MOT_Table[i][3].equals("AD")) {
                    opcode = "(AD, " + (i + 1) + ")";
                    break;
                }
            }
        }
        return opcode;
    }

    public int findLiteralIndex(String token){
        for(int i = 0; i < literalTable.size(); i++){
            if(token.equals(literalTable.get(i)[0])){
               return i + 1;
            }
        }
        return -1;
    }

    public Boolean isLiteralToken(String token) {
        String strippedToken = token.substring(1, token.length()-1);
        return isNumeric(strippedToken) && token.equals("'" + strippedToken + "'");
    }

    public String getRegisterCode(String token) {
        return registerMap.get(token.toUpperCase());
    }

    public static boolean isNumeric(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void firstPass(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                parseLine(line.trim());
                if(!displayTables){
                  break;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void parseLine(String line) {
        if (line.isEmpty() || line.startsWith(";")) {
            return;
        }

        String[] tokens = line.split("\\s+");
        try{
            switch (tokens.length) {
                case 1:
                    processSingleToken(tokens[0]);
                    break;
                case 2:
                    processTwoTokens(tokens);
                    break;
                case 3:
                    processThreeTokens(tokens);
                    break;
            }
        }
        catch(Exception e) {
            displayTables = false;
            System.out.println("\n"+ e +"\n");
        }
    }

    void processSingleToken(String token) throws myException{
        String opcode = null;
        int previousLc = lc;
        if(checkMnemonic(token)){ // case 1 : Mnemonic
            if((lineNo == 1) && !token.toUpperCase().equals("START")){
                throw new myException("Error on line no-"+lineNo+": Program should start with 'START'.");
            } 
            
            if (token.toUpperCase().equals("START")) {
                opcode = getOpcode("START");
                lc = 00;
            } else if(token.toUpperCase().equals("LTORG")){
                opcode = getOpcode(token);
                assignLiterals();

            } else if (token.toUpperCase().equals("END")) {
                opcode = getOpcode("END");
                lc--;
                assignLiterals();
            }
            intermediateCode.add(new String[] { String.valueOf(previousLc), opcode });
        } else {
           throw new myException("Error on line no-"+lineNo+": invalid use of Mnemonic.");
        } 
    }

    void processTwoTokens(String[] tokens) throws myException {
        String opcode = null, operand1 = null;
        int previousLc = lc;
        
        if((lineNo == 1) && !tokens[0].toUpperCase().equals("START")){
            throw new myException("Error on line no-"+lineNo+": Program should start with 'START'.");
        }
        if(checkMnemonic(tokens[0])) {
            opcode = getOpcode(tokens[0]);
            if(tokens[0].toUpperCase().equals("START") && isNumeric(tokens[1])) {
                lc = Integer.parseInt(tokens[1]);
            }
            if(tokens[0].toUpperCase().equals("ORG") && isNumeric(tokens[1])) {
                operand1 = "(C, " + tokens[1] + ")";
                lc = Integer.parseInt(tokens[1]);
            } else if(getRegisterCode(tokens[1]) != null) {
                operand1 = getRegisterCode(tokens[1]);
            } else if(isNumeric(tokens[1])) {
                operand1 = "(C, " + tokens[1] + ")";
            } else {
                throw new myException("Error on line no-"+lineNo+": invalid use of characters");
            }
        } else {
            throw new myException("Error on line no-"+lineNo+": invalid use of Mnemonic.");
        }

        intermediateCode.add(new String[] { String.valueOf(previousLc), opcode, operand1 = (operand1 == null)? "" : operand1});
    }

    void processThreeTokens(String[] tokens) throws myException {
        String opcode = null, operand1 = null, operand2 = null;
        int previousLc = lc;

        if (checkMnemonic(tokens[0])) { 
            if((lineNo == 1) && !tokens[0].toUpperCase().equals("START")){
                throw new myException("Error on line no-"+lineNo+": Program should start with 'START'.");
            }
            opcode = getOpcode(tokens[0]);

            if(getRegisterCode(tokens[1]) != null){
                operand1 = getRegisterCode(tokens[1]);

                if (getRegisterCode(tokens[2]) != null) {
                    operand2 = getRegisterCode(tokens[2]);
                } else if (isNumeric(tokens[2])) {
                    operand2 = "(C, " + Integer.parseInt(tokens[2]) + ")";
                } else if(isLiteralToken(tokens[2])){
                    literalTable.add(new String[] {tokens[2], "-"});
                    pendingLiterals.add(tokens[2]);
                    operand2 = "(L, " + findLiteralIndex(tokens[2]) + ")";
                } else {
                    throw new myException("Error on line no-"+lineNo+": use of invalid symbol.");
                }
            } else {
                throw new myException("Error on line no-"+lineNo+": use of invalid register.");
            }
        } else {
            throw new myException("Error on line no-"+lineNo+": use of invalid mnemonic.");
        }
        intermediateCode.add(new String[] { String.valueOf(previousLc), opcode, operand1, operand2 = (operand2 == null)? "" : operand2 });
    }

    public void printLiteralTable() {
        System.out.println("\n___________________________________");
        System.out.println("\nLiteral Table:\n");
        for (int i = 0; i < literalTable.size(); i++) {
            System.out.println((i + 1)+"." + "\t" + literalTable.get(i)[0] + "\t" + literalTable.get(i)[1]);
        }
        System.out.println("\n___________________________________");
    }

    public void printPoolTable() {
        System.out.println("\nPool Table:\n");
        for (int i = 0; i < poolTable.size(); i++) {
            System.out.println((i + 1)+"." + "\t" + poolTable.get(i));
        }
        System.out.println("\n___________________________________");
    }

    public void printIntermediateCode() {
        System.out.println("\nIntermediate Code:\n");
        for (String[] row : intermediateCode) {
            System.out.println(String.join("\t", row));
        }
        System.out.println("\n___________________________________");
    }

    public static void main(String[] args) {
        AssemblerPass1 assembler = new AssemblerPass1();
        assembler.firstPass("source.asm");
        if(displayTables){
            assembler.printLiteralTable();
            assembler.printPoolTable();
            assembler.printIntermediateCode();
        }
    }
}
