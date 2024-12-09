package src;

public class ReservationStation {
    private String name;
    private boolean busy = false;
    private String op; // instruction
    private double Vj;
    private double Vk;
    private String Qj = "";
    private String Qk = "";
    private boolean executing = false;
    private double result;
    private int remainingCycles;

    // Instruction latencies
    private static final int ADD_LATENCY = 1;      // Reduced from 2
    private static final int SUB_LATENCY = 1;      // Reduced from 2
    private static final int MUL_LATENCY = 2;      // Reduced from 10
    private static final int DIV_LATENCY = 4;      // Reduced from 40
    private static final int ADD_S_LATENCY = 1;    // Reduced from 2
    private static final int SUB_S_LATENCY = 1;    // Reduced from 2
    private static final int MUL_S_LATENCY = 2;    // Reduced from 4
    private static final int DIV_S_LATENCY = 4;    // Reduced from 8
    private static final int INTEGER_LATENCY = 1;  // Same
    private static final int BRANCH_LATENCY = 1;   // Same

    public ReservationStation(String name) {
        this.name = name;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public double getVj() {
        return Vj;
    }

    public void setVj(double vj) {
        this.Vj = vj;
    }

    public double getVk() {
        return Vk;
    }

    public void setVk(double vk) {
        this.Vk = vk;
    }

    public String getQj() {
        return Qj;
    }

    public void setQj(String qj) {
        this.Qj = qj;
    }

    public String getQk() {
        return Qk;
    }

    public void setQk(String qk) {
        this.Qk = qk;
    }

    public boolean isExecuting() {
        return executing;
    }

    public void setExecuting(boolean executing) {
        this.executing = executing;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void reset() {
        busy = false;
        op = "";
        Vj = 0;
        Vk = 0;
        Qj = "";
        Qk = "";
        executing = false;
        result = 0;
        remainingCycles = 0;
    }

    public void execute() {
        if (!executing) return;
        
        if (remainingCycles > 0) {
            remainingCycles--;
            System.out.println(name + " executing " + op + ", " + remainingCycles + " cycles left");
            return;
        }
        
        // Only perform the operation when cycles are done
        if (remainingCycles == 0) {
            System.out.println(name + " completing execution of " + op + " with Vj=" + Vj + " and Vk=" + Vk);
            switch (op.toUpperCase()) {
                case "ADD.D":
                case "ADD.S":
                case "DADDI":
                    result = Vj + Vk;
                    break;
                case "SUB.D":
                case "SUB.S":
                case "DSUBI":
                    result = Vj - Vk;
                    break;
                case "MUL.D":
                case "MUL.S":
                    result = Vj * Vk;
                    break;
                case "DIV.D":
                case "DIV.S":
                    if (Vk == 0) {
                        System.err.println("Error: Division by zero");
                        result = 0;
                    } else {
                        result = Vj / Vk;
                    }
                    break;
                case "BEQ":
                    result = (Vj == Vk) ? 1 : 0;
                    break;
                case "BNE":
                    result = (Vj != Vk) ? 1 : 0;
                    break;
                default:
                    System.err.println("Unknown operation: " + op);
            }
            executing = false;  // Mark execution as complete
            System.out.println(name + " result: " + result);
        }
    }

    public int getLatency() {
        if (op == null) return 1;  // Default latency
        
        switch (op.toUpperCase()) {
            case "ADD.D":
                return ADD_LATENCY;
            case "SUB.D":
                return SUB_LATENCY;
            case "MUL.D":
                return MUL_LATENCY;
            case "DIV.D":
                return DIV_LATENCY;
            case "ADD.S":
                return ADD_S_LATENCY;
            case "SUB.S":
                return SUB_S_LATENCY;
            case "MUL.S":
                return MUL_S_LATENCY;
            case "DIV.S":
                return DIV_S_LATENCY;
            case "DADDI":
            case "DSUBI":
                return INTEGER_LATENCY;
            case "BEQ":
            case "BNE":
                return BRANCH_LATENCY;
            default:
                return 1;  // Default latency for unknown operations
        }
    }

    public int getCyclesLeft() {
        return remainingCycles;
    }

    public void setCyclesLeft(int cycles) {
        this.remainingCycles = cycles;
    }
}
