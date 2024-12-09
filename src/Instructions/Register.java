package src.Instructions;

public class Register {

    private double value;
    private String name;
    private String Qi;

    // Default constructor
    public Register() {
        this.value = 0;
        this.name = "";
        this.Qi = "";
    }

    // Constructor that accepts an initial value for the register
    public Register(double value) {
        this.value = value;  // Initialize the register value
        this.name = "";      // Default name (can be set later)
        this.Qi = "";        // Default Qi (can be set later)
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getQi() {
        return Qi;
    }

    public void setQi(String qi) {
        Qi = qi;
    }

    // Method to check if the register is ready (Qi is empty)
    public boolean regReady() {
        return Qi.equals("");  // If Qi is empty, the register is ready
    }

    // Method to update the value of the register
    public void updateRegister(double newVal) {
        this.value = newVal;
    }

    // Reset method to reset the register to its initial state
    public void reset() {
        this.value = 0.0; // Reset to 0 as default
        this.Qi = "";     // Clear any dependencies
    }
}
