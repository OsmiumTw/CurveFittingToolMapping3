/*
 * Created by JFormDesigner on Sat Apr 01 22:13:22 CST 2023
 */

import com.cjToolbox.Preprocessing.FillOutliers;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author WCJ
 */
public class MainFrame extends JFrame {

    List<String> AllData;
    private final PlotCurve plotCurve;
    private final Fit fit;
    private double[] Wavelength;
    private double[] Intensity;
    private String FolderPath;
    private String FileName;
    private LinkedHashSet<Double> XPos;
    private LinkedHashSet<Double> YPos;
    private Thread FitAllDataThread;
    private ExecutorService Executor;


    private double[] RemoveOutliers(double[] y) throws Exception {
        switch (comboBox3.getSelectedIndex()) {
            case 0:
                return FillOutliers.MAD(y.clone(),5,1);
            case 1:
                return FillOutliers.BoxPlots(y.clone(),5,1);
            case 2:
                return FillOutliers.SD(y.clone(),5,1);
            case 3:
                return y;
            default:
                throw new Exception("檢測離散值時發生例外的狀況");
        }
    }

    private void UpdatePlotCurve() throws Exception{
        StringTokenizer tokenizer = new StringTokenizer(AllData.get((int) spinner1.getValue()),"\t");
        if (tokenizer.countTokens() - 2 != Wavelength.length) {
            throw new Exception("X與Y軸長度不同");
        }
        tokenizer.nextToken();
        tokenizer.nextToken();
        double[] intensity = new double[Wavelength.length];
        for (int i = 0; i < intensity.length; i++) {
            intensity[i] = Double.parseDouble(tokenizer.nextToken());
        }
        Intensity = RemoveOutliers(intensity);
        plotCurve.setXYData(Wavelength,Intensity);
    }

