package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Random;

public class MatrixUtil {

    public static void printMatrix(Matrix matrix) {
        System.out.println(matrix.getName());
        for (int i = 0; i < matrix.getRows(); i++) {
            for (int j = 0; j < matrix.getCols(); j++) {
                System.out.printf("%10d", matrix.get(i, j));
            }
            System.out.println();
        }
    }

    public static void printMatrixCompare(Matrix matrix) {
        try (PrintWriter out = new PrintWriter(matrix.getName().toLowerCase() + ".txt")) {
            out.printf("matrix_name=%s, rows=%d, cols=%d\n", matrix.getName(), matrix.getRows(), matrix.getCols());
            for (int i = 0; i < matrix.getCols(); i++) {
                for (int j = 0; j < matrix.getRows(); j++) {
                    if (matrix.get(j, i).compareTo(BigInteger.ZERO) != 0)
                        out.printf("%d,%d = %s \n", j, i, matrix.get(j, i));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static File saveMatrix(Matrix matrix, String fileName) {
        try (PrintWriter out = new PrintWriter(fileName + ".rix")) {
            out.printf("matrix_name=%s, rows=%d, cols=%d\n", matrix.getName(), matrix.getRows(), matrix.getCols());
            for (int i = 0; i < matrix.getCols(); i++) {
                for (int j = 0; j < matrix.getRows(); j++) {
                    out.printf("%d,%d = %s \n", j, i, matrix.get(i, j));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return new File(fileName + ".rix");
    }

    public static void generateMatrixFile() {

        Random random = new Random();

        String name1 = "TEST1";
        int n = random.nextInt(1000, 3000);
        int m = random.nextInt(1000, 3000);

        try (PrintWriter out = new PrintWriter(name1.toLowerCase() + ".rix")) {
            out.printf("matrix_name=%s, rows=%d, cols=%d\n", name1, n, m);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    int r = random.nextInt(1, 71);
                    if (r % 11 == 0) {
                        out.printf("%d,%d = %s \n", j, i, random.nextInt(1000, 10000));
                    }

                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        String name2 = "TEST2";
        try (PrintWriter out = new PrintWriter(name2.toLowerCase() + ".rix")) {
            out.printf("matrix_name=%s, rows=%d, cols=%d\n", name2, m, n);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    int r = random.nextInt(1, 71);
                    if (r % 11 == 0) {
                        System.out.println(r);
                        out.printf("%d,%d = %s \n", j, i, random.nextInt(500, 5000));
                    }

                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

}
