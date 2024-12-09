package src;

import java.util.HashMap;

public class Cache {
    private int cacheSize;  // in bytes
    private int blockSize;  // in bytes
    private HashMap<Integer, CacheBlock> blocks;
    private int hits;
    private int misses;

    public Cache(int cacheSize, int blockSize) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.blocks = new HashMap<>(cacheSize / blockSize);
        this.hits = 0;
        this.misses = 0;
    }

    public boolean hasBlock(int address) {
        int blockNumber = address / blockSize;
        if (blocks.containsKey(blockNumber)) {
            hits++;
            return true;
        }
        misses++;
        return false;
    }

    public double read(int address) {
        int blockNumber = address / blockSize;
        int offset = address % blockSize;
        CacheBlock block = blocks.get(blockNumber);
        if (block == null) {
            throw new IllegalStateException("Attempting to read from non-existent cache block at address " + address);
        }
        return block.getData(offset);
    }

    public void write(int address, double value) {
        int blockNumber = address / blockSize;
        int offset = address % blockSize;
        CacheBlock block = blocks.get(blockNumber);
        if (block != null) {
            block.setData(offset, value);
        }
    }

    public void loadBlock(int address, Memory memory) {
        int blockNumber = address / blockSize;
        
        // If cache is full, remove oldest block
        if (blocks.size() >= cacheSize / blockSize) {
            blocks.remove(blocks.keySet().iterator().next());
        }

        // Create new block
        CacheBlock newBlock = new CacheBlock(blockSize);
        
        // Load data from memory
        int startAddress = blockNumber * blockSize;
        for (int i = 0; i < blockSize; i++) {
            newBlock.setData(i, memory.read(startAddress + i));
        }

        blocks.put(blockNumber, newBlock);
    }

    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public int getCacheSize() { return cacheSize; }
    public int getBlockSize() { return blockSize; }
}

class CacheBlock {
    private double[] data;

    public CacheBlock(int blockSize) {
        this.data = new double[blockSize];
    }

    public double getData(int offset) {
        return data[offset];
    }

    public void setData(int offset, double value) {
        data[offset] = value;
    }
}
