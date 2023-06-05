package ch.idsia.credici.model.io.dot;

import ch.idsia.credici.model.StructuralCausalModel;

public class DotSerialize {
    public String run(StructuralCausalModel gm) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("digraph model {\n");
        for (int i : gm.getVariables()) {
            
            builder.append("   node" + i + " [label=\"" + i + " (" + gm.getSize(i) +")\"");
            
            if (gm.isExogenous(i)) builder.append(" shape=box");
            if (gm.isSpurious(i)) builder.append(" color=gray fontcolor=gray");
            

            builder.append("];\n");
        }
        
        for (int i : gm.getVariables()) {
            for (int child : gm.getChildren(i)) {
                builder.append("   node").append(i).append(" -> node").append(child);
                if (gm.isSpurious(i))
                    builder.append("[color=gray]");
                builder.append(";\n");
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
