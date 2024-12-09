package src.Instructions;

public class SinglePrecisionInstruction {
    private String operation; // "ADD.S", "SUB.S", "MUL.S", or "DIV.S"
    
    public SinglePrecisionInstruction(String operation) {
        this.operation = operation;
    }
    
    public float SinglePrecisionOp(Register s1, Register s2, Register d) {
        float result = 0.0f;
        float val1 = (float)s1.getValue();
        float val2 = (float)s2.getValue();
        
        switch (operation) {
            case "ADD.S":
                result = val1 + val2;
                break;
            case "SUB.S":
                result = val1 - val2;
                break;
            case "MUL.S":
                result = val1 * val2;
                break;
            case "DIV.S":
                if (val2 == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                result = val1 / val2;
                break;
            default:
                throw new IllegalArgumentException("Invalid single-precision operation: " + operation);
        }
        
        d.setValue(result);
        return result;
    }
}
