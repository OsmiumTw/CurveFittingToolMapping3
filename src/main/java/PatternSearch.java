import com.cjToolbox.Preprocessing.DataOperate;

import java.util.Arrays;

/**
 * Pattern Search Optimization
 */
public class PatternSearch {

    /*
    a = intensity
    b = central wavelength
    c = width
    d = background
     */

    private int MaxTime = 2000;
    private double FunctionTolerance = 1E-6;
    private double StepTolerance = 1E-6;
    private double MeshTolerance = 1E-6;
    private double MeshExpansionFactor = 2.0;
    private double MeshContractionFactor = 0.5;
    private double[] aRange;
    private double[] bRange;
    private double[] cRange;
    private double[] dRange;
    private final double[] xData;
    private final double[] yData;

    public PatternSearch(double[] xData, double[] yData) throws Exception{
        this.xData = xData;
        this.yData = yData;
        if (xData.length != yData.length) {
            throw new Exception("x和y長度不同");
        }
    }

    /**
     * 最大計算次數
     * @param maxTime
     * @throws Exception
     */
    public void setMaxTime(int maxTime) throws Exception{
        if (maxTime <= 0.0) {
            throw new Exception("最大計算次數必須大於0");
        }
        MaxTime = maxTime;
    }

    /**
     * 函數值小於這個值時將停止
     * @param functionTolerance
     */
    public void setFunctionTolerance(double functionTolerance) throws Exception{
        if (functionTolerance < 0.0) {
            throw new Exception("函數值必須大於等於0");
        }
        FunctionTolerance = functionTolerance;
    }

    /**
     * pattern的縮小係數
     * @param meshContractionFactor
     */
    public void setMeshContractionFactor(double meshContractionFactor) throws Exception{
        if (meshContractionFactor >= 1.0) {
            throw new Exception("縮小係數必須小於1");
        }
        if (meshContractionFactor <= 0.0) {
            throw new Exception("縮小係數必須大於0");
        }
        MeshContractionFactor = meshContractionFactor;
    }

    /**
     * pattern的放大係數
     * @param meshExpansionFactor
     */
    public void setMeshExpansionFactor(double meshExpansionFactor) throws Exception{
        if (meshExpansionFactor < 1.0) {
            throw new Exception("放大係數必須大於等於1");
        }
        MeshExpansionFactor = meshExpansionFactor;
    }

    /**
     * 網格小於這個值時將停止
     * @param meshTolerance
     */
    public void setMeshTolerance(double meshTolerance) throws Exception{
        if (meshTolerance < 0.0) {
            throw new Exception("網格必須大於等於0");
        }
        MeshTolerance = meshTolerance;
    }

    /**
     * 上一次最佳函數值與這一次最佳函數值的差小於這個值將停止
     * @param stepTolerance
     */
    public void setStepTolerance(double stepTolerance) throws Exception{
        if (stepTolerance < 0.0) {
            throw new Exception("函數值差必須大於等於0");
        }
        StepTolerance = stepTolerance;
    }

    public double[] fit() {
        double meshSize = 2.0;
        double[] initParameter = guessParameter(xData,yData);
        setLimit(xData,yData);
        double[] globalParameter = initParameter; // 全部過程中最佳值
        double globalFitness = Integer.MAX_VALUE; // 全部過程中最小的函數值
        double currentFitness; // 一次while loop中的最小值
        double fitness; // 每一次計算的函數值
        double[][] pattern;
        int pointer; // 指向每一個for loop最小的那一次
        int count = 0;
        pattern = getPattern(initParameter[0],initParameter[1],initParameter[2],initParameter[3],meshSize);
        while (globalFitness > FunctionTolerance) {
            currentFitness = Integer.MAX_VALUE;
            pointer = -1;
            for (int i = 0; i < 8; i++) {
                if (parameterCheck(pattern[i][0], pattern[i][1], pattern[i][2], pattern[i][3])) {
                    fitness = getFitness(pattern[i][0],pattern[i][1],pattern[i][2],pattern[i][3],xData,yData);
                    if (fitness < currentFitness) {
                        currentFitness = fitness;
                        pointer = i;
                    }
                }
            }

            // 沒有符合的pattern時停止
            if (pointer == -1) {
                break;
            }

            // meshSize太小時停止
            if (meshSize < MeshTolerance) {
                break;
            }
            if (currentFitness < globalFitness) {
                // Step太小時停止
                if (Math.abs(currentFitness - globalFitness) < StepTolerance) {
                    globalParameter = pattern[pointer];
                    break;
                }
                globalParameter = pattern[pointer];
                globalFitness = currentFitness;
                meshSize *= MeshExpansionFactor;
            } else {
                meshSize *= MeshContractionFactor;
            }
            count++;
            if (count < MaxTime) {
                pattern = getPattern(globalParameter[0],globalParameter[1],globalParameter[2],globalParameter[3],meshSize);
            } else {
                break;
            }
        }
        return new double[] {globalParameter[0],globalParameter[1],globalParameter[2] * 2.0 * Math.sqrt(Math.log(4.0)),globalParameter[3]};
    }

