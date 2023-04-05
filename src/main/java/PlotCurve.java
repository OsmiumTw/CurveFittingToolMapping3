package MyComponent;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.jfree.chart.*;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

public class PlotCurve {
    private final JPanel Panel;
    private XYSeriesCollection Dataset;
    private JFreeChart Chart;
    private ChartPanel ChartPanel;
    private XYSeries Series;
    private Crosshair X1Crosshair;
    private Crosshair X2Crosshair;
    private ChartMouseListener ChartMouseListener;
    private MouseWheelListener MouseWheelListener;
    private ArrayList<double[]> FitRange;
    private double Distance = 10.0;
    private int RangeCount = 1;
    private int RangeAmount = Integer.MAX_VALUE;
    private boolean hasFitCurve = false;




    public PlotCurve(JPanel panel, String chartTitle) {
        this.Panel = panel;
        initChart(chartTitle,panel);
        initCrossHair();
    }

    /**
     * 刪除所有圖上的曲線，畫原始數據
     * @param xData
     * @param yData
     * @throws Exception
     */
    public void setXYData(double[] xData, double[] yData) throws Exception {
        if (xData.length != yData.length) {
            throw new Exception("x和y的長度不同");
        }
        if (xData.length == 0) {
            throw new Exception("x或y的長度不可為0");
        }
        Series = new XYSeries("Raw data");
        Series.setDescription("Raw data");
        for (int i = 0; i < xData.length; i++) {
            Series.add(xData[i],yData[i]);
        }
        if (Dataset.getSeriesCount() != 0) {
            Dataset.removeAllSeries();
        }
        Dataset.addSeries(Series);
        Chart.getXYPlot().getDomainAxis().setRange(Series.getMinX(), Series.getMaxX());
        Chart.getXYPlot().getRangeAxis().setRange(Series.getMinY(), Series.getMaxY());
        setLine();
    }

    /**
     * 畫契合曲線
     * @param fitResult 契合結果
     * @throws Exception
     */
    public void setXYData(ArrayList<double[]> fitResult) throws Exception {
        double[] result;
        double[][] fitCurve;
        double[] x;
        double[] y;
        XYSeries series;
        for (int i = 0; i < fitResult.size(); i++) {
            result = fitResult.get(i);
            fitCurve = getFitCurve(result[0],result[1],result[2],result[3]);
            x = fitCurve[0];
            y = fitCurve[1];
            series = new XYSeries("Fit" + (i + 1));
            series.setDescription("Fit");
            for (int j = 0; j < x.length; j++) {
                series.add(x[j],y[j]);
            }
            Dataset.addSeries(series);
        }
        hasFitCurve = true;
        setLine();
    }

    /**
     * 畫契合曲線
     * @param fitResult 契合結果
     * @throws Exception
     */
    public void setXYData(double[][] fitResult) throws Exception {
        ArrayList<double[]> result = new ArrayList<>(Arrays.asList(fitResult));
        setXYData(result);
    }

    /**
     * 畫選擇的範圍的曲線
     */
    public void setXYPlot(double[][] fittingRange) {
        double x1;
        double x2;
        double xPoint;
        double yPoint;
        XYSeries series;
        for (int i = 0; i < fittingRange.length; i++) {
            x1 = fittingRange[i][0];
            x2 = fittingRange[i][1];
            series = new XYSeries("Range" + (i + 1));
            series.setDescription("Range");
            for (int j = 0; j < Series.getItemCount(); j++) {
                xPoint = Series.getX(j).doubleValue();
                yPoint = Series.getY(j).doubleValue();
                if (xPoint >= x1 && xPoint <= x2) {
                    series.add(xPoint,yPoint);
                }
            }
            Dataset.addSeries(series);
        }
        setLine();
    }

    /**
     * 設定範圍的數量
     * @param rangeAmount 範圍數量，預設是Integer.MAX_VALUE
     * @throws Exception
     */
    public void setRangeAmount(int rangeAmount) throws Exception{
        if (rangeAmount <= 0) {
            throw new Exception("最少要有一個範圍");
        }
        RangeAmount = rangeAmount;
    }

    public boolean hasData() {
        return Dataset.getSeriesCount() != 0;
    }