    private void OnceFit() {
        PanelONOFF(0,false);
        Timer timer = new Timer();
        Thread thread = new Thread(() -> {
            try {
                double[][] xy = plotCurve.getXYData();
                double[][] range = plotCurve.getFitRange();
                double[] x;
                double[] y;
                if (range.length == 0) {
                    x = xy[0];
                    y = xy[1];
                } else {
                    ArrayList<Double> temp1 = new ArrayList<>();
                    ArrayList<Double> temp2 = new ArrayList<>();
                    for (int i = 0; i < xy[0].length; i++) {
                        if (xy[0][i] >= range[0][0] && xy[0][i] <= range[0][1]) {
                            temp1.add(xy[0][i]);
                            temp2.add(xy[1][i]);
                        }
                    }
                    x = temp1.stream().mapToDouble(Double::doubleValue).toArray();
                    y = temp2.stream().mapToDouble(Double::doubleValue).toArray();
                }
                double[][] result = new double[][]{fit.autoFit(x,y ,xy[1])};
                label19.setText(String.valueOf(Math.round(result[0][0] * 1000.0) / 1000.0));
                label22.setText(String.valueOf(Math.round(result[0][1] * 1000.0) / 1000.0));
                label25.setText(String.valueOf(Math.round(result[0][2] * 1000.0) / 1000.0));
                label28.setText(String.valueOf(Math.round(result[0][3] * 1000.0) / 1000.0));
                label30.setText("數據分析");
                plotCurve.clearFitCurve();
                plotCurve.setXYData(result);
                timer.cancel();
                PanelONOFF(0,true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        thread.start();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (thread.isAlive()) {
                    thread.stop();
                    label19.setText("null");
                    label22.setText("null");
                    label25.setText("null");
                    label28.setText("null");
                    label30.setText("超時未收斂");
                    PanelONOFF(0,true);
                }
            }
        },2000);
    }

    private void PanelONOFF(int type, boolean enable) {
        switch (type) {
            case 0:
                // OnceFit
                button1.setEnabled(enable);
                spinner1.setEnabled(enable);
                comboBox1.setEnabled(enable);
                toggleButton1.setEnabled(enable);
                comboBox2.setEnabled(enable);
                comboBox3.setEnabled(enable);
                toggleButton2.setEnabled(enable);
                button2.setEnabled(enable);
                break;
            case 1:
                // Select fitting range
                button1.setEnabled(enable);
                spinner1.setEnabled(enable);
                comboBox1.setEnabled(enable);
                toggleButton1.setEnabled(enable);
                comboBox2.setEnabled(enable);
                comboBox3.setEnabled(enable);
                button3.setEnabled(!enable);
                button2.setEnabled(enable);
                break;
            case 2:
                // Fit All Data
                button1.setEnabled(enable);
                spinner1.setEnabled(enable);
                comboBox1.setEnabled(enable);
                toggleButton1.setEnabled(enable);
                toggleButton2.setEnabled(enable);
                comboBox2.setEnabled(enable);
                comboBox3.setEnabled(enable);
                button2.setEnabled(enable);
                button4.setEnabled(!enable);
                break;
        }
    }


    public MainFrame() {
        initComponents();
        plotCurve = new PlotCurve(panel13,null);
        fit = new Fit();
    }

    private void LoadFile(ActionEvent e) {
        // TODO add your code here
        FileDialog dialog = new FileDialog(this);
        dialog.setTitle("選擇檔案");
        dialog.setFilenameFilter((dir, name) -> name.endsWith(".txt"));
        dialog.setMode(FileDialog.LOAD);
        dialog.setMultipleMode(false);
        dialog.setVisible(true);
        if (!Objects.isNull(dialog.getFile())) {
            try {
                if (!dialog.getFile().toLowerCase().endsWith(".txt")) {
                    throw new Exception("檔案格式檢查器：只支援.txt的檔案格式");
                }
                plotCurve.clear();
                AllData = FileUtils.readLines(new File(dialog.getDirectory(),dialog.getFile()), StandardCharsets.UTF_8);
                XPos = new LinkedHashSet<>();
                YPos = new LinkedHashSet<>();
                ArrayList<Double> wavelength = new ArrayList<>();
                ArrayList<Double> intensity = new ArrayList<>();
                StringTokenizer tokenizer1 = new StringTokenizer(AllData.get(0),"\t");
                StringTokenizer tokenizer2 = new StringTokenizer(AllData.get(1),"\t");
                if (tokenizer1.countTokens() != tokenizer2.countTokens() - 2) {
                    throw new Exception("數據完整性檢查器：波長(波數)長度與強度不同");
                }
                tokenizer2.nextToken();
                tokenizer2.nextToken();
                while (tokenizer1.hasMoreTokens()) {
                    wavelength.add(Double.valueOf(tokenizer1.nextToken()));
                    intensity.add(Double.valueOf(tokenizer2.nextToken()));
                }
                StringTokenizer tokenizer;
                for (int i = 1; i < AllData.size(); i++) {
                    tokenizer = new StringTokenizer(AllData.get(i),"\t");
                    XPos.add(Double.valueOf(tokenizer.nextToken()));
                    YPos.add(Double.valueOf(tokenizer.nextToken()));
                }
                int count = XPos.size() * YPos.size();
                Wavelength = wavelength.stream().mapToDouble(Double::doubleValue).toArray();
                Intensity = RemoveOutliers(intensity.stream().mapToDouble(Double::doubleValue).toArray());
                FolderPath = dialog.getDirectory();
                FileName = dialog.getFile();
                plotCurve.setXYData(Wavelength,Intensity);
                spinner1.setModel(new SpinnerNumberModel(1,1,count,1));
                label11.setText(dialog.getFile());
                label13.setText((double) (new File(dialog.getDirectory(), dialog.getFile()).length() / 1024 / 1024) + " MB");
                label15.setText(String.valueOf(count));
                label30.setText("數據分析");
                double XRange = Math.round(Math.abs(XPos.stream().reduce(Math::max).orElseThrow() - XPos.stream().reduce(Math::min).orElseThrow()) * 1000.0) / 1000.0;
                double YRange = Math.round(Math.abs(YPos.stream().reduce(Math::max).orElseThrow() - YPos.stream().reduce(Math::min).orElseThrow()) * 1000.0) / 1000.0;
                label17.setText(XRange + " x " + YRange + "um2");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,ex.getMessage(),"LoadFile",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void ShowNextCurve(ChangeEvent e) {
        // TODO add your code here
        if (plotCurve.hasData()) {
            try {
                StringTokenizer tokenizer = new StringTokenizer(AllData.get((int) spinner1.getValue()),"\t");
                ArrayList<Double> intensity = new ArrayList<>();
                tokenizer.nextToken();
                tokenizer.nextToken();
                while (tokenizer.hasMoreTokens()) {
                    intensity.add(Double.valueOf(tokenizer.nextToken()));
                }
                if (intensity.size() != Wavelength.length) {
                    throw new Exception("數據完整性檢查器：波長(波數)長度與強度不同");
                }
                Intensity = RemoveOutliers(intensity.stream().mapToDouble(Double::doubleValue).toArray());
                plotCurve.setXYData(Wavelength,Intensity);
                if (toggleButton1.isSelected()) {
                    OnceFit();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,ex.getMessage(),"ShowNextCurve",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void DetectBackgroundAndOutlier(ActionEvent e) {
        // TODO add your code here
        switch (comboBox2.getSelectedIndex()) {
            case 0:
                fit.setDetectType(Fit.BackgroundDetectType.Mode);
                break;
            case 1:
                fit.setDetectType(Fit.BackgroundDetectType.Mean);
                break;
            case 2:
                fit.setDetectType(Fit.BackgroundDetectType.Median);
                break;
            case 3:
                fit.setDetectType(Fit.BackgroundDetectType.Min);
                break;
        }
        if (plotCurve.hasData()) {
            try {
                UpdatePlotCurve();
                if (toggleButton1.isSelected()) {
                    OnceFit();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,ex,"DetectBackgroundAndOutlier",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void SetFitType(ActionEvent e) {
        // TODO add your code here
        switch (comboBox1.getSelectedIndex()) {
            case 0:
                fit.setFitMethod(Fit.Method.StandardGaussFit);
                break;
            case 1:
                fit.setFitMethod(Fit.Method.NumericalAnalysis);
                break;
            case 2:
                fit.setFitMethod(Fit.Method.PatternSearch);
                break;
        }
        if (plotCurve.hasData()) {
            if (toggleButton1.isSelected()) {
                try {
                    OnceFit();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,ex,"SetFitType",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void AutoFitONOFF(ActionEvent e) {
        // TODO add your code here
        if (plotCurve.hasData()) {
            try {
                if (toggleButton1.isSelected()) {
                    OnceFit();
                } else {
                    double[][] xy = plotCurve.getXYData();
                    plotCurve.setXYData(xy[0],xy[1]);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,ex,"AutoFitOnOff",JOptionPane.ERROR_MESSAGE);
            }
        } else {
            toggleButton1.setSelected(false);
        }
    }

    private void SetFittingRange(ActionEvent e) {
        // TODO add your code here
        if (plotCurve.hasData()) {
            try {
                if (toggleButton2.isSelected()) {
                    toggleButton2.setText("設定完成");
                    PanelONOFF(1,false);
                    plotCurve.enableCrossHair(true);
                    plotCurve.setRangeAmount(1);
                    plotCurve.clearFitCurve();
                    if (plotCurve.getFitRange().length != 0) {
                        plotCurve.setXYPlot(plotCurve.getFitRange());
                    }
                } else {
                    toggleButton2.setText("設定契合範圍");
                    PanelONOFF(1,true);
                    plotCurve.enableCrossHair(false);
                    plotCurve.setXYData(Wavelength,Intensity);
                    if (toggleButton1.isSelected()) {
                        OnceFit();
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,ex,"SetFittingRange",JOptionPane.ERROR_MESSAGE);
            }
        } else {
            toggleButton2.setSelected(false);
        }
    }

    private void ClearFittingRange(ActionEvent e) {
        // TODO add your code here
        try {
            plotCurve.clearFitRange(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,ex,"ClearFittingRange",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void FitAllData(ActionEvent e) {
        // TODO add your code here
        if (plotCurve.hasData()) {
            FitAllDataThread = new Thread(() -> {
                try {
                    label30.setText("正在提交契合任務");
                    boolean fitDone = false;
                    Executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                    int pointCount = XPos.size() * YPos.size();
                    ArrayList<Future<double[]>> fitResultCollect = new ArrayList<>(pointCount);
                    Future<double[]> future;
                    Callable<double[]> callable;
                    int startIndex = -1;
                    int endIndex = -1 ;
                    int count;
                    StringTokenizer tokenizer;
                    double[] wavelength = Wavelength;
                    double[] intensity;
                    double[] x = new double[0];
                    double[] y;
                    for (int i = 1; i < pointCount + 1; i++) {
                        tokenizer = new StringTokenizer(AllData.get(i),"\t");
                        if (tokenizer.countTokens() != Wavelength.length + 2) {
                            throw new Exception("數據完整性檢查器：波長(波數)長度與強度不同");
                        }
                        tokenizer.nextToken();
                        tokenizer.nextToken();
                        count = 0;
                        intensity = new double[wavelength.length];
                        while (tokenizer.hasMoreTokens()) {
                            intensity[count] = Double.parseDouble(tokenizer.nextToken());
                            count++;
                        }
                        if (i == 1) {
                            double[][] range = plotCurve.getFitRange();
                            for (int j = 0; j < wavelength.length; j++) {
                                if (range.length == 0) {
                                    startIndex = 0;
                                    endIndex = wavelength.length - 1;
                                } else {
                                    if (wavelength[j] >= range[0][0] && wavelength[j] <= range[0][1]) {
                                        if (startIndex == -1) {
                                            startIndex = j;
                                        }
                                        endIndex = j;
                                    }
                                }
                            }
                            if (startIndex >= endIndex) {
                                throw new Exception("Fitting range ERROR");
                            }
                            x = new double[endIndex - startIndex + 1];
                            System.arraycopy(Wavelength,startIndex,x,0,x.length);
                        }
                        y = new double[endIndex - startIndex + 1];
                        System.arraycopy(intensity,startIndex,y,0,y.length);
                        y = RemoveOutliers(y);
                        callable = new FitAllData(x,y,intensity,fit);
                        if (!Executor.isShutdown()) {
                            future = Executor.submit(callable);
                            fitResultCollect.add(future);
                        } else {
                            break;
                        }
                        progressBar1.setValue(i * 100 / pointCount);
                        progressBar1.setString("提交任務: " + progressBar1.getValue() + "%");
                    }
                    label30.setText("分析中");
                    double[][] fitResult = new double[pointCount][4];
                    if (!Executor.isShutdown()) {
                        for (int i = 0; i < fitResultCollect.size(); i++) {
                            try {
                                fitResult[i] = fitResultCollect.get(i).get(2, TimeUnit.SECONDS);
                            } catch (Exception ignored) {}
                            progressBar1.setValue(i * 100 / (pointCount - 1));
                            progressBar1.setString("分析中: " + progressBar1.getValue() + "%");
                        }
                        fitDone = true;
                    }
                    if (fitDone) {
                        label30.setText("正在儲存結果");
                        for (int i = 0; i < 3; i++) {
                            String fileName;
                            if (i == 0) {
                                fileName = FileName.substring(0,FileName.length() - 4) + "_Intensity.txt";
                            } else if (i == 1) {
                                fileName = FileName.substring(0,FileName.length() - 4) + "_Wavelength.txt";
                            } else {
                                fileName = FileName.substring(0,FileName.length() - 4) + "_FWHM.txt";
                            }
                            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(FolderPath,fileName)));
                            writer.write(" ");
                            writer.write("\t");
                            Iterator<Double> iterator_YPos = YPos.iterator();;
                            for (int j = 0; j < YPos.size(); j++) {
                                writer.write(String.valueOf(iterator_YPos.next()));
                                if (j != YPos.size() - 1) {
                                    writer.write("\t");
                                }
                            }
                            writer.newLine();
                            Iterator<Double> iterator_XPos = XPos.iterator();
                            int fitResultIndex = 0;
                            for (int j = 0; j < XPos.size(); j++) {
                                writer.write(String.valueOf(iterator_XPos.next()));
                                writer.write("\t");
                                for (int k = 0; k < YPos.size(); k++) {
                                    writer.write(String.valueOf(fitResult[fitResultIndex][i]));
                                    fitResultIndex++;
                                    if (k != YPos.size() - 1) {
                                        writer.write("\t");
                                    } else {
                                        writer.newLine();
                                    }
                                }
                            }
                            writer.close();
                        }
                        JOptionPane.showMessageDialog(this,"已完成","FitAllData",JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,ex.getMessage(),"FitAllData",JOptionPane.ERROR_MESSAGE);
                } finally {
                    PanelONOFF(2,true);
                    label30.setText("數據分析");
                    progressBar1.setString("0%");
                    progressBar1.setValue(0);
                    Executor.shutdownNow();
                }
            });
            FitAllDataThread.start();
            PanelONOFF(2,false);
        }
    }

    private void StopFittingAllData(ActionEvent e) {
        // TODO add your code here
        if (FitAllDataThread.isAlive()) {
            Executor.shutdownNow();
            while (true) {
                if (Executor.isShutdown()) {
                    FitAllDataThread.stop();
                    break;
                }
            }
            JOptionPane.showMessageDialog(this,"已停止","Stop",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void OpenFolder(ActionEvent e) {
        // TODO add your code here
        String path = Objects.isNull(FolderPath) ? System.getProperty("user.home") : FolderPath;
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,ex,"OpenFolder",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Educational license - 王成杰 (d108066002)
        panel1 = new JPanel();
        label1 = new JLabel();
        panel2 = new JPanel();
        button1 = new JButton();
        spinner1 = new JSpinner();
        panel3 = new JPanel();
        separator1 = new JSeparator();
        label2 = new JLabel();
        panel4 = new JPanel();
        label3 = new JLabel();
        panel5 = new JPanel();
        separator2 = new JSeparator();
        label4 = new JLabel();
        label5 = new JLabel();
        comboBox2 = new JComboBox<>();
        panel6 = new JPanel();
        separator3 = new JSeparator();
        label6 = new JLabel();
        label7 = new JLabel();
        comboBox3 = new JComboBox<>();
        panel7 = new JPanel();
        separator4 = new JSeparator();
        label8 = new JLabel();
        panel8 = new JPanel();
        button3 = new JButton();
        toggleButton2 = new JToggleButton();
        panel9 = new JPanel();
        separator5 = new JSeparator();
        label9 = new JLabel();
        panel10 = new JPanel();
        label10 = new JLabel();
        label11 = new JLabel();
        label12 = new JLabel();
        label13 = new JLabel();
        label14 = new JLabel();
        label15 = new JLabel();
        label16 = new JLabel();
        label17 = new JLabel();
        comboBox1 = new JComboBox<>();
        toggleButton1 = new JToggleButton();
        panel14 = new JPanel();
        separator6 = new JSeparator();
        label29 = new JLabel();
        label30 = new JLabel();
        panel15 = new JPanel();
        separator7 = new JSeparator();
        panel16 = new JPanel();
        button2 = new JButton();
        button4 = new JButton();
        button5 = new JButton();
        panel11 = new JPanel();
        label18 = new JLabel();
        label19 = new JLabel();
        label20 = new JLabel();
        label21 = new JLabel();
        label22 = new JLabel();
        label23 = new JLabel();
        label24 = new JLabel();
        label25 = new JLabel();
        label26 = new JLabel();
        label27 = new JLabel();
        label28 = new JLabel();
        panel12 = new JPanel();
        progressBar1 = new JProgressBar();
        panel13 = new JPanel();

        //======== this ========
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Curve Fitting Tool 3 (PL/Raman)");
        var contentPane = getContentPane();

        //======== panel1 ========
        {

            //---- label1 ----
            label1.setText("\u8b80\u53d6\u6a94\u6848");
            label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.BOLD, label1.getFont().getSize() + 4f));

            //======== panel2 ========
            {
                panel2.setLayout(new GridLayout(1, 2, 10, 5));

                //---- button1 ----
                button1.setText("\u9078\u64c7\u6a94\u6848");
                button1.setFont(button1.getFont().deriveFont(button1.getFont().getStyle() | Font.BOLD, button1.getFont().getSize() + 2f));
                button1.addActionListener(e -> LoadFile(e));
                panel2.add(button1);

                //---- spinner1 ----
                spinner1.setFont(spinner1.getFont().deriveFont(spinner1.getFont().getStyle() | Font.BOLD, spinner1.getFont().getSize() + 2f));
                spinner1.addChangeListener(e -> ShowNextCurve(e));
                panel2.add(spinner1);
            }

            //======== panel3 ========
            {
                panel3.setLayout(new CardLayout());
                panel3.add(separator1, "card1");
            }

            //---- label2 ----
            label2.setText("\u9078\u64c7\u6c42\u89e3\u5668");
            label2.setFont(label2.getFont().deriveFont(label2.getFont().getStyle() | Font.BOLD, label2.getFont().getSize() + 4f));

            //======== panel4 ========
            {
                panel4.setLayout(new CardLayout());
            }

            //---- label3 ----
            label3.setText("\u6c42\u89e3\u5668");
            label3.setFont(label3.getFont().deriveFont(label3.getFont().getStyle() | Font.BOLD, label3.getFont().getSize() + 2f));
            label3.setHorizontalAlignment(SwingConstants.CENTER);

            //======== panel5 ========
            {
                panel5.setLayout(new CardLayout());
                panel5.add(separator2, "card1");
            }

            //---- label4 ----
            label4.setText("\u6aa2\u6e2c\u80cc\u666f\u503c");
            label4.setFont(label4.getFont().deriveFont(label4.getFont().getStyle() | Font.BOLD, label4.getFont().getSize() + 4f));

            //---- label5 ----
            label5.setText("\u6aa2\u6e2c\u9078\u9805");
            label5.setFont(label5.getFont().deriveFont(label5.getFont().getStyle() | Font.BOLD, label5.getFont().getSize() + 2f));

            //---- comboBox2 ----
            comboBox2.setFont(comboBox2.getFont().deriveFont(comboBox2.getFont().getStyle() | Font.BOLD, comboBox2.getFont().getSize() + 2f));
            comboBox2.setModel(new DefaultComboBoxModel<>(new String[] {
                "\u773e\u6578",
                "\u5e73\u5747\u6578",
                "\u4e2d\u4f4d\u6578",
                "\u6700\u5c0f\u503c"
            }));
            comboBox2.addActionListener(e -> DetectBackgroundAndOutlier(e));

            //======== panel6 ========
            {
                panel6.setLayout(new CardLayout());
                panel6.add(separator3, "card1");
            }

            //---- label6 ----
            label6.setText("\u6aa2\u6e2c\u96e2\u6563\u503c");
            label6.setFont(label6.getFont().deriveFont(label6.getFont().getStyle() | Font.BOLD, label6.getFont().getSize() + 4f));

            //---- label7 ----
            label7.setText("\u6aa2\u6e2c\u9078\u9805");
            label7.setFont(label7.getFont().deriveFont(label7.getFont().getStyle() | Font.BOLD, label7.getFont().getSize() + 2f));

            //---- comboBox3 ----
            comboBox3.setFont(comboBox3.getFont().deriveFont(Font.BOLD, comboBox3.getFont().getSize() + 2f));
            comboBox3.setModel(new DefaultComboBoxModel<>(new String[] {
                "MAD",
                "BoxPlot",
                "3 sigma",
                "\u95dc\u9589"
            }));
            comboBox3.addActionListener(e -> DetectBackgroundAndOutlier(e));

            //======== panel7 ========
            {
                panel7.setLayout(new CardLayout());
                panel7.add(separator4, "card1");
            }

            //---- label8 ----
            label8.setText("\u5951\u5408\u7bc4\u570d");
            label8.setFont(label8.getFont().deriveFont(label8.getFont().getStyle() | Font.BOLD, label8.getFont().getSize() + 4f));

            //======== panel8 ========
            {

                //---- button3 ----
                button3.setText("\u6e05\u9664\u7bc4\u570d");
                button3.setFont(button3.getFont().deriveFont(button3.getFont().getStyle() | Font.BOLD, button3.getFont().getSize() + 2f));
                button3.setEnabled(false);
                button3.addActionListener(e -> ClearFittingRange(e));

                //---- toggleButton2 ----
                toggleButton2.setText("\u8a2d\u5b9a\u5951\u5408\u7bc4\u570d");
                toggleButton2.setFont(toggleButton2.getFont().deriveFont(toggleButton2.getFont().getStyle() | Font.BOLD, toggleButton2.getFont().getSize() + 2f));
                toggleButton2.addActionListener(e -> SetFittingRange(e));

                GroupLayout panel8Layout = new GroupLayout(panel8);
                panel8.setLayout(panel8Layout);
                panel8Layout.setHorizontalGroup(
                    panel8Layout.createParallelGroup()
                        .addGroup(panel8Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(toggleButton2)
                            .addGap(12, 12, 12)
                            .addComponent(button3)
                            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                panel8Layout.setVerticalGroup(
                    panel8Layout.createParallelGroup()
                        .addGroup(panel8Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel8Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(button3)
                                .addComponent(toggleButton2))
                            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
            }

            //======== panel9 ========
            {
                panel9.setLayout(new CardLayout());
                panel9.add(separator5, "card1");
            }

            //---- label9 ----
            label9.setText("\u6a94\u6848\u8cc7\u8a0a");
            label9.setFont(label9.getFont().deriveFont(label9.getFont().getStyle() | Font.BOLD, label9.getFont().getSize() + 4f));

            //======== panel10 ========
            {

                //---- label10 ----
                label10.setText("\u6a94\u6848\u540d\u7a31\uff1a");
                label10.setFont(label10.getFont().deriveFont(label10.getFont().getStyle() | Font.BOLD, label10.getFont().getSize() + 2f));

                //---- label11 ----
                label11.setText("null");
                label11.setFont(label11.getFont().deriveFont(label11.getFont().getStyle() | Font.BOLD, label11.getFont().getSize() + 2f));

                //---- label12 ----
                label12.setText("\u6a94\u6848\u5927\u5c0f\uff1a");
                label12.setFont(label12.getFont().deriveFont(label12.getFont().getStyle() | Font.BOLD, label12.getFont().getSize() + 2f));

                //---- label13 ----
                label13.setText("null");
                label13.setFont(label13.getFont().deriveFont(label13.getFont().getStyle() | Font.BOLD, label13.getFont().getSize() + 2f));

                //---- label14 ----
                label14.setText("\u9ede\u6578\uff1a");
                label14.setFont(label14.getFont().deriveFont(label14.getFont().getStyle() | Font.BOLD, label14.getFont().getSize() + 2f));
                label14.setHorizontalAlignment(SwingConstants.RIGHT);

                //---- label15 ----
                label15.setText("null");
                label15.setFont(label15.getFont().deriveFont(label15.getFont().getStyle() | Font.BOLD, label15.getFont().getSize() + 2f));

                //---- label16 ----
                label16.setText("\u91cf\u6e2c\u7bc4\u570d\uff1a");
                label16.setFont(label16.getFont().deriveFont(label16.getFont().getStyle() | Font.BOLD, label16.getFont().getSize() + 2f));
                label16.setHorizontalAlignment(SwingConstants.RIGHT);

                //---- label17 ----
                label17.setText("null");
                label17.setFont(label17.getFont().deriveFont(label17.getFont().getStyle() | Font.BOLD, label17.getFont().getSize() + 2f));

                GroupLayout panel10Layout = new GroupLayout(panel10);
                panel10.setLayout(panel10Layout);
                panel10Layout.setHorizontalGroup(
                    panel10Layout.createParallelGroup()
                        .addGroup(panel10Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addGroup(panel10Layout.createSequentialGroup()
                                    .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(label16, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(label14, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(label12, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(label13, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(label17, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                                        .addComponent(label15, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)))
                                .addGroup(panel10Layout.createSequentialGroup()
                                    .addComponent(label10)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(label11, GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)))
                            .addGap(0, 0, Short.MAX_VALUE))
                );
                panel10Layout.setVerticalGroup(
                    panel10Layout.createParallelGroup()
                        .addGroup(panel10Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label10, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label11, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label12, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label13, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label14, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label15, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(panel10Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label16, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addComponent(label17, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
            }

            //---- comboBox1 ----
            comboBox1.setFont(comboBox1.getFont().deriveFont(comboBox1.getFont().getStyle() | Font.BOLD, comboBox1.getFont().getSize() + 2f));
            comboBox1.setModel(new DefaultComboBoxModel<>(new String[] {
                "\u6a19\u6e96\u9ad8\u65af\u5951\u5408",
                "\u6578\u503c\u5206\u6790",
                "\u6a21\u5f0f\u641c\u5c0b"
            }));
            comboBox1.addActionListener(e -> SetFitType(e));

            //---- toggleButton1 ----
            toggleButton1.setText("\u81ea\u52d5\u5951\u5408");
            toggleButton1.setFont(toggleButton1.getFont().deriveFont(toggleButton1.getFont().getStyle() | Font.BOLD, toggleButton1.getFont().getSize() + 2f));
            toggleButton1.addActionListener(e -> AutoFitONOFF(e));

            //======== panel14 ========
            {
                panel14.setLayout(new CardLayout());
                panel14.add(separator6, "card1");
            }

            //---- label29 ----
            label29.setText("\u72c0\u614b\uff1a");
            label29.setFont(label29.getFont().deriveFont(label29.getFont().getStyle() | Font.BOLD, label29.getFont().getSize() + 4f));

            //---- label30 ----
            label30.setText("\u7121\u6578\u64da");
            label30.setFont(label30.getFont().deriveFont(label30.getFont().getStyle() | Font.BOLD, label30.getFont().getSize() + 4f));

            //======== panel15 ========
            {
                panel15.setLayout(new CardLayout());
                panel15.add(separator7, "card1");
            }

            //======== panel16 ========
            {
                panel16.setLayout(new GridLayout(1, 3, 10, 10));

                //---- button2 ----
                button2.setText("\u958b\u59cb\u5951\u5408");
                button2.setFont(button2.getFont().deriveFont(button2.getFont().getStyle() | Font.BOLD, button2.getFont().getSize() + 2f));
                button2.addActionListener(e -> FitAllData(e));
                panel16.add(button2);

                //---- button4 ----
                button4.setText("\u505c\u6b62");
                button4.setFont(button4.getFont().deriveFont(button4.getFont().getStyle() | Font.BOLD, button4.getFont().getSize() + 2f));
                button4.setEnabled(false);
                button4.addActionListener(e -> StopFittingAllData(e));
                panel16.add(button4);

                //---- button5 ----
                button5.setText("\u6253\u958b\u8cc7\u6599\u593e");
                button5.setFont(button5.getFont().deriveFont(button5.getFont().getStyle() | Font.BOLD, button5.getFont().getSize() + 2f));
                button5.addActionListener(e -> OpenFolder(e));
                panel16.add(button5);
            }

            GroupLayout panel1Layout = new GroupLayout(panel1);
            panel1.setLayout(panel1Layout);
            panel1Layout.setHorizontalGroup(
                panel1Layout.createParallelGroup()
                    .addGroup(panel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel1Layout.createParallelGroup()
                            .addComponent(label6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(label1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(label2, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(label4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel7, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(label8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel9, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(label9, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel10, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel14, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel15, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(panel1Layout.createSequentialGroup()
                                .addComponent(label29)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(label30, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(panel1Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(panel1Layout.createParallelGroup()
                                    .addGroup(panel1Layout.createSequentialGroup()
                                        .addComponent(label5)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBox2, GroupLayout.PREFERRED_SIZE, 121, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panel1Layout.createSequentialGroup()
                                        .addComponent(label7)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBox3, GroupLayout.PREFERRED_SIZE, 122, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panel1Layout.createSequentialGroup()
                                        .addComponent(label3, GroupLayout.PREFERRED_SIZE, 57, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(panel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(toggleButton1)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(panel16, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())
            );
            panel1Layout.setVerticalGroup(
                panel1Layout.createParallelGroup()
                    .addGroup(panel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(label1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel2, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(label2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel1Layout.createParallelGroup()
                            .addComponent(panel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(panel1Layout.createSequentialGroup()
                                .addGroup(panel1Layout.createParallelGroup()
                                    .addComponent(label3, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                    .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(toggleButton1)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(label4)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(label5, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBox2, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(label6)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(label7, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboBox3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel7, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(label8)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel8, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel9, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(label9)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel10, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel14, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(panel1Layout.createParallelGroup()
                            .addComponent(label29)
                            .addComponent(label30, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel15, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(panel16, GroupLayout.PREFERRED_SIZE, 39, GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41))
            );
        }

        //======== panel11 ========
        {
            panel11.setLayout(new FlowLayout(FlowLayout.LEFT));

            //---- label18 ----
            label18.setText("\u5f37\u5ea6(a.u)\uff1a");
            label18.setFont(label18.getFont().deriveFont(label18.getFont().getStyle() | Font.BOLD, label18.getFont().getSize() + 2f));
            panel11.add(label18);

            //---- label19 ----
            label19.setText("null");
            label19.setFont(label19.getFont().deriveFont(label19.getFont().getStyle() | Font.BOLD, label19.getFont().getSize() + 2f));
            panel11.add(label19);

            //---- label20 ----
            label20.setText("  |  ");
            label20.setFont(label20.getFont().deriveFont(label20.getFont().getStyle() | Font.BOLD, label20.getFont().getSize() + 2f));
            panel11.add(label20);

            //---- label21 ----
            label21.setText("\u6ce2\u9577(nm)\uff1a");
            label21.setFont(label21.getFont().deriveFont(label21.getFont().getStyle() | Font.BOLD, label21.getFont().getSize() + 2f));
            panel11.add(label21);

            //---- label22 ----
            label22.setText("null");
            label22.setFont(label22.getFont().deriveFont(label22.getFont().getStyle() | Font.BOLD, label22.getFont().getSize() + 2f));
            panel11.add(label22);

            //---- label23 ----
            label23.setText("  |  ");
            label23.setFont(label23.getFont().deriveFont(label23.getFont().getStyle() | Font.BOLD, label23.getFont().getSize() + 2f));
            panel11.add(label23);

            //---- label24 ----
            label24.setText("\u534a\u9ad8\u5bec(nm)\uff1a");
            label24.setFont(label24.getFont().deriveFont(label24.getFont().getStyle() | Font.BOLD, label24.getFont().getSize() + 2f));
            panel11.add(label24);

            //---- label25 ----
            label25.setText("null");
            label25.setFont(label25.getFont().deriveFont(label25.getFont().getStyle() | Font.BOLD, label25.getFont().getSize() + 2f));
            panel11.add(label25);

            //---- label26 ----
            label26.setText("  |  ");
            label26.setFont(label26.getFont().deriveFont(label26.getFont().getStyle() | Font.BOLD, label26.getFont().getSize() + 2f));
            panel11.add(label26);

            //---- label27 ----
            label27.setText("\u80cc\u666f\u503c(a.u)\uff1a");
            label27.setFont(label27.getFont().deriveFont(label27.getFont().getStyle() | Font.BOLD, label27.getFont().getSize() + 2f));
            panel11.add(label27);

            //---- label28 ----
            label28.setText("null");
            label28.setFont(label28.getFont().deriveFont(label28.getFont().getStyle() | Font.BOLD, label28.getFont().getSize() + 2f));
            panel11.add(label28);
        }

        //======== panel12 ========
        {
            panel12.setLayout(new CardLayout());

            //---- progressBar1 ----
            progressBar1.setStringPainted(true);
            progressBar1.setFont(progressBar1.getFont().deriveFont(progressBar1.getFont().getStyle() | Font.BOLD, progressBar1.getFont().getSize() + 2f));
            panel12.add(progressBar1, "card1");
        }

        //======== panel13 ========
        {
            panel13.setLayout(new CardLayout());
        }

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addGroup(contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addComponent(panel11, GroupLayout.PREFERRED_SIZE, 668, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panel12, GroupLayout.PREFERRED_SIZE, 173, GroupLayout.PREFERRED_SIZE)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addComponent(panel13, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(contentPaneLayout.createParallelGroup()
                        .addComponent(panel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                .addComponent(panel12, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(panel11, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(panel13, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Educational license - 王成杰 (d108066002)
    private JPanel panel1;
    private JLabel label1;
    private JPanel panel2;
    private JButton button1;
    private JSpinner spinner1;
    private JPanel panel3;
    private JSeparator separator1;
    private JLabel label2;
    private JPanel panel4;
    private JLabel label3;
    private JPanel panel5;
    private JSeparator separator2;
    private JLabel label4;
    private JLabel label5;
    private JComboBox<String> comboBox2;
    private JPanel panel6;
    private JSeparator separator3;
    private JLabel label6;
    private JLabel label7;
    private JComboBox<String> comboBox3;
    private JPanel panel7;
    private JSeparator separator4;
    private JLabel label8;
    private JPanel panel8;
    private JButton button3;
    private JToggleButton toggleButton2;
    private JPanel panel9;
    private JSeparator separator5;
    private JLabel label9;
    private JPanel panel10;
    private JLabel label10;
    private JLabel label11;
    private JLabel label12;
    private JLabel label13;
    private JLabel label14;
    private JLabel label15;
    private JLabel label16;
    private JLabel label17;
    private JComboBox<String> comboBox1;
    private JToggleButton toggleButton1;
    private JPanel panel14;
    private JSeparator separator6;
    private JLabel label29;
    private JLabel label30;
    private JPanel panel15;
    private JSeparator separator7;
    private JPanel panel16;
    private JButton button2;
    private JButton button4;
    private JButton button5;
    private JPanel panel11;
    private JLabel label18;
    private JLabel label19;
    private JLabel label20;
    private JLabel label21;
    private JLabel label22;
    private JLabel label23;
    private JLabel label24;
    private JLabel label25;
    private JLabel label26;
    private JLabel label27;
    private JLabel label28;
    private JPanel panel12;
    private JProgressBar progressBar1;
    private JPanel panel13;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
