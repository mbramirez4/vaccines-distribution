package vaccinesdistribution.UI;

import javax.swing.*;
import java.awt.*;

import vaccinesdistribution.Service.Distributor;

public class VaccineDistributionUI extends JFrame {
    private Distributor distributor;
    private MapPanel mapPanel;
    private SidePanel sidePanel;

    public VaccineDistributionUI() {
        distributor = Distributor.getDistributor();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Vaccine Distribution System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panels
        mapPanel = new MapPanel(distributor, this);
        sidePanel = new SidePanel(distributor, this);

        // Layout: 2/3 map on left, 1/3 side panel on right
        add(mapPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        // Set preferred size for side panel (1/3 of width)
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int sidePanelWidth = screenSize.width / 3;
        sidePanel.setPreferredSize(new Dimension(sidePanelWidth, screenSize.height));

        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    public void refreshMap() {
        mapPanel.repaint();
    }

    public void addMessage(String message) {
        sidePanel.addMessage(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VaccineDistributionUI().setVisible(true);
        });
    }
}



