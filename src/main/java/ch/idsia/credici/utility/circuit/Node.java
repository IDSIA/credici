package ch.idsia.credici.utility.circuit;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Node {
    public enum Type {
        LITERAL,
        ADD,
        MULT;

        public static Type get(String code) {
            switch(code) {
                case "+": return ADD;
                case "*": return MULT;
                default: return null;
            }
        }
    }

    private double weight;
    private String str;
    private int id;
    private int label;
    private Type type;
    private List<Node> parents; 
    private List<Node> children; 
    

    public Node(int id, int label, Type type){
        this.id = id;
        this.label = label;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.type = type;
        this.str = "";
    }

    public int getId() {
        return id;
    }

    public Node setId(int id) {
        this.id = id;
        return this;
    }

    public List<Node> getParents() {
        return new ArrayList<>(parents);
    }

    public Node addParent(Node parent) {
        if (this.parents == null) this.parents = new ArrayList<>();
        this.parents.add(parent);
        return this;
    }

    public Node addChild(Node node) {
        if (children == null) children = new ArrayList<>();
        children.add(node);
        return this;
    }

    public Node removeChild(Node node) {
        if (children == null) return this;
        children.remove(node);
        return this;
    }

    public List<Node> getChildren() {
        return new ArrayList<>(this.children);
    }
    
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    public String getString() {
        return str;
    }

    public void setString(String str) {
        this.str = str;
    }

    public String toString() {
        NumberFormat nf = new DecimalFormat("0.###");

        String ws = (weight != 1 && weight != 0) ? "" + nf.format(weight) : "";
        switch(this.type){
            case ADD: return "+" +ws;
            case MULT: return "*"+ws;
            case LITERAL: return str + " " + ws;
            default: return "N/A";
        }
    }
}