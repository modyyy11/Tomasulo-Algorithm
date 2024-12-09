package src.Instructions;

public class BranchInstruction {
    private boolean op; // true for BEQ, false for BNE
    
    public BranchInstruction(boolean op) {
        this.op = op;
    }
    
    public boolean Branch(Register s1, Register s2) {
        if (op) {
            // BEQ
            return s1.getValue() == s2.getValue();
        } else {
            // BNE
            return s1.getValue() != s2.getValue();
        }
    }
}
