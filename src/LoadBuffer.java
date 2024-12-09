package src;

public class LoadBuffer {
    private String name;
    private boolean busy;
    private int address;
    private boolean executing;
    private int executionCycles;
    private int remainingCycles;
    private double value;
    private boolean waitingForCache;
    private static final int CACHE_MISS_PENALTY = 2;  // Reduced from 10

    public LoadBuffer(String name) {
        this.name = name;
        reset();
    }

    public void execute(Memory memory, Cache cache) {
        if (!executing && busy) {
            // Start execution
            executing = true;
            waitingForCache = true;
            remainingCycles = cache.hasBlock(address) ? 1 : CACHE_MISS_PENALTY;
            
            // Get the value immediately
            if (cache.hasBlock(address)) {
                value = cache.read(address);
                System.out.println(name + " cache hit, reading " + value + " from address " + address);
            } else {
                value = memory.read(address);
                cache.loadBlock(address, memory);
                System.out.println(name + " cache miss, loaded " + value + " from memory address " + address);
            }
        } else if (executing && busy) {
            remainingCycles--;
            System.out.println(name + " remaining cycles: " + remainingCycles);
            if (remainingCycles <= 0) {
                executing = false;  // Mark as ready for writeback
                System.out.println(name + " completed load with value " + value + " - ready for writeback");
            }
        }
    }

    public void reset() {
        busy = false;
        address = 0;
        executing = false;
        executionCycles = 2;
        remainingCycles = executionCycles;
        waitingForCache = false;
        value = 0.0;
    }

    // New method to set the executing status
    public void setExecuting(boolean executing) {
        this.executing = executing;
    }

    // Getters and setters
    public String getName() { return name; }
    public boolean isBusy() { return busy; }
    public void setBusy(boolean busy) { this.busy = busy; }
    public int getAddress() { return address; }
    public void setAddress(int address) { this.address = address; }
    public boolean isExecuting() { return executing; }
    public double getValue() { return value; }
    public boolean isWaitingForCache() { return waitingForCache; }
}
