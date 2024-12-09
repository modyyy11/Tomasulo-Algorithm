package src;
import src.Instructions.Register;

public class InstructionQueue {
    public String instruction;  // Instruction type (e.g., L.D, ADD.D)
    public String d;            // Destination register
    public String j;            // Source 1 register/memory address
    public String k;            // Source 2 register
    public int issue;           // Issue cycle
    public int startTime;       // Execution start cycle
    public int endTime;         // Execution end cycle
    public int wbTime;          // Write-back time
    public boolean isLoopStart; // Indicates if this is a loop start instruction
    public boolean isLoopEnd;   // Indicates if this is a loop end instruction
    public int loopIterations;  // Number of iterations for the loop
    public int currentIteration; // Tracks the current loop iteration

    public InstructionQueue(String instruction, String dest, String src1, String src2, 
                            int issue, int startTime, int endTime, int wbTime) {
        this.instruction = instruction;
        this.d = dest;
        this.j = src1;
        this.k = src2;
        this.issue = issue;
        this.startTime = startTime;
        this.endTime = endTime;
        this.wbTime = wbTime;
        this.isLoopStart = false;
        this.isLoopEnd = false;
        this.loopIterations = 0;
        this.currentIteration = 0;
    }
}
