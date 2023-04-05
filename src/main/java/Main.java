import com.formdev.flatlaf.FlatLightLaf;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    }
}
