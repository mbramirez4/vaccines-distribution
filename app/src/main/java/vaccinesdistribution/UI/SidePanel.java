package vaccinesdistribution.UI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import vaccinesdistribution.Service.Distributor;
import vaccinesdistribution.Util.Point;
import vaccinesdistribution.Model.Order;
import java.util.List;

public class SidePanel extends JPanel {
    private Distributor distributor;
    private VaccineDistributionUI parentUI;
    
    private JTextField xField;
    private JTextField yField;
    private JTextField quantityField;
    private JButton createOrderButton;
    private JTextArea messageArea;
    private JScrollPane messageScrollPane;

    public SidePanel(Distributor distributor, VaccineDistributionUI parentUI) {
        this.distributor = distributor;
        this.parentUI = parentUI;
        setLayout(new BorderLayout());
        
        // Create order input panel (top half)
        JPanel orderPanel = createOrderPanel();
        
        // Create message area (bottom half)
        JPanel messagePanel = createMessagePanel();
        
        // Split the side panel: top half for order input, bottom half for messages
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, orderPanel, messagePanel);
        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);
        
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new TitledBorder("Create Order"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // X coordinate label and field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("X Coordinate:"), gbc);
        
        gbc.gridx = 1;
        xField = new JTextField(15);
        panel.add(xField, gbc);
        
        // Y coordinate label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Y Coordinate:"), gbc);
        
        gbc.gridx = 1;
        yField = new JTextField(15);
        panel.add(yField, gbc);
        
        // Quantity label and field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Quantity:"), gbc);
        
        gbc.gridx = 1;
        quantityField = new JTextField(15);
        panel.add(quantityField, gbc);
        
        // Create order button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        createOrderButton = new JButton("Create Order");
        createOrderButton.addActionListener(new CreateOrderListener());
        panel.add(createOrderButton, gbc);
        
        // Add separator
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JSeparator(), gbc);
        
        // Dispatch Next Order button
        gbc.gridy = 5;
        JButton dispatchNextOrderButton = new JButton("Dispatch Next Order");
        dispatchNextOrderButton.addActionListener(new DispatchNextOrderListener());
        panel.add(dispatchNextOrderButton, gbc);
        
        // Dispatch All Orders button
        gbc.gridy = 6;
        JButton dispatchAllOrdersButton = new JButton("Dispatch All Orders");
        dispatchAllOrdersButton.addActionListener(new DispatchAllOrdersListener());
        panel.add(dispatchAllOrdersButton, gbc);
        
        // Finish Day button
        gbc.gridy = 7;
        JButton finishDayButton = new JButton("Finish Day");
        finishDayButton.addActionListener(new FinishDayListener());
        panel.add(finishDayButton, gbc);
        
        // Add some spacing
        gbc.gridy = 8;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }

    private JPanel createMessagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new TitledBorder("Messages"));
        
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messageArea.setBackground(Color.WHITE);
        
        messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        panel.add(messageScrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    public void addMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }

    private class CreateOrderListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int x = Integer.parseInt(xField.getText().trim());
                int y = Integer.parseInt(yField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim());
                
                Point deliveryLocation = new Point(x, y);
                
                distributor.createOrder(quantity, deliveryLocation);
                
                String message = String.format("Order created successfully: Quantity=%d, Location=(%d, %d)", 
                                               quantity, x, y);
                addMessage(message);
                addMessage("Current Day: " + distributor.getCurrentDay());
                addMessage("Available Batches: " + distributor.getAvailableBatches());
                
                // Clear fields
                xField.setText("");
                yField.setText("");
                quantityField.setText("");
                
                // Refresh map to show new order location if needed
                parentUI.refreshMap();
                
            } catch (NumberFormatException ex) {
                addMessage("Error: Please enter valid numbers for coordinates and quantity.");
            } catch (IllegalArgumentException ex) {
                addMessage("Error: " + ex.getMessage());
            } catch (Exception ex) {
                addMessage("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private class DispatchNextOrderListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                distributor.dispatchNextOrder();
                addMessage("Dispatched next order in queue.");
                addMessage("Current Day: " + distributor.getCurrentDay());
                addMessage("Available Batches: " + distributor.getAvailableBatches());
                parentUI.refreshMap();
            } catch (RuntimeException ex) {
                addMessage("Error dispatching order: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                addMessage("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private class DispatchAllOrdersListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                distributor.dispatchOrders();
                addMessage("Dispatched all orders in queue.");
                addMessage("Current Day: " + distributor.getCurrentDay());
                addMessage("Available Batches: " + distributor.getAvailableBatches());
                parentUI.refreshMap();
            } catch (RuntimeException ex) {
                addMessage("Error dispatching orders: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                addMessage("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private class FinishDayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                distributor.finishDay();
                addMessage("Finished day. Day advanced to: " + distributor.getCurrentDay());
                addMessage("Available Batches: " + distributor.getAvailableBatches());
                addMessage("Expired objects disposed and new vaccines inserted.");
                
                // Display previous day orders information
                List<Order> previousDayOrders = distributor.getPreviousDayOrders();
                addMessage("--- Previous Day Orders (" + previousDayOrders.size() + ") ---");
                if (previousDayOrders.isEmpty()) {
                    addMessage("No orders from the previous day.");
                } else {
                    for (Order order : previousDayOrders) {
                        addMessage("  " + order.toString());
                    }
                }
                addMessage("--- End of Previous Day Orders ---");
                
                parentUI.refreshMap();
            } catch (RuntimeException ex) {
                addMessage("Error finishing day: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                addMessage("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}