    /**
     * 得到下一個迭代的pattern
     * @param a 當前最佳a
     * @param b 當前最佳b
     * @param c 當前最佳c
     * @param d 當前最佳d
     * @param meshSize
     * @return
     */
    private double[][] getPattern(double a, double b, double c, double d, double meshSize) {
        double[][] output = new double[8][4];
        for (int i = 0; i < 8; i++) {
            if (i <= 3) {
                output[i][i] = 1;
            } else {
                output[i][i - 4] = -1;
            }
            output[i][0] = a + meshSize * output[i][0];
            output[i][1] = b + meshSize * output[i][1];
            output[i][2] = c + meshSize * output[i][2];
            output[i][3] = d + meshSize * output[i][3];
        }
        return output;
    }

    /**
     * 取得函數值
     * @param pattern_a
     * @param pattern_b
     * @param pattern_c
     * @param pattern_d
     * @param x
     * @param y
     * @return
     */
    private double getFitness(double pattern_a, double pattern_b, double pattern_c, double pattern_d, double[] x, double[] y) {
        double[] fValue = Arrays.stream(x).parallel().map(i -> pattern_a * Math.exp(- 1.0 * (i - pattern_b) * (i - pattern_b) / 2.0 / pattern_c / pattern_c) + pattern_d).toArray();
        double norm = 0.0;
        for (int i = 0; i < fValue.length; i++) {
            norm = norm + (y[i] - fValue[i]) * (y[i] - fValue[i]);
        }
        return Math.sqrt(norm);
    }

    /**
     * 猜起始值
     * @param x
     * @param y
     * @return
     */
    private double[] guessParameter(double[] x, double[] y) {
        double[] output = new double[4];
        output[3] = DataOperate.getMin(y);
        double[] backgroundRemovedY = Arrays.stream(y).map(i -> i - output[3]).toArray();
        output[0] = DataOperate.getMax(backgroundRemovedY);
        int maxYIndex = 0;
        {
            double diff1 = Math.abs(backgroundRemovedY[0] - output[0]);
            double diff2;
            for (int i = 1; i < backgroundRemovedY.length; i++) {
                diff2 = Math.abs(backgroundRemovedY[i] - output[0]);
                if (diff2 < diff1) {
                    diff1 = diff2;
                    maxYIndex = i;
                }
            }
            output[1] = x[maxYIndex];
        }
        {
            double halfIntensity = output[0] / 2.0;
            double diff1 = Math.abs(backgroundRemovedY[0] - halfIntensity);
            double diff2 = Math.abs(backgroundRemovedY[maxYIndex] - halfIntensity);
            int halfXIndex1 = 0;
            int halfXIndex2 = maxYIndex;
            double diff;
            for (int i = 1; i < backgroundRemovedY.length; i++) {
                diff = Math.abs(backgroundRemovedY[i] - halfIntensity);
                if (i < maxYIndex) {
                    if (diff < diff1) {
                        diff1 = diff;
                        halfXIndex1 = i;
                    }
                } else {
                    if (diff < diff2) {
                        diff2 = diff;
                        halfXIndex2 = i;
                    }
                }
            }
            output[2] = x[halfXIndex2] - x[halfXIndex1];
        }
        return output;
    }

    /**
     * 設定搜尋範圍
     * @param x
     * @param y
     */
    private void setLimit(double[] x, double[] y) {
        double maxX = x[x.length - 1];
        double minX = x[0];
        double maxY = DataOperate.getMax(y);
        aRange = new double[] {-maxY * 5, maxY * 5};
        bRange = new double[] {minX, maxX};
        cRange = new double[] {0.0, maxX - minX};
        dRange = aRange;
    }

    /**
     * 檢查這個pattern可不可用
     * @param pattern_a
     * @param pattern_b
     * @param pattern_c
     * @param pattern_d
     * @return
     */
    private boolean parameterCheck(double pattern_a, double pattern_b, double pattern_c, double pattern_d) {
        if (pattern_a < aRange[0] || pattern_a > aRange[1]) {
            return false;
        }
        if (pattern_b < bRange[0] || pattern_b > bRange[1]) {
            return false;
        }
        if (pattern_c < cRange[0] || pattern_c > cRange[1]) {
            return false;
        }
        if (pattern_d < dRange[0] || pattern_d > dRange[1]) {
            return false;
        }
        return true;
    }
}
