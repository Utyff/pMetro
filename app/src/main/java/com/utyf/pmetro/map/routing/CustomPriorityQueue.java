package com.utyf.pmetro.map.routing;

/**
 * An efficient implementation of priority queue. Elements are stored as pairs of index and value.
 * Indices and values are stored as arrays instead of ArrayList to improve performance.
 */
class CustomPriorityQueue {
    private static final int INITIAL_CAPACITY = 16;

    private double[] values;  // elements are stored starting from index 1
    private int[] indices;  // elements are stored starting from index 1
    private int size;  // number of elements in the queue + 1

    /**
     * Creates empty priority queue with default capacity
     */
    public CustomPriorityQueue() {
        this.values = new double[INITIAL_CAPACITY];
        this.indices = new int[INITIAL_CAPACITY];
        this.size = 1;
    }

    /**
     * Adds element to the queue
     * @param index index of the element
     * @param value value of the element
     */
    public void add(int index, double value) {
        if (size >= indices.length)
            increaseCapacity();
        indices[size] = index;
        values[size] = value;
        size++;
        siftUp(size - 1);
    }

    /**
     * Gets and removes from the queue minimal element
     * @return index of element with the minimal value
     */
    public int poll() {
        int result = indices[1];
        indices[1] = indices[size - 1];
        values[1] = values[size - 1];
        size--;
        siftDown();
        return result;
    }

    /**
     * Checks if the queue is empty
     * @return true if the queue is empty
     */
    public boolean isEmpty() {
        return size == 1;
    }

    private void siftUp(int p) {
        int temp = indices[p];
        double tempValue = values[p];
        while (p > 1 && tempValue < values[p / 2]) {
            indices[p] = indices[p / 2];
            values[p] = values[p / 2];
            p = p / 2;
        }
        indices[p] = temp;
        values[p] = tempValue;
    }

    private void siftDown() {
        int p = 1;
        int temp = indices[p];
        double tempValue = values[p];
        while (true) {
            if (p * 2 >= size) {
                break;
            }
            else if (p * 2 + 1 >= size) {
                if (values[p * 2] < tempValue) {
                    indices[p] = indices[p * 2];
                    values[p] = values[p * 2];
                    p = p * 2;
                }
                break;
            }
            else if (values[p * 2] < tempValue &&
                    values[p * 2] <= values[p * 2 + 1]) {
                indices[p] = indices[p * 2];
                values[p] = values[p * 2];
                p = p * 2;
            }
            else if (values[p * 2 + 1] < tempValue &&
                    values[p * 2 + 1] <= values[p * 2]) {
                indices[p] = indices[p * 2 + 1];
                values[p] = values[p * 2 + 1];
                p = p * 2 + 1;
            }
            else {
                break;
            }
        }
        indices[p] = temp;
        values[p] = tempValue;
    }

    private void increaseCapacity() {
        int[] newIndices = new int[indices.length * 2];
        System.arraycopy(indices, 0, newIndices, 0, indices.length);
        indices = newIndices;
        double[] newDistances = new double[values.length * 2];
        System.arraycopy(values, 0, newDistances, 0, values.length);
        values = newDistances;
    }
}
