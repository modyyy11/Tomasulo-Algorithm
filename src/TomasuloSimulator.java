package src;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import src.Instructions.Register;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TomasuloSimulator extends Application {
    private Register[] registers;
    private ArrayList<InstructionQueue> instructionQueue;
    private ArrayList<ReservationStation> addSubRS;
    private ArrayList<ReservationStation> mulDivRS;
    private ArrayList<LoadBuffer> loadBuffers;
    private ArrayList<StoreBuffer> storeBuffers;
    private Memory memory;
    private Cache cache;
    private int currentCycle;
    private boolean isExecuting;
    
    private TextArea statusArea;
    private Label cycleLabel;
    private TextArea instructionsInput;
    private Slider speedSlider;
    private volatile boolean pauseRequested = false;
    private Thread simulationThread;
    private BorderPane layout;
    private TextField[] registerInputs;
    private TextField cacheSizeField;
    private TextField blockSizeField;

    public TomasuloSimulator() {
        registers = new Register[6];
        registerInputs = new TextField[6];
        for (int i = 0; i < 6; i++) {
            registers[i] = new Register(0);
            registers[i].setName("F" + (i * 2));
        }
        
        instructionQueue = new ArrayList<>();
        addSubRS = new ArrayList<>();
        mulDivRS = new ArrayList<>();
        loadBuffers = new ArrayList<>();
        storeBuffers = new ArrayList<>();
        memory = new Memory();
        currentCycle = 0;
        isExecuting = false;
        
        for (int i = 0; i < 3; i++) {
            addSubRS.add(new ReservationStation("Add" + (i + 1)));
            mulDivRS.add(new ReservationStation("Mult" + (i + 1)));
            loadBuffers.add(new LoadBuffer("Load" + (i + 1)));
            storeBuffers.add(new StoreBuffer("Store" + (i + 1)));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tomasulo Architecture Simulator");
        layout = new BorderPane();

        // Left panel with register inputs
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.getChildren().add(new Label("Initial Register Values:"));
        
        // Initialize register input fields
        for (int i = 0; i < 6; i++) {
            HBox registerBox = new HBox(5);
            Label regLabel = new Label("F" + (i * 2) + ":");
            registerInputs[i] = new TextField("0.0");
            registerInputs[i].setPrefWidth(100);
            registerInputs[i].setPromptText("Enter value for F" + (i * 2));
            registerBox.getChildren().addAll(regLabel, registerInputs[i]);
            leftPanel.getChildren().add(registerBox);
        }

        // Add cache configuration
        leftPanel.getChildren().add(new Label("\nCache Configuration:"));
        
        HBox cacheSizeBox = new HBox(5);
        cacheSizeBox.getChildren().addAll(new Label("Cache Size (bytes):"), cacheSizeField = new TextField("64"));
        cacheSizeField.setPrefWidth(100);
        
        HBox blockSizeBox = new HBox(5);
        blockSizeBox.getChildren().addAll(new Label("Block Size (bytes):"), blockSizeField = new TextField("16"));
        blockSizeField.setPrefWidth(100);
        
        leftPanel.getChildren().addAll(cacheSizeBox, blockSizeBox);

        // Center panel with instruction input
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));
        
        Label instructionsLabel = new Label("Instructions (one per line):");
        instructionsInput = new TextArea();
        instructionsInput.setPrefRowCount(10);
        instructionsInput.setWrapText(true);
        instructionsInput.setPromptText(
            "Example:\n" +
            "L.D F6, 0\n" +
            "L.D F2, 4\n" +
            "MUL.D F0, F2, F4\n" +
            "SUB.D F8, F2, F6\n" +
            "DIV.D F10, F0, F6\n" +
            "ADD.D F6, F8, F2\n" +
            "S.D F6, 0"
        );
        
        // Status area for simulation output
        Label statusLabel = new Label("Simulation Status:");
        statusArea = new TextArea();
        statusArea.setPrefRowCount(10);
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        
        cycleLabel = new Label("Current Cycle: 0");
        
        // Speed control slider
        HBox speedControl = new HBox(10);
        speedSlider = new Slider(100, 1000, 500);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedControl.getChildren().addAll(
            new Label("Simulation Speed (ms):"),
            speedSlider
        );
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        Button executeButton = new Button("Execute");
        Button stepButton = new Button("Step");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        buttonBox.getChildren().addAll(executeButton, stepButton, pauseButton, resetButton);
        
        centerPanel.getChildren().addAll(
            instructionsLabel, instructionsInput,
            statusLabel, statusArea,
            cycleLabel,
            speedControl,
            buttonBox
        );
        
        // Add panels to layout
        layout.setLeft(leftPanel);
        layout.setCenter(centerPanel);
        
        // Button event handlers
        executeButton.setOnAction(e -> {
            if (!isExecuting) {
                isExecuting = true;
                executeInstructions();
            }
        });
        
        stepButton.setOnAction(e -> {
            if (!isExecuting) {
                isExecuting = true;
                stepExecution();
            }
        });
        
        pauseButton.setOnAction(e -> {
            pauseRequested = !pauseRequested;
            pauseButton.setText(pauseRequested ? "Resume" : "Pause");
        });
        
        resetButton.setOnAction(e -> {
            if (simulationThread != null) {
                simulationThread.interrupt();
            }
            resetSimulation();
            statusArea.clear();
            cycleLabel.setText("Current Cycle: 0");
            pauseButton.setText("Pause");
            isExecuting = false;
            pauseRequested = false;
        });
        
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void executeInstructions() {
        System.out.println("\n=== Starting Tomasulo Simulation ===");
        System.out.println("Initializing registers and memory...");
        resetSimulation();
        
        // Initialize registers from text fields
        System.out.println("\nSetting initial register values:");
        for (int i = 0; i < registers.length; i++) {
            try {
                String inputText = registerInputs[i].getText().trim();
                if (!inputText.isEmpty()) {
                    double value = Double.parseDouble(inputText);
                    registers[i].setValue(value);
                    System.out.println("Set F" + (i*2) + " = " + value);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input for F" + (i*2) + ", using default value 0.0");
            }
        }
        
        // Parse and queue instructions
        System.out.println("\nParsing instructions:");
        String[] lines = instructionsInput.getText().split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                parseInstruction(line);
            }
        }
        
        if (instructionQueue.isEmpty()) {
            System.out.println("No valid instructions to execute!");
            return;
        }
        
        // Start simulation in background thread
        simulationThread = new Thread(() -> {
            try {
                runFullSimulation();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusArea.appendText("\nError during simulation: " + e.getMessage());
                });
            }
        });
        simulationThread.start();
    }

    private void parseInstruction(String instruction) {
        System.out.println("Raw instruction: " + instruction);
        
        instruction = instruction.replaceAll("\\s*[.]\\s*", "."); 
        instruction = instruction.replaceAll("\\s+", " ").trim(); 
        
        String[] parts = instruction.split("[,\\s]+");
        if (parts.length < 2) {
            System.out.println("Invalid instruction format");
            return;
        }
        
        String op;
        if (parts.length > 1 && parts[0].length() == 1) {
            op = parts[0] + "." + parts[1];
            String[] newParts = new String[parts.length - 1];
            newParts[0] = op;
            System.arraycopy(parts, 2, newParts, 1, parts.length - 2);
            parts = newParts;
        } else {
            op = parts[0].toUpperCase();
        }
        
        System.out.println("Parsed operation: " + op);
        String dest = parts[1];
        String src1 = parts.length > 2 ? parts[2] : "";
        String src2 = parts.length > 3 ? parts[3] : "";
        
        System.out.println("Parsed components: dest=" + dest + ", src1=" + src1 + ", src2=" + src2);
        
        if (op.equals("L.D") || op.equals("S.D")) {
            try {
                int address = Integer.parseInt(src1);
                instructionQueue.add(new InstructionQueue(op, dest, String.valueOf(address), "", -1, -1, -1, -1));
                System.out.println("Added load/store instruction with address: " + address);
            } catch (NumberFormatException e) {
                System.err.println("Invalid memory address: " + src1);
            }
        } else if (op.equals("ADDI") || op.equals("SUBI") || op.equals("ANDI") || op.equals("ORI")) {
            instructionQueue.add(new InstructionQueue(op, dest, src1, src2, -1, -1, -1, -1));
            System.out.println("Added integer instruction");
        } else if (op.equals("BEQ") || op.equals("BNE")) {
            instructionQueue.add(new InstructionQueue(op, dest, src1, src2, -1, -1, -1, -1));
            System.out.println("Added branch instruction");
        } else {
            instructionQueue.add(new InstructionQueue(op, dest, src1, src2, -1, -1, -1, -1));
            System.out.println("Added arithmetic instruction");
        }
    }

    private void runFullSimulation() throws InterruptedException {
        System.out.println("\n=== Starting Full Simulation ===");
        
        while (!allInstructionsComplete() && !Thread.currentThread().isInterrupted()) {
            if (!pauseRequested) {
                // Execute one cycle directly in this thread
                if (!instructionQueue.isEmpty()) {
                    System.out.println("Trying to issue: " + instructionQueue.get(0).instruction);
                    tryIssueNextInstruction();
                }
                
                executeReservationStations();
                performWriteBack();
                
                // Update UI in JavaFX thread
                Platform.runLater(() -> {
                    currentCycle++;
                    cycleLabel.setText("Current Cycle: " + currentCycle);
                    updateStatus();
                });
                
                Thread.sleep((long)speedSlider.getValue());
            } else {
                Thread.sleep(100);
            }
        }
        
        Platform.runLater(() -> {
            isExecuting = false;
            pauseRequested = false;
            System.out.println("Simulation complete!");
        });
    }

    private void stepExecution() {
        if (!isExecuting) {
            // First time stepping, initialize everything
            System.out.println("\n=== Starting Step-by-Step Simulation ===");
            resetSimulation();
            
            // Initialize registers from text fields
            System.out.println("\nSetting initial register values:");
            for (int i = 0; i < registers.length; i++) {
                try {
                    String inputText = registerInputs[i].getText().trim();
                    if (!inputText.isEmpty()) {
                        double value = Double.parseDouble(inputText);
                        registers[i].setValue(value);
                        System.out.println("Set F" + (i*2) + " = " + value);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input for F" + (i*2) + ", using default value 0.0");
                }
            }
            
            // Parse instructions if not already parsed
            if (instructionQueue.isEmpty()) {
                System.out.println("\nParsing instructions:");
                String[] lines = instructionsInput.getText().split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        parseInstruction(line);
                    }
                }
            }
        }

        // Execute one cycle
        System.out.println("\n=== Cycle " + currentCycle + " ===");
        
        // Try to issue a new instruction
        if (!instructionQueue.isEmpty()) {
            System.out.println("Trying to issue: " + instructionQueue.get(0).instruction);
            tryIssueNextInstruction();
        }
        
        // Execute all stations
        executeReservationStations();
        
        // Perform write back
        performWriteBack();
        
        // Update cycle counter and UI
        currentCycle++;
        cycleLabel.setText("Current Cycle: " + currentCycle);
        updateStatus();
        
        // Check if simulation is complete
        if (allInstructionsComplete()) {
            System.out.println("All instructions complete!");
            isExecuting = false;
        }
    }

    private void tryIssueNextInstruction() {
        if (instructionQueue.isEmpty()) {
            return;
        }

        InstructionQueue instruction = instructionQueue.get(0);
        boolean issued = false;
        
        System.out.println("\nTrying to issue: " + instruction.instruction);
        
        switch (instruction.instruction) {
            case "L.D":
                issued = issueLoadInstruction(instruction);
                break;
            case "S.D":
                issued = issueStoreInstruction(instruction);
                break;
            case "ADD.D":
            case "SUB.D":
                issued = issueAddSubInstruction(instruction);
                break;
            case "MUL.D":
            case "DIV.D":
                issued = issueMulDivInstruction(instruction);
                break;
            case "DADDI":
            case "DSUBI":
                issued = issueIntegerInstruction(instruction);
                break;
            case "BEQ":
            case "BNE":
                issued = issueBranchInstruction(instruction);
                break;
        }
        
        if (issued) {
            System.out.println("Successfully issued: " + instruction.instruction);
            instructionQueue.remove(0);
        } else {
            System.out.println("Could not issue: " + instruction.instruction + " (no available stations)");
        }
    }

    private boolean issueLoadInstruction(InstructionQueue instruction) {
        System.out.println("Trying to issue load instruction to buffers: " + loadBuffers.size());
        for (LoadBuffer lb : loadBuffers) {
            System.out.println("Checking buffer " + lb.getName() + ": busy=" + lb.isBusy());
            if (!lb.isBusy()) {
                lb.setBusy(true);
                lb.setAddress(Integer.parseInt(instruction.j));
                Register destReg = getRegisterByName(instruction.d);
                if (destReg != null) {
                    System.out.println("Setting " + destReg.getName() + " Qi to " + lb.getName());
                    destReg.setQi(lb.getName());
                    return true;
                } else {
                    System.out.println("Error: Could not find register " + instruction.d);
                    return false;
                }
            }
        }
        return false;
    }

    private boolean issueStoreInstruction(InstructionQueue instruction) {
        for (StoreBuffer sb : storeBuffers) {
            if (!sb.isBusy()) {
                sb.setBusy(true);
                sb.setAddress(Integer.parseInt(instruction.j));
                Register srcReg = getRegisterByName(instruction.d);
                if (!srcReg.getQi().isEmpty()) {
                    sb.setQ(srcReg.getQi());
                } else {
                    sb.setValue(srcReg.getValue());
                }
                return true;
            }
        }
        return false;
    }

    private boolean issueAddSubInstruction(InstructionQueue instruction) {
        for (ReservationStation rs : addSubRS) {
            if (!rs.isBusy()) {
                rs.setBusy(true);
                rs.setOp(instruction.instruction);
                
                Register destReg = getRegisterByName(instruction.d);
                Register src1Reg = getRegisterByName(instruction.j);
                Register src2Reg = getRegisterByName(instruction.k);
                
                // Set the destination register's Qi to this reservation station
                destReg.setQi(rs.getName());
                System.out.println("Set " + destReg.getName() + " Qi to " + rs.getName());
                
                // Check source operands for dependencies
                if (!src1Reg.getQi().isEmpty()) {
                    rs.setQj(src1Reg.getQi());
                    System.out.println(rs.getName() + " waiting for " + src1Reg.getName() + " from " + src1Reg.getQi());
                } else {
                    rs.setVj(src1Reg.getValue());
                    System.out.println(rs.getName() + " got " + src1Reg.getName() + " value: " + src1Reg.getValue());
                }
                
                if (!src2Reg.getQi().isEmpty()) {
                    rs.setQk(src2Reg.getQi());
                    System.out.println(rs.getName() + " waiting for " + src2Reg.getName() + " from " + src2Reg.getQi());
                } else {
                    rs.setVk(src2Reg.getValue());
                    System.out.println(rs.getName() + " got " + src2Reg.getName() + " value: " + src2Reg.getValue());
                }
                
                System.out.println("Successfully issued " + instruction.instruction + " to " + rs.getName());
                return true;
            }
        }
        return false;
    }

    private boolean issueMulDivInstruction(InstructionQueue instruction) {
        for (ReservationStation rs : mulDivRS) {
            if (!rs.isBusy()) {
                rs.setBusy(true);
                rs.setOp(instruction.instruction);
                
                Register destReg = getRegisterByName(instruction.d);
                Register src1Reg = getRegisterByName(instruction.j);
                Register src2Reg = getRegisterByName(instruction.k);
                
                // Set the destination register's Qi to this reservation station
                destReg.setQi(rs.getName());
                System.out.println("Set " + destReg.getName() + " Qi to " + rs.getName());
                
                // Check source operands for dependencies
                if (!src1Reg.getQi().isEmpty()) {
                    rs.setQj(src1Reg.getQi());
                    System.out.println(rs.getName() + " waiting for " + src1Reg.getName() + " from " + src1Reg.getQi());
                } else {
                    rs.setVj(src1Reg.getValue());
                    System.out.println(rs.getName() + " got " + src1Reg.getName() + " value: " + src1Reg.getValue());
                }
                
                if (!src2Reg.getQi().isEmpty()) {
                    rs.setQk(src2Reg.getQi());
                    System.out.println(rs.getName() + " waiting for " + src2Reg.getName() + " from " + src2Reg.getQi());
                } else {
                    rs.setVk(src2Reg.getValue());
                    System.out.println(rs.getName() + " got " + src2Reg.getName() + " value: " + src2Reg.getValue());
                }
                
                System.out.println("Successfully issued " + instruction.instruction + " to " + rs.getName());
                return true;
            }
        }
        return false;
    }

    private boolean issueIntegerInstruction(InstructionQueue instruction) {
        System.out.println("Attempting to issue integer instruction: " + instruction.instruction);
        // Integer operations use the Add/Sub reservation stations
        for (ReservationStation rs : addSubRS) {
            if (!rs.isBusy()) {
                rs.setBusy(true);
                rs.setOp(instruction.instruction);
                
                Register destReg = getRegisterByName(instruction.d);
                Register src1Reg = getRegisterByName(instruction.j);
                
                System.out.println("Source register " + src1Reg.getName() + " value: " + src1Reg.getValue());
                
                destReg.setQi(rs.getName());
                
                if (!src1Reg.getQi().isEmpty()) {
                    rs.setQj(src1Reg.getQi());
                    System.out.println("Source register " + src1Reg.getName() + " is waiting for " + src1Reg.getQi());
                } else {
                    rs.setVj(src1Reg.getValue());
                    System.out.println("Set Vj to " + src1Reg.getValue() + " from register " + src1Reg.getName());
                }
                
                // For immediate value
                try {
                    double immediate = Double.parseDouble(instruction.k);
                    rs.setVk(immediate);
                    System.out.println("Set Vk to immediate value: " + immediate);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid immediate value: " + instruction.k);
                    return false;
                }
                
                System.out.println("Successfully issued " + instruction.instruction + " to " + rs.getName());
                return true;
            }
        }
        return false;
    }

    private boolean issueBranchInstruction(InstructionQueue instruction) {
        // Branch operations use the Add/Sub reservation stations for comparison
        for (ReservationStation rs : addSubRS) {
            if (!rs.isBusy()) {
                rs.setBusy(true);
                rs.setOp(instruction.instruction);
                
                Register src1Reg = getRegisterByName(instruction.j);
                Register src2Reg = getRegisterByName(instruction.k);
                
                if (!src1Reg.getQi().isEmpty()) {
                    rs.setQj(src1Reg.getQi());
                } else {
                    rs.setVj(src1Reg.getValue());
                }
                
                if (!src2Reg.getQi().isEmpty()) {
                    rs.setQk(src2Reg.getQi());
                } else {
                    rs.setVk(src2Reg.getValue());
                }
                
                return true;
            }
        }
        return false;
    }

    private void executeReservationStations() {
        // Execute load buffers first since others may depend on them
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy()) {
                System.out.println("\nExecuting " + lb.getName());
                lb.execute(memory, cache);
            }
        }

        // Execute Add/Sub reservation stations
        for (ReservationStation rs : addSubRS) {
            if (rs.isBusy()) {
                if (rs.getQj().isEmpty() && rs.getQk().isEmpty()) {  // Has all operands
                    if (!rs.isExecuting()) {
                        // Start execution
                        System.out.println("Starting execution of " + rs.getName() + ": " + rs.getOp());
                        rs.setExecuting(true);
                        rs.setCyclesLeft(rs.getLatency());
                    }
                    
                    if (rs.isExecuting()) {
                        System.out.println("Executing " + rs.getName() + ": " + rs.getOp() + " (cycles left: " + rs.getCyclesLeft() + ")");
                        rs.execute();
                    }
                } else {
                    System.out.println(rs.getName() + " waiting for operands: Qj=" + rs.getQj() + ", Qk=" + rs.getQk());
                }
            }
        }
        
        // Execute Mul/Div reservation stations
        for (ReservationStation rs : mulDivRS) {
            if (rs.isBusy()) {
                if (rs.getQj().isEmpty() && rs.getQk().isEmpty()) {  // Has all operands
                    if (!rs.isExecuting()) {
                        // Start execution
                        System.out.println("Starting execution of " + rs.getName() + ": " + rs.getOp());
                        rs.setExecuting(true);
                        rs.setCyclesLeft(rs.getLatency());
                    }
                    
                    if (rs.isExecuting()) {
                        System.out.println("Executing " + rs.getName() + ": " + rs.getOp() + " (cycles left: " + rs.getCyclesLeft() + ")");
                        rs.execute();
                    }
                } else {
                    System.out.println(rs.getName() + " waiting for operands: Qj=" + rs.getQj() + ", Qk=" + rs.getQk());
                }
            }
        }
        
        // Execute store buffers with cache
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy() && sb.getQ().isEmpty()) {  // Make sure we have the value to store
                if (!sb.isExecuting()) {
                    // Start execution
                    System.out.println("Starting execution of " + sb.getName());
                    sb.setExecuting(true);
                    
                    // Write to both cache and memory immediately since we have the value
                    System.out.println("Writing value " + sb.getValue() + " to address " + sb.getAddress());
                    cache.write(sb.getAddress(), sb.getValue());
                    memory.write(sb.getAddress(), sb.getValue());
                    sb.setExecuting(false);  // Mark as done immediately
                }
            } else if (sb.isBusy()) {
                System.out.println(sb.getName() + " waiting for value from " + sb.getQ());
            }
        }
    }

    private void performWriteBack() {
        // Write back results from load buffers first
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy() && !lb.isExecuting()) {  
                System.out.println("\nWriting back from " + lb.getName() + " result=" + lb.getValue());
                writeResult(lb.getName(), lb.getValue());
                lb.setBusy(false);
            }
        }
        
        // Write back results from store buffers
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy() && !sb.isExecuting()) {
                System.out.println("\nWriting back from " + sb.getName());
                sb.setBusy(false);
            }
        }
        
        // Write back results from add/sub reservation stations
        for (ReservationStation rs : addSubRS) {
            if (rs.isBusy() && !rs.isExecuting() && rs.getQj().isEmpty() && rs.getQk().isEmpty()) {  
                System.out.println("\nWriting back from " + rs.getName() + " result=" + rs.getResult());
                writeResult(rs.getName(), rs.getResult());
                rs.setBusy(false);
            }
        }
        
        // Write back results from mul/div reservation stations
        for (ReservationStation rs : mulDivRS) {
            if (rs.isBusy() && !rs.isExecuting() && rs.getQj().isEmpty() && rs.getQk().isEmpty()) {  
                System.out.println("\nWriting back from " + rs.getName() + " result=" + rs.getResult());
                writeResult(rs.getName(), rs.getResult());
                rs.setBusy(false);
            }
        }
    }

    private boolean allInstructionsComplete() {
        // Check instruction queue
        if (!instructionQueue.isEmpty()) {
            return false;
        }
        
        // Check all units
        for (ReservationStation rs : addSubRS) {
            if (rs.isBusy()) return false;
        }
        for (ReservationStation rs : mulDivRS) {
            if (rs.isBusy()) return false;
        }
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy()) return false;
        }
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy()) return false;
        }
        
        return true;
    }

    private void writeResult(String source, double result) {
        System.out.println("\nWriting result from " + source + ": " + result);
        
        // Update registers
        for (Register reg : registers) {
            if (reg.getQi().equals(source)) {
                System.out.println("Updating register " + reg.getName() + " with value " + result);
                reg.setValue(result);
                reg.setQi("");  // Clear the dependency
                System.out.println("Register " + reg.getName() + " now has value " + reg.getValue());
            }
        }
        
        // Update reservation stations
        List<ReservationStation> allRS = new ArrayList<>();
        allRS.addAll(addSubRS);
        allRS.addAll(mulDivRS);
        
        for (ReservationStation rs : allRS) {
            if (rs.getQj().equals(source)) {
                System.out.println("Updating " + rs.getName() + " Vj with " + result + " (was waiting for " + source + ")");
                rs.setQj("");  // Clear the dependency
                rs.setVj(result);
            }
            if (rs.getQk().equals(source)) {
                System.out.println("Updating " + rs.getName() + " Vk with " + result + " (was waiting for " + source + ")");
                rs.setQk("");  // Clear the dependency
                rs.setVk(result);
            }
            
            // If this station now has all operands, it can start executing
            if (rs.isBusy() && !rs.isExecuting() && rs.getQj().isEmpty() && rs.getQk().isEmpty()) {
                System.out.println(rs.getName() + " now has all operands (Vj=" + rs.getVj() + ", Vk=" + rs.getVk() + ") and can begin execution");
                rs.setExecuting(true);
                rs.setCyclesLeft(rs.getLatency());
            }
        }
        
        // Update store buffers
        for (StoreBuffer sb : storeBuffers) {
            if (sb.getQ().equals(source)) {
                System.out.println("Updating " + sb.getName() + " value with " + result);
                sb.setQ("");  // Clear the dependency
                sb.setValue(result);
            }
        }
    }

    private Register getRegisterByName(String name) {
        System.out.println("Looking for register: " + name);
        if (name == null || name.isEmpty()) {
            System.out.println("Invalid register name: null or empty");
            return null;
        }
        
        // Remove any 'F' prefix if present
        name = name.toUpperCase().replace("F", "");
        
        try {
            int regNum = Integer.parseInt(name);
            int index = regNum / 2;  // Convert register number to array index
            
            if (index >= 0 && index < registers.length) {
                System.out.println("Found register F" + regNum + " at index " + index);
                return registers[index];
            } else {
                System.out.println("Register index out of bounds: " + index);
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid register number format: " + name);
            return null;
        }
    }

    private void resetSimulation() {
        currentCycle = 0;
        isExecuting = true;
        pauseRequested = false;
        memory = new Memory();  // Reset memory first
        memory.reset();  // Initialize memory with test values
        
        // Initialize cache with user-specified parameters
        try {
            int cacheSize = Integer.parseInt(cacheSizeField.getText().trim());
            int blockSize = Integer.parseInt(blockSizeField.getText().trim());
            cache = new Cache(cacheSize, blockSize);
            // Pre-load the blocks we need
            cache.loadBlock(0, memory);  // Load block containing address 0
            cache.loadBlock(4, memory);  // Load block containing address 4
        } catch (NumberFormatException e) {
            System.err.println("Invalid cache parameters, using defaults");
            cache = new Cache(64, 16);  // Default values
            // Pre-load the blocks we need
            cache.loadBlock(0, memory);  // Load block containing address 0
            cache.loadBlock(4, memory);  // Load block containing address 4
        }
        
        // Reset registers
        for (Register reg : registers) {
            reg.reset();
        }
        
        // Reset stations and buffers
        for (ReservationStation rs : addSubRS) rs.reset();
        for (ReservationStation rs : mulDivRS) rs.reset();
        for (LoadBuffer lb : loadBuffers) lb.reset();
        for (StoreBuffer sb : storeBuffers) sb.reset();
        
        // Clear instruction queue
        instructionQueue.clear();
    }

    private void updateStatus() {
        StringBuilder status = new StringBuilder();
        
        status.append("=== Cycle ").append(currentCycle).append(" ===\n\n");
        
        status.append("Instruction Queue:\n");
        if (instructionQueue.isEmpty()) {
            status.append("(empty)\n");
        } else {
            for (InstructionQueue inst : instructionQueue) {
                status.append(String.format("%s %s, %s, %s\n", 
                    inst.instruction, inst.d, inst.j, inst.k));
            }
        }
        status.append("\n");
        
        status.append("Registers:\n");
        for (Register reg : registers) {
            status.append(String.format("F%d: Value=%.2f, Qi=%s\n",
                Arrays.asList(registers).indexOf(reg) * 2,
                reg.getValue(), 
                reg.getQi().isEmpty() ? "(ready)" : reg.getQi()));
        }
        status.append("\n");
        
        status.append("Add/Sub Reservation Stations:\n");
        boolean hasAddSub = false;
        for (ReservationStation rs : addSubRS) {
            if (rs.isBusy()) {
                hasAddSub = true;
                status.append(String.format("%s: Op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s%s\n",
                    rs.getName(), rs.getOp(), rs.getVj(), rs.getVk(), 
                    rs.getQj().isEmpty() ? "(ready)" : rs.getQj(),
                    rs.getQk().isEmpty() ? "(ready)" : rs.getQk(),
                    rs.isExecuting() ? " [Executing]" : ""));
            }
        }
        if (!hasAddSub) status.append("(none busy)\n");
        status.append("\n");
        
        status.append("Mul/Div Reservation Stations:\n");
        boolean hasMulDiv = false;
        for (ReservationStation rs : mulDivRS) {
            if (rs.isBusy()) {
                hasMulDiv = true;
                status.append(String.format("%s: Op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s%s\n",
                    rs.getName(), rs.getOp(), rs.getVj(), rs.getVk(), 
                    rs.getQj().isEmpty() ? "(ready)" : rs.getQj(),
                    rs.getQk().isEmpty() ? "(ready)" : rs.getQk(),
                    rs.isExecuting() ? " [Executing]" : ""));
            }
        }
        if (!hasMulDiv) status.append("(none busy)\n");
        status.append("\n");
        
        status.append("Load Buffers:\n");
        boolean hasLoad = false;
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy()) {
                hasLoad = true;
                status.append(String.format("%s: Address=%d%s\n",
                    lb.getName(), lb.getAddress(),
                    lb.isExecuting() ? " [Executing]" : ""));
            }
        }
        if (!hasLoad) status.append("(none busy)\n");
        status.append("\n");
        
        status.append("Store Buffers:\n");
        boolean hasStore = false;
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy()) {
                hasStore = true;
                status.append(String.format("%s: Address=%d, V=%.2f, Q=%s%s\n",
                    sb.getName(), sb.getAddress(), sb.getValue(),
                    sb.getQ().isEmpty() ? "(ready)" : sb.getQ(),
                    sb.isExecuting() ? " [Executing]" : ""));
            }
        }
        if (!hasStore) status.append("(none busy)\n");
        
        // Add cache status
        status.append("Cache Status:\n");
        status.append(String.format("Size: %d bytes, Block Size: %d bytes\n",
            cache.getCacheSize(), cache.getBlockSize()));
        status.append(String.format("Hits: %d, Misses: %d\n\n",
            cache.getHits(), cache.getMisses()));
        
        Platform.runLater(() -> {
            statusArea.setText(status.toString());
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}