    /**
     * 如果有除了原始x和y之外的曲線將會回傳ture
     * @return
     */
    public boolean hasOtherCurve() {
        return Dataset.getSeriesCount() != 1;
    }

    public void clear() {
        Dataset.removeAllSeries();
        FitRange.clear();
    }

    public void enableCrossHair(boolean enable) {
        X1Crosshair.setVisible(enable);
        X2Crosshair.setVisible(enable);
        if (enable) {
            initChartMouseListener();
        } else {
            ChartPanel.removeChartMouseListener(ChartMouseListener);
            Panel.removeMouseWheelListener(MouseWheelListener);
        }

    }

    public double[][] getFitRange() {
        double[][] output = new double[FitRange.size()][2];
        for (int i = 0; i < output.length; i++) {
            output[i] = FitRange.get(i);
        }
        return output;
    }

    /**
     * 清理圖中所有契合曲線
     */
    public void clearFitCurve() {
        if (hasFitCurve) {
            int count = Dataset.getSeriesCount();
            XYSeries series;
            String seriesName;
            ArrayList<XYSeries> removedSeries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                series = Dataset.getSeries(i);
                seriesName = series.getDescription();
                if (seriesName.contains("Fit")) {
                    removedSeries.add(series);
                }
            }
            if (!removedSeries.isEmpty()) {
                for (int i = 0; i < removedSeries.size(); i++) {
                    Dataset.removeSeries(removedSeries.get(i));
                }
            }
            hasFitCurve = false;
        }
    }

    /**
     * 清理圖中除了原始x和y之外的曲線，並且清理FitRange
     * @throws Exception
     */
    public void clearFitRange(boolean clearAll) throws Exception {
        double[][] data = getXYData();
        double[] x = data[0];
        double[] y = data[1];
        setXYData(x,y);
        if (clearAll) {
            if (!FitRange.isEmpty()) {
                FitRange.clear();
                RangeCount = 1;
            }
        } else {
            if (!FitRange.isEmpty()) {
                FitRange.remove(FitRange.size() - 1);
                RangeCount--;
                setXYPlot(getFitRange());
            }
        }
    }

    /**
     * 取得圖中的x和y的數據
     * @return
     * @throws Exception
     */
    public double[][] getXYData() throws Exception{
        if (hasData()) {
            double[] x = new double[Series.getItemCount()];
            double[] y = x.clone();
            for (int i = 0; i < Series.getItemCount(); i++) {
                x[i] = Series.getX(i).doubleValue();
                y[i] = Series.getY(i).doubleValue();
            }
            return new double[][]{x,y};
        } else {
            throw new Exception("沒有數據");
        }
    }


    private void initChart(String chartTitle, JPanel panel) {
        FitRange = new ArrayList<>();
        Dataset = new XYSeriesCollection();
        Chart = ChartFactory.createXYLineChart(chartTitle,"Wavelength (nm)","Intensity (a.u)", Dataset, PlotOrientation.VERTICAL,true,true,false);
        ChartPanel = new ChartPanel(Chart);
        ChartPanel.setMouseZoomable(false);
        ChartPanel.setPopupMenu(null);
        panel.add(ChartPanel);
    }

    private void initCrossHair() {
        CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        X1Crosshair = new Crosshair(Integer.MAX_VALUE);
        X1Crosshair.setVisible(false);
        X1Crosshair.setLabelOutlinePaint(Color.BLACK);
        X1Crosshair.setLabelOutlineVisible(true);
        X1Crosshair.setLabelVisible(false);
        X1Crosshair.setLabelGenerator(crosshair -> String.valueOf(crosshair.getValue()));
        X2Crosshair = new Crosshair(Integer.MAX_VALUE);
        X2Crosshair.setVisible(false);
        X2Crosshair.setLabelOutlinePaint(Color.BLACK);
        X2Crosshair.setLabelOutlineVisible(true);
        X2Crosshair.setLabelVisible(false);
        X2Crosshair.setLabelGenerator(crosshair -> String.valueOf(crosshair.getValue()));
        crosshairOverlay.addDomainCrosshair(X1Crosshair);
        crosshairOverlay.addDomainCrosshair(X2Crosshair);
        ChartPanel.addOverlay(crosshairOverlay);
    }

    private void initChartMouseListener() {
        ChartMouseListener = new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                if (chartMouseEvent.getTrigger().getClickCount() == 2) {
                    if (FitRange.size() < RangeAmount) {
                        double xStart = X1Crosshair.getValue();
                        double xEnd = X2Crosshair.getValue();
                        if (xStart < Series.getMinX()) {
                            xStart = Series.getMinX();
                        }
                        if (xEnd > Series.getMaxX()) {
                            xEnd = Series.getMaxX();
                        }
                        FitRange.add(new double[]{xStart,xEnd});
                        XYSeries selectedSeries = new XYSeries("Range" + RangeCount);
                        selectedSeries.setDescription("Range");
                        RangeCount++;
                        double xValue;
                        double yValue;
                        for (int i = 0; i < Series.getItemCount(); i++) {
                            xValue = Series.getX(i).doubleValue();
                            if (xValue >= xStart && xValue <= xEnd) {
                                yValue = Series.getY(i).doubleValue();
                                selectedSeries.add(xValue,yValue);
                            }
                        }
                        Dataset.addSeries(selectedSeries);
                        setLine();
                    } else {
                        JOptionPane.showMessageDialog(Panel.getParent(),"一次只能選擇一個範圍","ChartMouseListener",JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {
                Rectangle2D dataArea = ChartPanel.getScreenDataArea();
                int currentXPos = chartMouseEvent.getTrigger().getX();
                double newX1 = Chart.getXYPlot().getDomainAxis().java2DToValue(currentXPos,dataArea, RectangleEdge.BOTTOM) - Distance;
                double newX2 = Chart.getXYPlot().getDomainAxis().java2DToValue(currentXPos,dataArea,RectangleEdge.BOTTOM) + Distance;
                X1Crosshair.setValue(newX1);
                X2Crosshair.setValue(newX2);
            }
        };
        MouseWheelListener = e -> {
            double scroll = e.getWheelRotation();
            if (scroll > 0) {
                Distance = Distance - 0.1;
                if (Distance <= 0.2) {
                    Distance = 0.1;
                } else {
                    X1Crosshair.setValue(X1Crosshair.getValue() + 0.1);
                    X2Crosshair.setValue(X2Crosshair.getValue() - 0.1);
                }

            } else {
                Distance = Distance + 0.1;
                X1Crosshair.setValue(X1Crosshair.getValue() - 0.1);
                X2Crosshair.setValue(X2Crosshair.getValue() + 0.1);
            }
        };
        ChartPanel.addChartMouseListener(ChartMouseListener);
        Panel.addMouseWheelListener(MouseWheelListener);
    }

    private void setLine() {
        XYPlot plot = Chart.getXYPlot();
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
        for (int i = 0; i < Dataset.getSeriesCount(); i++) {
            if (Dataset.getSeries(i).getDescription().equals("Raw Data")) {
                plot.getRenderer().setSeriesPaint(i,Color.BLUE);
                plot.getRenderer().setSeriesStroke(i,new BasicStroke(1));
            } else if (Dataset.getSeries(i).getDescription().equals("Fit")) {
                plot.getRenderer().setSeriesPaint(i,Color.RED);
                plot.getRenderer().setSeriesStroke(i,new BasicStroke(1));
            } else if (Dataset.getSeries(i).getDescription().equals("Range")) {
                plot.getRenderer().setSeriesPaint(i,Color.GREEN);
                plot.getRenderer().setSeriesStroke(i,new BasicStroke(3));
            } else {
                plot.getRenderer().setSeriesPaint(i,Color.BLACK);
            }
        }
    }

    /**
     * 用契合的結果和目前圖的xy來取得契合曲線
     * @param intensity
     * @param wavelength
     * @param fhwm
     * @param background
     * @return
     * @throws Exception
     */
    private double[][] getFitCurve(double intensity, double wavelength, double fhwm, double background) throws Exception {
        double w = fhwm / 2.0 / Math.sqrt(Math.log(4.0));
        Gaussian gaussian = new Gaussian(intensity,wavelength,w);
        double[][] data = getXYData();
        double[] x = data[0];
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            y[i] = gaussian.value(x[i]) + background;
        }
        return new double[][]{x,y};
    }
}
