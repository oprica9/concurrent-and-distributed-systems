package model;

import java.math.BigInteger;

public class Matrix {
    private final String name;
    private final BigInteger[][] matrix;
    private final int rows;
    private final int cols;
    private String location;

    public Matrix(String name, int rows, int cols, String matrixFileLocation) {
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.matrix = new BigInteger[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = BigInteger.ZERO;
            }
        }
        this.location = matrixFileLocation;
    }

    public Matrix(String name, BigInteger[][] product) {
        this.name = name;
        this.rows = product.length;
        this.cols = product[0].length;
        this.matrix = product;
    }

    public BigInteger[][] getMatrix() {
        return matrix;
    }

    public void insert(int i, int j, BigInteger value) {
        matrix[i][j] = value;
    }

    public BigInteger get(int i, int j) {
        return matrix[i][j];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getData() {
        return "matrix_name=" + name + ", rows=" + rows + ", cols=" + cols + ", location=" + location;
    }

    public void update(Matrix newMatrix) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                this.matrix[i][j] = newMatrix.getMatrix()[i][j];
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Matrix other)) return false; // Check for type compatibility

        // Compare rows and columns
        return this.name.equals(other.name) && this.rows == other.rows && this.cols == other.cols;
    }

    @Override
    public int hashCode() {
        int result = 17; // Random vrednost
        result = 31 * result + rows; // Random prost broj
        result = 31 * result + cols;
        return result;
    }

}
