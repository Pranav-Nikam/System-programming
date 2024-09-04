import java.util.*;
import java.io.*;

class customException extends Exception{
    String exception = null;
    public customException(String str){
        exception = str;
    }

    @Override
    public String toString() {
        return exception;
    }
}

class Pass1{

    ArrayList<String[]> symbolTable = new ArrayList<>();
    ArrayList<String[]> intermediateTable = new ArrayList<>();
    ArrayList<String> symbolList = new ArrayList<>();

    public int locationCounter = 0;
    public int lineNumber = 0;
    public static boolean showTable = true;

    String[][] motTable = {
        // Imperative Statements 
        {"ADD",    "01",   "2",  "IS" }, {"SUB",   "02",  "2",  "IS" }, {"MUL",   "03",   "2", "IS" },
        {"MOVER",  "04",   "2",  "IS" }, {"MOVEM", "05",  "2",  "IS" }, {"COMP",  "06",  "2",  "IS" },
        {"BC",     "07",   "2",  "IS" }, {"DIV",   "08",  "2",  "IS" }, {"READ",  "09",  "2",  "IS" },
        {"PRINT",  "10",   "2",  "IS" }, {"MOV",   "89",  "2",  "IS" }, {"PUSH",  "50",  "1",  "IS" },
        {"POP",    "58",   "1",  "IS" }, {"JUMP",  "88",  "2",  "IS" },
        // Declarative Statements
        {"DC",     "01",   "1",  "DS" }, {"DS",    "02",  "n",  "DS" },
        // Assembler Directives
        {"ORG",    "-",    "-",  "AD" }, {"EQU",   "-",   "-",  "AD" }, {"LTORG",   "-",     "-",   "AD" },
        {"START",  "-",    "-",  "AD" }, {"END",   "-",   "-",  "AD" }, {"ENDS",    "-",     "-",   "AD" },
    };

    Map<String, String> Registers = new HashMap<>();

    public Pass1() {
        Registers.put("AREG", "(R, 01)");
        Registers.put("BREG", "(R, 02)");
        Registers.put("CREG", "(R, 03)");
        Registers.put("DREG", "(R, 04)");
    }

    public void addSymbolTable(String symbol, int LC) {
        if (symbol.endsWith(":")) {
            symbol = symbol.substring(0, symbol.length() - 1);
        }
        symbolTable.add(new String[] { symbol.toUpperCase(), String.valueOf(LC) });
    }

    public void modifySymbolTable(int index, int LC)
    {
        symbolTable.get(index-1)[1] = String.valueOf(LC);
    }

    public boolean alreadyExists(String symbol) {
        boolean isExists = false;
        for (int k = 0; k < symbolTable.size(); k++) {
            if (symbol.toUpperCase().equals(symbolTable.get(k)[0])) {
                isExists = true;
                break;
            }
        }
        return isExists;
    }

    public boolean isLable(String var) {
        boolean result = false;
        for (int i = 0; i < motTable.length; i++) {
            if (!var.toUpperCase().equals(motTable[i][0]) && var.endsWith(":")) {
                result = true;
            }
            if (var.toUpperCase().equals(motTable[i][0])) {
                result = false;
            }
        }
        return result;
    }

    public boolean isMnemonic(String var) {
        return Arrays.stream(motTable).anyMatch(row -> row[0].equals(var.toUpperCase()));
    }

    public int getSymbolIndex(String symbol) {
        for (int i = 0; i < symbolTable.size(); i++) {
            if (symbol.toUpperCase().equals(symbolTable.get(i)[0])) {
                return i + 1;
            }
        }
        return -1;
    }

    public String getOpcode(String var) {
        String OP_ST = null;
        for (int i = 0; i < motTable.length; i++) {
            if (var.toUpperCase().equals(motTable[i][0])) {
                if (motTable[i][3].equals("IS")) {
                    OP_ST = "(IS, " + motTable[i][1] + ")";
                    locationCounter += Integer.parseInt(motTable[i][2]);
                    break;
                } else if (motTable[i][3].equals("AD")) {
                    OP_ST = "(AD, " + (i + 1) + ")";
                    break;
                }
            }
        }
        return OP_ST;
    }

