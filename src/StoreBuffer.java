package src;

public class StoreBuffer {
    private String name;
    private boolean busy;
    private int address;
    private double value;
    private String Q;
    private boolean executing;

    public StoreBuffer(String name) {
        this.name = name;
        reset();
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getQ() {
        return Q;
    }

    public void setQ(String q) {
        this.Q = q;
    }

    public boolean isExecuting() {
        return executing;
    }

    public void setExecuting(boolean executing) {
        this.executing = executing;
    }

    public String getName() {
        return name;
    }

    public void reset() {
        busy = false;
        address = 0;
        value = 0;
        Q = "";
        executing = false;
    }
}
