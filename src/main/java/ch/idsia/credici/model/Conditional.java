package ch.idsia.credici.model;

import ch.idsia.crema.model.Domain;

public class Conditional {
    private int left;
    private int[] right;
    public Conditional(int left, int[] right) {
        this.left = left;
        this.right = right;
    }

    public Conditional(Domain left, Domain right) {
        if (left.getSize() != 1) new IllegalArgumentException("Left side must be size 1");
        this.left = left.getVariables()[0];
        this.right = right.getVariables();
    }

    public int getLeft() {
        return left;
    }
    public int[] getRight() {
        return right;
    }
}