    public String isRegister(String var) {
        return Registers.get(var.toUpperCase());
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void pass1(String fileName) {
        try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                lineNumber++;
                processLine(line.trim());
                if(!showTable){
                  break;
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void processLine(String line) {
        if (line.isEmpty() || line.startsWith(";")) {
            return;
        }

        String[] words = line.split("\\s+");
        try{
            switch (words.length) {
                case 1:
                    processSingleToken(words[0]);
                    break;
                case 2:
                    processTwowords(words);
                    break;
                case 3:
                    processThreewords(words);
                    break;
                case 4:
                    processFourwords(words);
                    break;
            }
        }
        catch(Exception e)
        {
            showTable = false;
            System.out.println("\n"+ e +"\n");
        }
    }

    void processSingleToken(String word) throws customException{
        String opcode = null;
        if(isMnemonic(word)){ // case 1 : Mnemonic
            if((lineNumber == 1) && !word.toUpperCase().equals("START")){
                throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
            } 
            if (word.toUpperCase().equals("START")) {
                opcode = getOpcode("START");
                locationCounter = 0;
            } else if (word.toUpperCase().equals("END")) {
                if(symbolList.size() > 0)
                {
                    throw new customException("Error: symbols are not declared:" + symbolList);
                }
                opcode = getOpcode("END");
            }
            intermediateTable.add(new String[] { String.valueOf(locationCounter), opcode });
        }else{
           throw new customException("Error on line no-"+lineNumber+": invalid use of Mnemonic.");
        } 
    }

    void processTwowords(String[] words) throws customException{
        String opcode = null, label = null, operand1 = null;
        int prevLC = locationCounter;

        if (isLable(words[0]) && isMnemonic(words[1])) { // case-1 label, Mnemonic

            if((lineNumber == 1) && !words[1].toUpperCase().equals("START")){
                throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
            }

            label = words[0].substring(0, words[0].length() - 1);
            if (!alreadyExists(label)) {
                addSymbolTable(label, locationCounter);
            }else{
                throw new customException("Error on line no-"+lineNumber+": Label already exists.");
            }
            opcode = getOpcode(words[1]);
        } else {

            if((lineNumber == 1) && !words[0].toUpperCase().equals("START")){
                throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
            }
            if(isMnemonic(words[0])) {// case-2 Mnemonic, Operand
                opcode = getOpcode(words[0]);

                if (isRegister(words[1]) != null) {
                    operand1 = isRegister(words[1]);
                } else if (isInteger(words[1])) {
                    if(words[0].toUpperCase().equals("START"))
                    {
                        locationCounter += Integer.parseInt(words[1]);
                    }
                    operand1 = "(C, " + Integer.parseInt(words[1]) + ")";
                } else {
                    if (!alreadyExists(words[1])) {
                        addSymbolTable(words[1], locationCounter);
                        symbolList.add(words[1]);
                    }
                    operand1 = "(S, " + getSymbolIndex(words[1]) + ")";
                }
            }
            else{
                throw new customException("Error on line no-"+lineNumber+": invalid use of Mnemonic.");
            }
        }
        intermediateTable.add(new String[] { String.valueOf(prevLC), opcode, operand1 = (operand1 == null)? "" : operand1});
    }

    void processThreewords(String[] words) throws customException{
        String opcode = null, label = null, operand1 = null, operand2 = null;
        int prevLC = locationCounter;

        if (isLable(words[0])) { // case-1 : label mnemonic operand1
            label = words[0].substring(0, words[0].length() - 1);

            if (!alreadyExists(label)) {
                addSymbolTable(label, locationCounter);
            }else{
                throw new customException("Error on line no-"+lineNumber+": Label already exists.");
            }

            if(isMnemonic(words[1]))
            {
                if((lineNumber == 1) && !words[1].toUpperCase().equals("START")){
                    throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
                }
                opcode = getOpcode(words[1]);

                if (isRegister(words[2]) != null) {
                    operand1 = words[2];
                } else if (isInteger(words[2])) {
                    operand1 = "(C, " + Integer.parseInt(words[2]) + ")";
                } else {
                    if (!alreadyExists(words[2])) {
                        addSymbolTable(words[2], locationCounter);
                        symbolList.add(words[2]);
                    }
                    operand1 = "(S, " + getSymbolIndex(words[2]) + ")";
                }
            }else{
                throw new customException("Error on line no-"+lineNumber+": invalid use mnemonic.");
            }  

        } else if (!isLable(words[0]) && isMnemonic(words[1]) && isInteger(words[2])) { // case-2  symbol mnemonic constant

            if((lineNumber == 1) && !words[1].toUpperCase().equals("START")){
                throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
            }

            if (alreadyExists(words[0])) {
                modifySymbolTable(getSymbolIndex(words[0]), locationCounter);
                symbolList.remove(words[0]);
            }else{
               addSymbolTable(words[0], locationCounter);
               symbolList.add(words[0]);
            }
            if (words[1].toUpperCase().equals("DC")) {
                opcode = "(DL, 01)";
                operand1 = "(C, " + Integer.parseInt(words[2]) + ")";
                locationCounter += 1;
            }
            if (words[1].toUpperCase().equals("DS")) {
                opcode = "(DL, 02)";
                operand1 = "(C, " + Integer.parseInt(words[2]) + ")";
                locationCounter += Integer.parseInt(words[2]);
            }

        } else {
            if (isMnemonic(words[0])) { // case 3  mnemonic operand1 operand2
                if((lineNumber == 1) && !words[0].toUpperCase().equals("START")){
                    throw new customException("Error on line no-"+lineNumber+": Program should start with 'START'.");
                }
                opcode = getOpcode(words[0]);
                if(isRegister(words[1]) != null){
                    operand1 = isRegister(words[1]);

                    if (isRegister(words[2]) != null) {
                        operand2 = isRegister(words[2]);
                    } else if (isInteger(words[2])) {
                        operand2 = "(C, " + Integer.parseInt(words[2]) + ")";
                    } else if (!isInteger(words[2])) {
                        if (!alreadyExists(words[2])) {
                            addSymbolTable(words[2], locationCounter);
                            symbolList.add(words[2]);
                        }
                        operand2 = "(S, " + getSymbolIndex(words[2]) + ")";
                    }
                    else{
                        throw new customException("Error on line no-"+lineNumber+": found invalid symbol-" + words[2]);
                    }
                }
                else{
                    throw new customException("Error on line no-"+lineNumber+": use of invalid register.");
                }
            }else{
                throw new customException("Error on line no-"+lineNumber+": use of invalid mnemonic.");
            }
        }
        intermediateTable.add(new String[] { String.valueOf(prevLC), opcode, operand1, operand2 = (operand2 == null)? "" : operand2 });
    }

    void processFourwords(String[] words)throws customException 
    {
        String opcode = null, label = null, operand1 = null, operand2 = null;
        int prevLC = locationCounter;

        if (isLable(words[0])) { // case 1: label mnemonic operand1 operand2

            label = words[0].substring(0, words[0].length() - 1);
            if (!alreadyExists(label)) {
                addSymbolTable(label, locationCounter);
            }
            else{
                throw new customException("Error on line no-"+lineNumber+": Label already exists.");
            } 

            if(isMnemonic(words[1])){
                opcode = getOpcode(words[1]);

                if(isRegister(words[2]) != null)
                {
                    operand1 = isRegister(words[2]);
                    if (isRegister(words[3]) != null) {
                        operand2 = isRegister(words[3]);
                    } else if (isInteger(words[3])) {
                        operand2 = "(C, " + Integer.parseInt(words[3]) + ")";
                    } else if (!isInteger(words[3])) {
                        if(!alreadyExists(words[3]))
                        {
                            addSymbolTable(words[3], locationCounter);
                            symbolList.add(words[3]);
                        }
                        operand2 = "(S, " + getSymbolIndex(words[3]) + ")";
                    }
                    else{
                        throw new customException("Error on line no-"+lineNumber+": found invalid symbol-" + words[3]);
                    }
                }else{
                    throw new customException("Error on line no-"+lineNumber+": use of invalid register.");
                }
            }
            else{
                throw new customException("Error on line no-"+lineNumber+": use of invalid mnemonic.");
            }
        }
        else{
            throw new customException("Error on line no-"+lineNumber+": use of invalid Label.");
        } 
        intermediateTable.add(new String[] { String.valueOf(prevLC), opcode, operand1, operand2 });
    }

    public void printSymbolTable() {
        System.out.println("\n__________________________________");
        System.out.println("\nSymbol Table:\n");
        for (int i = 0; i < symbolTable.size(); i++) {
            System.out.println((i + 1) + "\t" + symbolTable.get(i)[0] + "\t" + symbolTable.get(i)[1]);
        }
        System.out.println("__________________________________");
    }

    public void printIntermediateTable() {
        System.out.println("\nIntermediate Table:\n");
        for (String[] row : intermediateTable) {
            System.out.println(String.join("\t", row));
        }
        System.out.println("__________________________________");
    }

}

class Pass2 extends Pass1 {

    public void pass2() {
        System.out.println("\nMachine Code:\n");
        for (String[] row : intermediateTable) {
            String location = row[0];
            String opcode = row[1];
            String operand1 = row.length > 2 ? row[2] : "";
            String operand2 = row.length > 3 ? row[3] : "";

            if (opcode.equals("(AD, 20)")) {
                continue;
            }

            if (opcode != null) {
                if(opcode.equals("(AD, 21)") || opcode.equals("(DL, 01)")){
                    opcode = "  ";
                }
                else{
                    opcode = opcode.equals("(DL, 02)") ? "(DL, 02)" : opcode.replaceAll("[^0-9]", "");
                }  
            }
            if (operand1 != null) {
                operand1 = opcode.equals("(DL, 02)") || opcode.equals("(AD, 20)") ? "  " : processOperand(operand1);
            }
            if (operand2 != null) {
                operand2 = opcode.equals("(DL, 02)") ? "  " : processOperand(operand2);
            }
            if(opcode.startsWith("(DL, 02)")){
                opcode = "--";
            }
            

            String machineCode = (opcode == null ? "" : opcode) + 
                                 (operand1.isEmpty() ? "" : "  " + operand1) + 
                                 (operand2.isEmpty() ? "" : "  " + operand2);
                                 
            System.out.println(location + "  " + machineCode);
        }
        System.out.println("__________________________________");
    }

    private String processOperand(String operand) {
        if (operand.startsWith("(S,")) {
            int symbolIndex = Integer.parseInt(operand.replaceAll("[^0-9]", "")) - 1;
            String[] symbolEntry = symbolTable.get(symbolIndex);
            return symbolEntry[1];
        }
        return operand.replaceAll("[^0-9]", "");
    }
}

public class Assignment_3 extends Pass2 {
    public static void main(String[] args) {
        Assignment_3 Assembler = new Assignment_3();
        Assembler.pass1("source.asm");
        if (showTable) {
            Assembler.printSymbolTable();
            Assembler.printIntermediateTable();
            Assembler.pass2();
        }
    }
}
