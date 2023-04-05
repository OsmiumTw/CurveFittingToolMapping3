package MyComponent;

import java.util.concurrent.Callable;

public class FitAllData implements Callable<double[]> {
    private final Fit fit;
    private final double[] x;
    private final double[] y;
    private final double[] originY;

    @Override
    public double[] call() throws Exception {
        return fit.autoFit(x,y,originY);
    }

    public FitAllData(double[] x, double[] y, double[] originY, Fit fit) {
        this.x = x;
        this.y = y;
        this.originY = originY;
        this.fit = fit;
    }
}
