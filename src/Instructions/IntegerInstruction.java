package src.Instructions;

public class IntegerInstruction {
    private boolean op; // true for DADDI, false for DSUBI
    
    public IntegerInstruction(boolean op) {
        this.op = op;
    }
    
    public double IntegerOp(Register s1, int immediate, Register d) {
        double result;
        if (op) {
            // DADDI
            result = s1.getValue() + immediate;
        } else {
            // DSUBI
            result = s1.getValue() - immediate;
        }
        d.setValue(result);
        return result;
    }
}
