package src.Instructions;
import src.Instructions.Register;
import src.Memory;

public class LoadStoreInstruction {
    private boolean op; // true -> LD , false -> SD
    private Register r;
    private int address;
    private Memory memory;

    public LoadStoreInstruction(boolean op, Register r, int address, Memory memory) {
        this.op = op;
        this.r = r;
        this.address = address;
        this.memory = memory;
    }

    public double LoadStore() {
        if (op) {
            return memory.read(address);  // Changed from readData to read
        } else {
            memory.write(address, r.getValue());  // Changed from writeData to write
            return r.getValue();
        }
    }
}