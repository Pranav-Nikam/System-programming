import java.util.*;
import java.io.*;

class MacroPass 
{
    ArrayList<String> MNT = new ArrayList<>();
    ArrayList<String> MDT = new ArrayList<>();
    ArrayList<String> PNTAB = new ArrayList<>();
    ArrayList<String> EVNTAB = new ArrayList<>();
    ArrayList<String> SSNTAB = new ArrayList<>();
    ArrayList<String> KPDTAB = new ArrayList<>();
    ArrayList<Integer> SSTAB =  new ArrayList<>();
    HashMap<String, String> APTAB = new HashMap<>();
    
    ArrayList<String> trackSSN = new ArrayList<>();
    ArrayList<String> parameters = new ArrayList<>();
    String trackParameters = null;


    // --------------------------------------   PASS - I -------------------------------------------------------
    void pass1(String fileName) throws IOException 
    {
        String macroName = null;
        Integer PP = 0, KP = 0, EV = 0, tempEV=0;
        Integer MDTP = 1, KPDTP = 0, SSTP = 1;
        boolean flag = false;

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine().trim()) != null) {
                
                if(line.isEmpty()){
                    continue;
                }

                String[] words = line.split("\\s+");

                if (words.length == 1 && words[0].equalsIgnoreCase("MACRO")) {
                    flag = true;
                    line = br.readLine();
                    words = line.split("\\s+");
                    macroName = words[0];
                    trackParameters = macroName;

                    if (words.length <= 1) {
                        MNT.add(macroName + "\t" + PP + "\t" + KP + "\t" + EV + "\t" + MDTP + "\t"+ (KP == 0 ? KPDTP : (KPDTP + 1)) + "\t" + SSTP);
                        continue;
                    }
                    for (int i = 1; i < words.length; i++) {
                        words[i] = words[i].replaceAll("[&,]", "");
                        if (words[i].contains("=")) {
                            String param_value[] = words[i].split("=");
                            KP++;
                            PNTAB.add(param_value[0]);
                            KPDTAB.add(String.join("\t",param_value));
                            trackParameters += "\t" + param_value[0];
                        } else {
                            PP++;
                            PNTAB.add(words[i]);
                            trackParameters += "\t" + words[i];
                        }
                    }
                    parameters.add(trackParameters.trim());
                } 
                else if (words[0].equalsIgnoreCase("LCL") || words[0].equalsIgnoreCase("GBL")) 
                {
                    flag = true;
                    ArrayList<String> EVname = new ArrayList<>();
                    for (int i = 1; i < words.length; i++) {
                        String cleanedWord = words[i].replaceAll("[&,]", "");
                        EVNTAB.add(cleanedWord);
                        EV++;
                        tempEV++;
                        EVname.add("(E, " + tempEV + ")");
                    }
                    String mdtEntry = words[0] + "\t" + String.join("\t", EVname);
                    MDT.add(mdtEntry);
                    EVname.clear();  
                } 
                else if (words.length == 1 && words[0].equalsIgnoreCase("MEND")) 
                {
                    flag = false;
                    MDT.add("MEND" );
                    createSSTAB();
                    SSTP = updateSSTP();
                    MNT.add(macroName + "\t" + PP + "\t" + KP + "\t" + EV + "\t" + MDTP + "\t"+ (KP == 0 ? KPDTP : (KPDTP + 1)) + "\t" + SSTP);
                    MDTP = MDT.size() + 1;
                    KPDTP += KP;
                    PP = KP = EV = 0;
                }
                else if (flag) 
                {
                    if (words[0].startsWith(".")) {
                        String cleanedWord = words[0].replaceAll("[.]", "");
                        if (!SSNTAB.contains(cleanedWord)) {
                            SSNTAB.add(cleanedWord);
                            trackSSN.add(cleanedWord);
                        }
                    } 
                    else if (words[words.length - 1].startsWith(".")) {
                        String cleanedWord = words[words.length - 1].replaceAll("[.]", "");
                        if (!SSNTAB.contains(cleanedWord)) {
                            SSNTAB.add(cleanedWord);
                            trackSSN.add(cleanedWord);
                        }
                    }
                    
                    ArrayList<String> MDT_parts = new ArrayList<>();
                    for (int i = 0; i < words.length; i++) 
                    {
                        if (words[i].contains("&") || words[i].startsWith(".")) 
                        {
                            words[i] = words[i].replaceAll("[&,.()]", "");
                            if (PNTAB.contains(words[i])) {
                                MDT_parts.add("(P," + (PNTAB.indexOf(words[i]) + 1) + ")");
                            } else if (EVNTAB.contains(words[i])) {
                                MDT_parts.add("(E," + (EVNTAB.indexOf(words[i]) + 1) + ")");
                            } else if (SSNTAB.contains(words[i])) {
                                MDT_parts.add("(S, " + (SSNTAB.indexOf(words[i]) + 1) + ")");
                            }
                        }
                        else {
                            MDT_parts.add(words[i].replaceAll("[,()]", ""));
                        }
                    }
                    String mdtEntry = String.join("\t", MDT_parts);
                    MDT.add(mdtEntry);
                    MDT_parts.clear();
                }
                else if (words.length == 1 && words[0].equalsIgnoreCase("START")){
                    pass2(br);
                    flag = false;
                    break;
                } 

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    void createSSTAB(){
        for(int j=0; j<trackSSN.size(); j++){
            String str = trackSSN.get(j);
            Integer indexinSS = SSNTAB.indexOf(str)+1;
            Integer indexinMDT=0;

            for (int i = 0; i < MDT.size(); i++) {
                if (MDT.get(i).startsWith("(S, "+indexinSS +")")) {
                    indexinMDT = i + 1;
                    SSTAB.add(indexinMDT);
                    break;
                }
            }
        }
    }

    Integer updateSSTP() {  
        Integer temp = 0;       
        String str = trackSSN.get(0);
        Integer indexinSS = SSNTAB.indexOf(str)+1;
        Integer indexinMDT=0;

        for (int i = 0; i < MDT.size(); i++) {
            if (MDT.get(i).startsWith("(S, "+indexinSS +")")) {
                indexinMDT = i + 1;
                temp = SSTAB.indexOf(indexinMDT)+1;
                break;
            }
        }
        trackSSN.clear();
        return temp;
    }
    // --------------------------------------   PASS - I  END -------------------------------------------------------



    // --------------------------------------   PASS - II   ----------------------------------------------------------
    void pass2(BufferedReader br) 
    {
        String line;
        Integer MDTP = -1, SSTP = -1;
        boolean isMacro = false;
        HashMap<String, String> paramValues = new HashMap<>();
        HashMap<String, Integer> evNames = new HashMap<>();
    
        HashMap<String, String[]> macroParametersMap = new HashMap<>();
        HashMap<String, Integer[]> macroInfoMap = new HashMap<>();
    
        for (String entry : MNT) {
            String[] parts = entry.split("\\s+");
            String macroName = parts[0];
            macroInfoMap.put(macroName, new Integer[]{
                Integer.parseInt(parts[4]) - 1,
                Integer.parseInt(parts[6]) - 1
            });
            for (String paramSet : parameters) {
                if (paramSet.startsWith(macroName)) {
                    macroParametersMap.put(macroName, paramSet.split("\t"));
                }
            }
        }
    
        try (FileWriter output = new FileWriter("Output.txt")) {
            output.write("START\n");
            while ((line = br.readLine()) != null) 
            {
                if (line.isEmpty()) {
                    continue;
                }
                String[] words = line.split("\\s+");
    
                if (macroInfoMap.containsKey(words[0])) {
                    isMacro = true;
                    String macroName = words[0];
                    MDTP = macroInfoMap.get(macroName)[0];
                    SSTP = macroInfoMap.get(macroName)[1];
                    paramValues.clear();
                    evNames.clear();
    
                    String[] macroParams = macroParametersMap.get(macroName);
                    for (int j = 1; j < macroParams.length; j++) {
                        String param = macroParams[j];
                        if (j < words.length && words[j].contains("=")) {
                            paramValues.put(param, words[j].split("=")[1].replace(",", ""));
                        } else if (j < words.length) {
                            paramValues.put(param, words[j].replace(",", ""));
                        }
                    }
                }
    
                if (isMacro) 
                {
                    while (!MDT.get(MDTP).trim().equalsIgnoreCase("MEND")) 
                    {
                        Integer evIndex;
                        String[] mdtParts = MDT.get(MDTP).split("\t");
    
                        if (mdtParts[0].equalsIgnoreCase("LCL")) 
                        {
                            for (int i = 1; i < mdtParts.length; i++) {
                                evIndex = Integer.parseInt(mdtParts[i].replaceAll("[(E,)]", "").trim()) - 1;
                                evNames.put(EVNTAB.get(evIndex), -1);
                            }
                        }
                        else if (mdtParts[1].equalsIgnoreCase("SET")) 
                        {
                            handleSetStatement(mdtParts, evNames);
                        }
                        else if (mdtParts[0].equalsIgnoreCase("AIF")) 
                        {
                            if (evaluateAIF(mdtParts, evNames)) {
                                MDTP = SSTAB.get(SSTP) - 2;
                            }
                        }
                        else 
                        {
                            output.write(expandValues(mdtParts, paramValues, evNames) + "\n");
                        }
                        MDTP++;
                    }
                    APTAB.putAll(paramValues);
                    isMacro = false;
                } 
                else
                {
                    if ((words.length == 1) && words[0].equalsIgnoreCase("END")){
                        output.write("END");
                        output.close();
                        return;
                    }
                    output.write(line + "\n");
                }
            }
        }catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private void handleSetStatement(String[] mdtParts, HashMap<String, Integer> evNames) 
    {
        Integer evIndex = Integer.parseInt(mdtParts[0].replaceAll("[(E,)]", "")) - 1;
        if (evNames.containsKey(EVNTAB.get(evIndex))) {
            try {
                evNames.put(EVNTAB.get(evIndex), Integer.parseInt(mdtParts[2]));
            } catch (Exception e) {
                Integer oldVal = evNames.getOrDefault(EVNTAB.get(evIndex), 0);
                Integer newVal;
                if (mdtParts[3].trim().equalsIgnoreCase("+")) {
                    newVal = oldVal + Integer.parseInt(mdtParts[4].trim());
                    evNames.put(EVNTAB.get(evIndex), newVal);
                } else if (mdtParts[3].trim().equalsIgnoreCase("-")) {
                    newVal = oldVal - Integer.parseInt(mdtParts[4].trim());
                    evNames.put(EVNTAB.get(evIndex), newVal);
                }
            }
        }
    }
    
    private boolean evaluateAIF(String[] mdtParts, HashMap<String, Integer> evNames) 
    {
        Integer evIndex = Integer.parseInt(mdtParts[1].replaceAll("[(E,)]", "").trim()) - 1;
        Integer op1 = evNames.get(EVNTAB.get(evIndex));
        Integer op2 = Integer.parseInt(mdtParts[3].replaceAll("[(),]", "").trim());
        String operator = mdtParts[2].trim();
    
        switch (operator) 
        {
            case "NE":
                return op1 != op2;
            case "EQ":
                return op1.equals(op2);
            case "GT":
                return op1 > op2;
            case "LT":
                return op1 < op2;
            default:
                return false;
        }
    }
    


    private String expandValues(String[] mdtParts, HashMap<String, String> paramValues, HashMap<String, Integer> evNames){

        StringBuilder expandedLine = new StringBuilder();        
        for(String part : mdtParts)
        {
            if(part.startsWith("(S,")){
                continue;
            }
            else if(part.startsWith("(P,")){
                int paramIndex = Integer.parseInt(part.replaceAll("[(),]", "").split("P")[1]) - 1;
                expandedLine.append(paramValues.get(PNTAB.get(paramIndex))).append(" ");
            }
            else if (part.startsWith("(E,")) {
                int evIndex = Integer.parseInt(part.replaceAll("[(),]", "").split("E")[1]) - 1;
                expandedLine.append(evNames.get(EVNTAB.get(evIndex))).append(" ");
            }
            else {
                expandedLine.append(part).append(" ");
            }
            
        }

        return expandedLine.toString().trim();
    }
    
    // --------------------------------------   PASS - II  END -------------------------------------------------------


    void Print_Tables()
    {
        System.out.println("\n---------------------------\nMNT:\n");
        System.out.println("Index\tMACRO\t#PP\t#KP\t#EV\tMDTP\tKPDTP\tSSTP");
        for(int i=0; i<MNT.size(); i++){
            System.out.println((i + 1) + ":\t" + (MNT.get(i)));
        }
            
        System.out.println("\n---------------------------\nMDT:\n");
        System.out.println("Index" + "\t" + "MACRO Definition\n");
        for (int i = 0; i < MDT.size(); i++) {
            System.out.println((i + 1) + ":\t" + (MDT.get(i)));
        }
    
        System.out.println("\n---------------------------\nPNTAB:\n");
        System.out.println("Index" + "\t" + "Parameter Name\n");
        for (int i = 0; i < PNTAB.size(); i++) {
            System.out.println((i + 1) + ":\t" + PNTAB.get(i));
        }
    
        System.out.println("\n---------------------------\nEVNTAB:\n");
        System.out.println("Index" + "\t" + "EV Name\n");
        for (int i = 0; i < EVNTAB.size(); i++) {
            System.out.println((i + 1) + ":\t" + EVNTAB.get(i));
        }
    
        System.out.println("\n---------------------------\nSSNTAB:\n");
        System.out.println("Index" + "\t" + "SS Name\n");
        for (int i = 0; i < SSNTAB.size(); i++) {
            System.out.println((i + 1) + ":\t" + SSNTAB.get(i));
        }
    
        System.out.println("\n---------------------------\nKPDTAB:\n");
        System.out.println("Index" + "\t" + "Parameter"+ "\t" +  "Value\n");
        for (int i = 0; i < KPDTAB.size(); i++) {
            System.out.println((i + 1) + ":\t" +(KPDTAB.get(i)));
        }

        System.out.println("\n---------------------------\nSSTAB:\n");
        System.out.println("Index" + "\t" + "MDT_ENTRY\n");
        for(int i=0; i<SSTAB.size(); i++){
            System.out.println((i + 1) + ":\t" + (SSTAB.get(i)));
        }

        System.out.println("\n---------------------------\nAPTAB:\n");
        System.out.println("Index" + "\t " + "Parameter" + "\t" + "Value\n");

        int index = 1;
        for (Map.Entry<String, String> entry : APTAB.entrySet()) {
            System.out.println(index + ":\t " + entry.getKey() + "\t" + entry.getValue());
            index++;
        }
    }
}
    
public class Assignment_5 extends MacroPass{
    public static void main(String[] arg) {
        Assignment_5 MACRO = new Assignment_5();
        try {
            MACRO.pass1("input.asm");
            MACRO.Print_Tables();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}