package src;

public class Memory {
    private double[] memory;
    private static final int MEMORY_SIZE = 1024;

    public Memory() {
        memory = new double[MEMORY_SIZE];
        reset();
    }

    public double read(int address) {
        if (isValidAddress(address)) {
            return memory[address];
        }
        throw new IllegalArgumentException("Invalid memory address: " + address);
    }

    public void write(int address, double value) {
        if (isValidAddress(address)) {
            memory[address] = value;
        } else {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
    }

    public void reset() {
        // Initialize memory with some test values
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
        // Initialize memory for test case
        memory[0] = 2.0;  // For L.D F6, 0
        memory[4] = 2.0;  // For L.D F2, 4
    }

    private boolean isValidAddress(int address) {
        return address >= 0 && address < MEMORY_SIZE;
    }
}
