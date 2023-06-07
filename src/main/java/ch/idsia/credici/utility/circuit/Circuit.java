package ch.idsia.credici.utility.circuit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Circuit {
    Map<Integer, Node> nodes;
    Map<Integer, Node> literals;

    public Circuit() {
       
    }

    public void load(BufferedReader reader) throws IOException {
        nodes = new TreeMap<>();
        literals = new TreeMap<>();
        String line;  
        int nodeId=0;
        while ((line = reader.readLine()) != null) {
            String[] codes = line.split(" ");
            if ("l".equals(codes[0])) {
                int l = Integer.parseInt(codes[1]);
                Node node = new Node(nodeId++, l, Node.Type.LITERAL);
                nodes.put(node.getId(), node);
                literals.put(node.getLabel(), node);
            }
            else if ("+".equals(codes[0]) || "*".equals(codes[0])) {
                Node node = new Node(nodeId++, 0, Node.Type.get(codes[0]));
                nodes.put(node.getId(), node);

                for (int i = 2; i < codes.length; ++i) {
                    int childId = Integer.parseInt(codes[i]);
                    Node child = nodes.get(childId);
                    
                    child.addParent(node);          
                    node.addChild(child);
                }
            }
        }
    }

    public void loadLmap(BufferedReader reader) throws NumberFormatException, IOException {
        String line;  
        int nodeId=0;
        while ((line = reader.readLine()) != null) {
            
            String[] codes = line.split("\\$");
            if (!"cc".equals(codes[0])) continue;

            if ("C".equals(codes[1])) {
                int lit = Integer.parseInt(codes[2]);
                Node n = literals.get(lit);
                if (n != null) {
                    double w = Double.parseDouble(codes[3]);
                    n.setWeight(w);
                } else {
                    System.out.println("forgetting C" + lit);
                }
            }
            else if ("I".equals(codes[1])) {
                int lit = Integer.parseInt(codes[2]);
                Node n = literals.get(lit);
                if (n != null) {
                    double w = Double.parseDouble(codes[3]);
                    n.setWeight(w);
                    n.setString(codes[4] + codes[5] + "("+codes[6]+")");
                } else {
                    System.out.println("forgetting I" + lit);
                }
            }
        }
    }
    public String dot() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("digraph circuit {");
        for (var m : nodes.values()) {
            pw.println("n" + m.getId() + "[label=\""+m.toString()+"\"];");
        }

        for (var m : nodes.values()) {
            List<Node> parents = m.getParents();
            for (Node parent:parents) {
                pw.println("n" + parent.getId() + " -> n" + m.getId() + ";");
            }
        }
        pw.println("}");
        pw.flush();
        return sw.toString();
    }

    public static void main(String[] args) {
        
        Circuit one = new Circuit();
        try (BufferedReader fr = new BufferedReader(new FileReader("circuito.ac"))) {
            one.load(fr);
        } catch(IOException e) {
            e.printStackTrace();
        }
        try (BufferedReader fr = new BufferedReader(new FileReader("circuito.lmap"))) {
            one.loadLmap(fr);
        } catch(IOException e) {
            e.printStackTrace();
        }
        System.out.println(one.dot());
    }
}
