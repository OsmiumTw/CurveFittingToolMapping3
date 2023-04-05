import com.cjToolbox.Preprocessing.DataOperate;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.Arrays;

/**
 * 輸出的格式均為[強度,波長,半高寬,背景值]
 */
public class Fit {

    public enum Method {
        StandardGaussFit,
        NumericalAnalysis,
        PatternSearch
    }

    public enum BackgroundDetectType {
        Mean,
        Median,
        Mode,
        Min
    }

    private BackgroundDetectType DetectType = BackgroundDetectType.Mode;
    private Method FitMethod = Method.StandardGaussFit;

    public Fit() {
    }

    /**
     *
     * @param x
     * @param y
     * @param originY 原始的y (row data)，用來計算背景值
     * @return
     * @throws Exception
     */
    public double[] autoFit(double[] x, double[] y, double[] originY) throws Exception {
        double[] output;
        switch (FitMethod) {
            case StandardGaussFit:
                output = StandardGaussFit(x,y, originY);
                break;
            case NumericalAnalysis:
                output = NumericalAnalysis(x,y, originY);
                break;
            case PatternSearch:
                output = PatternSearch(x,y);
                break;
            default:
                throw new Exception("發生例外的狀況");
        }
        return output;
    }

    public void setDetectType(BackgroundDetectType type) {
        DetectType = type;
    }

    public BackgroundDetectType getDetectType() {
        return DetectType;
    }

    public void setFitMethod(Method method) {
        FitMethod = method;
    }

    private void xyCheck(double[] x, double[] y) throws Exception{
        if (x.length != y.length) {
            throw new Exception("x和y的長度不同");
        }
        if (x.length == 0) {
            throw new Exception("x或y的長度不可為0");
        }
    }

    private double getBackground(double[] originY) throws Exception{
        switch (DetectType) {
            case Mean:
                return DataOperate.getMean(originY);
            case Mode:
                return DataOperate.getMode(originY);
            case Median:
                return DataOperate.getQuartile(originY,50);
            case Min:
                return DataOperate.getMin(originY);
            default:
                throw new Exception("發生例外的狀況");
        }
    }

    private double[] StandardGaussFit(double[] x, double[] y, double[] originY) throws Exception{
        xyCheck(x,y);
        GaussianCurveFitter fitter = GaussianCurveFitter.create();
        WeightedObservedPoints obs = new WeightedObservedPoints();
        double background = getBackground(originY);
        for (int j = 0; j < x.length; j++) {
            obs.add(x[j],y[j] - background);
        }
        double[] output = fitter.fit(obs.toList());
        return new double[]{output[0],output[1] ,output[2] * 2 * Math.sqrt(Math.log(4)), getBackground(originY)};
    }

    private double[] NumericalAnalysis(double[] x, double[] y, double[] originY) throws Exception{
        xyCheck(x,y);
        double background = getBackground(originY);
        y = Arrays.stream(y).parallel().map(i -> i -background).toArray();
        double maxX = x[0];
        double maxY = y[0];
        int count = 0;
        for (int i = 1; i < x.length; i++) {
            if (y[i] > maxY) {
                maxX = x[i];
                maxY = y[i];
                count = i;
            }
        }
        double halfY = maxY / 2.0;
        double minDiffY1 = Math.abs(y[0] - halfY);
        double minDiffY2 = Math.abs(y[count] - halfY);
        double x1 = x[0];
        double x2 = x[count];
        double minDiffY;
        for (int i = 0; i < x.length; i++) {
            minDiffY = Math.abs(y[i] - halfY);
            if (i <= count) {
                if (minDiffY < minDiffY1) {
                    minDiffY1 = minDiffY;
                    x1 = x[i];
                }
            }
            if (i >= count) {
                if (minDiffY < minDiffY2) {
                    minDiffY2 = minDiffY;
                    x2 = x[i];
                }
            }
        }
        return new double[]{maxY, maxX, x2 - x1, background};
    }

    private double[] PatternSearch(double[] x, double[] y) throws Exception {
        xyCheck(x,y);
        PatternSearch patternSearch = new PatternSearch(x,y);
        patternSearch.setMaxTime(2000);
        patternSearch.setFunctionTolerance(1E-6);
        patternSearch.setMeshContractionFactor(0.5);
        patternSearch.setMeshExpansionFactor(2.0);
        patternSearch.setStepTolerance(1E-6);
        return patternSearch.fit();
    }
}

