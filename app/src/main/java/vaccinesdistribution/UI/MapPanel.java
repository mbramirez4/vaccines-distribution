package vaccinesdistribution.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import vaccinesdistribution.Service.Distributor;
import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.Order;
import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Util.Point;

public class MapPanel extends JPanel {
    private Distributor distributor;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private static final int PADDING = 50;
    private static final int WAREHOUSE_SIZE = 10;
    private static final int ORDER_SIZE = 8;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_IN_FACTOR = 1.05;  // Smaller factor for smoother zoom in
    private static final double ZOOM_OUT_FACTOR = 1.03;  // Even smaller for zoom out to reduce sensitivity
    
    // Zoom and pan state
    private double zoomLevel = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    
    // Mouse interaction state
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private boolean isDragging = false;
    private Warehouse hoveredWarehouse = null;
    private Order hoveredOrder = null;
    
    // Tooltip
    private JLabel tooltipLabel;
    private JWindow tooltipWindow;

    public MapPanel(Distributor distributor, VaccineDistributionUI parentUI) {
        this.distributor = distributor;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Warehouse Map"));
        setLayout(null); // Use null layout for absolute positioning of reset button
        
        setupMouseListeners();
        setupTooltip();
        setupResetButton();
    }
    
    private void setupResetButton() {
        JButton resetButton = new JButton("Reset View");
        resetButton.setBounds(10, 10, 100, 30);
        resetButton.addActionListener(e -> resetView());
        add(resetButton);
    }
    
    private void resetView() {
        zoomLevel = 1.0;
        panX = 0.0;
        panY = 0.0;
        repaint();
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = true;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    setCursor(new Cursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = false;
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;
                    
                    // Pan in world coordinates
                    // Drag right → canvas moves left (decrease panX)
                    // Drag down → canvas moves up (increase panY, accounting for Y inversion)
                    panX -= dx / zoomLevel;
                    panY += dy / zoomLevel;
                    
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoveredObjects(e.getX(), e.getY());
            }
        });

        addMouseWheelListener(e -> {
            // Use precise wheel rotation for better touchpad support
            double rotation = e.getPreciseWheelRotation();
            double oldZoom = zoomLevel;
            
            // Get mouse position in screen coordinates
            int mouseScreenX = e.getX();
            int mouseScreenY = e.getY();
            
            // On macOS touchpad, rotation direction may be inverted
            // Invert the logic: positive rotation (scroll up) = zoom in
            if (rotation > 0) {
                // Zoom in - use smaller factor for smoother experience
                zoomLevel = Math.min(MAX_ZOOM, zoomLevel * ZOOM_IN_FACTOR);
            } else if (rotation < 0) {
                // Zoom out - use even smaller factor to reduce sensitivity
                zoomLevel = Math.max(MIN_ZOOM, zoomLevel / ZOOM_OUT_FACTOR);
            } else {
                // No rotation, skip
                return;
            }
            
            // Adjust pan so that the point under the mouse stays at the same screen position
            // This ensures zooming is centered on the mouse cursor
            List<Warehouse> warehouses = distributor.getWarehouses();
            if (!warehouses.isEmpty()) {
                int panelWidth = getWidth() - 2 * PADDING;
                int panelHeight = getHeight() - 2 * PADDING;
                
                double baseScaleX = (double) panelWidth / (maxX - minX);
                double baseScaleY = (double) panelHeight / (maxY - minY);
                double baseScale = Math.min(baseScaleX, baseScaleY);
                
                double oldScale = baseScale * oldZoom;
                double newScale = baseScale * zoomLevel;
                
                // Calculate offset to keep mouse position fixed in world coordinates
                double centerScreenX = getWidth() / 2.0;
                double centerScreenY = getHeight() / 2.0;
                
                double offsetX = (mouseScreenX - centerScreenX) * (1.0 / oldScale - 1.0 / newScale);
                double offsetY = -(mouseScreenY - centerScreenY) * (1.0 / oldScale - 1.0 / newScale);
                
                panX += offsetX;
                panY += offsetY;
            }
            
            repaint();
        });
    }

    private void setupTooltip() {
        tooltipWindow = new JWindow((Frame) SwingUtilities.getWindowAncestor(this));
        tooltipLabel = new JLabel();
        tooltipLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        tooltipLabel.setBackground(new Color(255, 255, 225));
        tooltipLabel.setOpaque(true);
        tooltipWindow.add(tooltipLabel);
        tooltipWindow.pack();
    }

    private void updateHoveredObjects(int screenX, int screenY) {
        Point mouseWorld = screenToWorld(screenX, screenY);
        double threshold = 20.0 / zoomLevel;
        
        // Check orders first (they should take priority for hover)
        Order closestOrder = null;
        double minOrderDistance = Double.MAX_VALUE;
        
        // Check previous day orders
        List<Order> previousDayOrders = distributor.getPreviousDayOrders();
        Point location;
        double distance;
        for (Order order : previousDayOrders) {
            location = order.getDeliveryLocation();
            distance = Math.sqrt(
                Math.pow(location.getXCoordinate() - mouseWorld.getXCoordinate(), 2) +
                Math.pow(location.getYCoordinate() - mouseWorld.getYCoordinate(), 2)
            );
            if (distance < threshold && distance < minOrderDistance) {
                minOrderDistance = distance;
                closestOrder = order;
            }
        }
        
        // Check current day orders
        List<Order> currentDayOrders = distributor.getCurrentDayOrders();
        for (Order order : currentDayOrders) {
            location = order.getDeliveryLocation();
            distance = Math.sqrt(
                Math.pow(location.getXCoordinate() - mouseWorld.getXCoordinate(), 2) +
                Math.pow(location.getYCoordinate() - mouseWorld.getYCoordinate(), 2)
            );
            if (distance < threshold && distance < minOrderDistance) {
                minOrderDistance = distance;
                closestOrder = order;
            }
        }
        
        // Check warehouses only if no order is hovered
        Warehouse closestWarehouse = null;
        if (closestOrder == null) {
            List<Warehouse> warehouses = distributor.getWarehouses();
            double minWarehouseDistance = Double.MAX_VALUE;
            
            for (Warehouse warehouse : warehouses) {
                location = warehouse.getLocation();
                distance = Math.sqrt(
                    Math.pow(location.getXCoordinate() - mouseWorld.getXCoordinate(), 2) +
                    Math.pow(location.getYCoordinate() - mouseWorld.getYCoordinate(), 2)
                );
                
                if (distance < threshold && distance < minWarehouseDistance) {
                    minWarehouseDistance = distance;
                    closestWarehouse = warehouse;
                }
            }
        }
        
        // Update hovered objects
        boolean needsRepaint = false;
        if (closestOrder != hoveredOrder) {
            hoveredOrder = closestOrder;
            needsRepaint = true;
        }
        if (closestWarehouse != hoveredWarehouse) {
            hoveredWarehouse = closestWarehouse;
            needsRepaint = true;
        }
        
        // Show/hide tooltip
        if (hoveredOrder != null) {
            showOrderTooltip(hoveredOrder, screenX, screenY);
        } else if (hoveredWarehouse != null) {
            showTooltip(hoveredWarehouse, screenX, screenY);
        } else {
            hideTooltip();
        }
        
        if (needsRepaint) {
            repaint();
        } else if (hoveredOrder != null || hoveredWarehouse != null) {
            // Update tooltip position
            if (hoveredOrder != null) {
                showOrderTooltip(hoveredOrder, screenX, screenY);
            } else {
                showTooltip(hoveredWarehouse, screenX, screenY);
            }
        }
    }

    private void showTooltip(Warehouse warehouse, int screenX, int screenY) {
        Point location = warehouse.getLocation();
        int availableBatches = warehouse.getAvailableBatches();
        Perishable topPriority = warehouse.getTopPriorityObject();
        
        StringBuilder tooltipText = new StringBuilder("<html>");
        tooltipText.append("<b>").append(warehouse.getIdentifier().getName()).append("</b><br>");
        tooltipText.append("Location: (").append(location.getXCoordinate())
                   .append(", ").append(location.getYCoordinate()).append(")<br>");
        tooltipText.append("Available Batches: ").append(availableBatches).append("<br>");
        
        if (topPriority != null) {
            tooltipText.append("Top Priority Batch:<br>");
            tooltipText.append("&nbsp;&nbsp;Quantity: ").append(topPriority.getQuantity()).append("<br>");
            tooltipText.append("&nbsp;&nbsp;Expiration: Day ").append(topPriority.getExpirationDate());
        } else {
            tooltipText.append("Top Priority Batch: None");
        }
        tooltipText.append("</html>");
        
        tooltipLabel.setText(tooltipText.toString());
        tooltipWindow.pack();
        
        // Position tooltip near cursor
        java.awt.Point windowLocation = getLocationOnScreen();
        tooltipWindow.setLocation(
            windowLocation.x + screenX + 15,
            windowLocation.y + screenY + 15
        );
        tooltipWindow.setVisible(true);
    }

    private void showOrderTooltip(Order order, int screenX, int screenY) {
        Point location = order.getDeliveryLocation();
        
        StringBuilder tooltipText = new StringBuilder("<html>");
        tooltipText.append("<b>Order #").append(order.getId()).append("</b><br>");
        tooltipText.append("Location: (").append(location.getXCoordinate())
                   .append(", ").append(location.getYCoordinate()).append(")<br>");
        tooltipText.append("Quantity: ").append(order.getQuantity()).append("<br>");
        tooltipText.append("Status: ");
        
        if (order.isRejected()) {
            tooltipText.append("<font color='red'>Rejected</font>");
        } else if (order.isDispatched()) {
            tooltipText.append("<font color='green'>Dispatched</font>");
        } else {
            tooltipText.append("<font color='orange'>Pending</font>");
        }
        
        if (order.getProcessingDate() >= 0) {
            tooltipText.append("<br>Processed on Day: ").append(order.getProcessingDate());
        }
        
        tooltipText.append("</html>");
        
        tooltipLabel.setText(tooltipText.toString());
        tooltipWindow.pack();
        
        // Position tooltip near cursor
        java.awt.Point windowLocation = getLocationOnScreen();
        tooltipWindow.setLocation(
            windowLocation.x + screenX + 15,
            windowLocation.y + screenY + 15
        );
        tooltipWindow.setVisible(true);
    }

    private void hideTooltip() {
        tooltipWindow.setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Warehouse> warehouses = distributor.getWarehouses();
        if (warehouses.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.drawString("No warehouses available. Add warehouses to see them on the map.", 
                          getWidth() / 2 - 200, getHeight() / 2);
            return;
        }

        // Calculate bounds
        calculateBounds(warehouses);

        // Draw grid
        drawGrid(g2d);

        // Draw previous day orders (gray)
        List<Order> previousDayOrders = distributor.getPreviousDayOrders();
        for (Order order : previousDayOrders) {
            drawOrder(g2d, order, Color.GRAY);
        }

        // Draw current day orders (orange/green/red)
        Color orderColor;
        List<Order> currentDayOrders = distributor.getCurrentDayOrders();
        for (Order order : currentDayOrders) {
            if (order.isRejected()) {
                orderColor = Color.RED;
            } else if (order.isDispatched()) {
                orderColor = Color.GREEN;
            } else {
                orderColor = Color.ORANGE;
            }
            drawOrder(g2d, order, orderColor);
        }

        // Draw warehouses
        for (Warehouse warehouse : warehouses) {
            drawWarehouse(g2d, warehouse);
        }
    }

    private void calculateBounds(List<Warehouse> warehouses) {
        minX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;

        // Include warehouses in bounds
        Point location;
        for (Warehouse warehouse : warehouses) {
            location = warehouse.getLocation();
            minX = Math.min(minX, location.getXCoordinate());
            maxX = Math.max(maxX, location.getXCoordinate());
            minY = Math.min(minY, location.getYCoordinate());
            maxY = Math.max(maxY, location.getYCoordinate());
        }

        // Include previous day orders in bounds
        List<Order> previousDayOrders = distributor.getPreviousDayOrders();
        for (Order order : previousDayOrders) {
            location = order.getDeliveryLocation();
            minX = Math.min(minX, location.getXCoordinate());
            maxX = Math.max(maxX, location.getXCoordinate());
            minY = Math.min(minY, location.getYCoordinate());
            maxY = Math.max(maxY, location.getYCoordinate());
        }

        // Include current day orders in bounds
        List<Order> currentDayOrders = distributor.getCurrentDayOrders();
        for (Order order : currentDayOrders) {
            location = order.getDeliveryLocation();
            minX = Math.min(minX, location.getXCoordinate());
            maxX = Math.max(maxX, location.getXCoordinate());
            minY = Math.min(minY, location.getYCoordinate());
            maxY = Math.max(maxY, location.getYCoordinate());
        }

        // Add padding to bounds
        if (minX == maxX) {
            minX -= 10;
            maxX += 10;
        }
        if (minY == maxY) {
            minY -= 10;
            maxY += 10;
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(0.5f));

        // Calculate grid spacing based on zoom level
        double gridSpacing = calculateGridSpacing();
        
        // Calculate visible world bounds from screen corners
        // This ensures grid covers the entire visible viewport, not just warehouse bounds
        Point topLeft = screenToWorld(0, 0);
        Point topRight = screenToWorld(getWidth(), 0);
        Point bottomLeft = screenToWorld(0, getHeight());
        Point bottomRight = screenToWorld(getWidth(), getHeight());
        
        // Find the min/max world coordinates visible on screen
        int visibleMinX = Math.min(Math.min(topLeft.getXCoordinate(), topRight.getXCoordinate()),
                                  Math.min(bottomLeft.getXCoordinate(), bottomRight.getXCoordinate()));
        int visibleMaxX = Math.max(Math.max(topLeft.getXCoordinate(), topRight.getXCoordinate()),
                                  Math.max(bottomLeft.getXCoordinate(), bottomRight.getXCoordinate()));
        int visibleMinY = Math.min(Math.min(topLeft.getYCoordinate(), topRight.getYCoordinate()),
                                  Math.min(bottomLeft.getYCoordinate(), bottomRight.getYCoordinate()));
        int visibleMaxY = Math.max(Math.max(topLeft.getYCoordinate(), topRight.getYCoordinate()),
                                  Math.max(bottomLeft.getYCoordinate(), bottomRight.getYCoordinate()));
        
        // Add some padding to ensure grid extends beyond visible area
        int padding = (int) (gridSpacing * 2);
        visibleMinX -= padding;
        visibleMaxX += padding;
        visibleMinY -= padding;
        visibleMaxY += padding;
        
        // Find the grid lines to draw based on visible bounds
        int startX = (int) (Math.floor(visibleMinX / gridSpacing) * gridSpacing);
        int endX = (int) (Math.ceil(visibleMaxX / gridSpacing) * gridSpacing);
        int startY = (int) (Math.floor(visibleMinY / gridSpacing) * gridSpacing);
        int endY = (int) (Math.ceil(visibleMaxY / gridSpacing) * gridSpacing);

        // Draw vertical lines across the entire visible height
        Point p1, p2, labelPos;
        for (int x = startX; x <= endX; x += gridSpacing) {
            // Draw line from top to bottom of visible area
            p1 = worldToScreen(x, visibleMinY);
            p2 = worldToScreen(x, visibleMaxY);
            g2d.drawLine(p1.getXCoordinate(), p1.getYCoordinate(), 
                        p2.getXCoordinate(), p2.getYCoordinate());
            
            // Draw coordinate label at bottom of screen if line is visible
            labelPos = worldToScreen(x, visibleMinY);
            int labelX = labelPos.getXCoordinate();
            if (labelX >= 0 && labelX < getWidth()) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                g2d.drawString(String.valueOf(x), labelX + 2, getHeight() - 5);
                g2d.setColor(new Color(200, 200, 200));
            }
        }

        // Draw horizontal lines across the entire visible width
        for (int y = startY; y <= endY; y += gridSpacing) {
            // Draw line from left to right of visible area
            p1 = worldToScreen(visibleMinX, y);
            p2 = worldToScreen(visibleMaxX, y);
            g2d.drawLine(p1.getXCoordinate(), p1.getYCoordinate(), 
                        p2.getXCoordinate(), p2.getYCoordinate());
            
            // Draw coordinate label at left of screen if line is visible
            labelPos = worldToScreen(visibleMinX, y);
            int labelY = labelPos.getYCoordinate();
            if (labelY >= 0 && labelY < getHeight()) {
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                g2d.drawString(String.valueOf(y), 5, labelY - 2);
                g2d.setColor(new Color(200, 200, 200));
            }
        }
    }

    private double calculateGridSpacing() {
        // Adjust grid spacing based on zoom level
        // At zoom 1.0, use spacing of 50, scale with zoom
        double baseSpacing = 50.0;
        double spacing = baseSpacing / zoomLevel;
        
        // Round to nice numbers (1, 2, 5, 10, 20, 50, 100, etc.)
        if (spacing > 100) {
            spacing = Math.round(spacing / 100) * 100;
        } else if (spacing > 50) {
            spacing = Math.round(spacing / 50) * 50;
        } else if (spacing > 20) {
            spacing = Math.round(spacing / 20) * 20;
        } else if (spacing > 10) {
            spacing = Math.round(spacing / 10) * 10;
        } else if (spacing > 5) {
            spacing = Math.round(spacing / 5) * 5;
        } else if (spacing > 2) {
            spacing = Math.round(spacing / 2) * 2;
        } else {
            spacing = Math.round(spacing);
        }
        
        return Math.max(1, spacing);
    }

    private void drawOrder(Graphics2D g2d, Order order, Color color) {
        Point location = order.getDeliveryLocation();
        Point screenPos = worldToScreen(location.getXCoordinate(), location.getYCoordinate());
        
        int x = screenPos.getXCoordinate();
        int y = screenPos.getYCoordinate();
        
        // Highlight hovered order
        if (order == hoveredOrder) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            g2d.fillOval(x - ORDER_SIZE / 2 - 3, y - ORDER_SIZE / 2 - 3, 
                        ORDER_SIZE + 6, ORDER_SIZE + 6);
        }

        // Draw order as a filled circle
        g2d.setColor(color);
        g2d.fillOval(x - ORDER_SIZE / 2, y - ORDER_SIZE / 2, ORDER_SIZE, ORDER_SIZE);
        
        // Draw border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(x - ORDER_SIZE / 2, y - ORDER_SIZE / 2, ORDER_SIZE, ORDER_SIZE);
    }

    private void drawWarehouse(Graphics2D g2d, Warehouse warehouse) {
        Point location = warehouse.getLocation();
        Point screenPos = worldToScreen(location.getXCoordinate(), location.getYCoordinate());
        
        int x = screenPos.getXCoordinate();
        int y = screenPos.getYCoordinate();
        
        // Highlight hovered warehouse
        if (warehouse == hoveredWarehouse) {
            g2d.setColor(new Color(100, 150, 255));
            g2d.fillOval(x - WAREHOUSE_SIZE / 2 - 2, y - WAREHOUSE_SIZE / 2 - 2, 
                        WAREHOUSE_SIZE + 4, WAREHOUSE_SIZE + 4);
        }

        // Draw warehouse as a circle
        g2d.setColor(Color.BLUE);
        g2d.fillOval(x - WAREHOUSE_SIZE / 2, y - WAREHOUSE_SIZE / 2, WAREHOUSE_SIZE, WAREHOUSE_SIZE);
        
        // Draw warehouse name (only if zoomed in enough)
        if (zoomLevel > 0.5) {
            g2d.setColor(Color.BLACK);
            String name = warehouse.getIdentifier().getName();
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(name);
            g2d.drawString(name, x - textWidth / 2, y - WAREHOUSE_SIZE - 5);
        }
    }

    private Point worldToScreen(double worldX, double worldY) {
        // Center of the panel in world coordinates
        double centerWorldX = (minX + maxX) / 2.0 + panX;
        double centerWorldY = (minY + maxY) / 2.0 + panY;
        
        // Calculate scale to fit bounds
        int panelWidth = getWidth() - 2 * PADDING;
        int panelHeight = getHeight() - 2 * PADDING;
        
        double baseScaleX = (double) panelWidth / (maxX - minX);
        double baseScaleY = (double) panelHeight / (maxY - minY);
        double baseScale = Math.min(baseScaleX, baseScaleY);
        
        // Apply zoom
        double scale = baseScale * zoomLevel;
        
        // Convert to screen coordinates
        int screenX = (int) (getWidth() / 2.0 + (worldX - centerWorldX) * scale);
        int screenY = (int) (getHeight() / 2.0 - (worldY - centerWorldY) * scale); // Invert Y
        
        return new Point(screenX, screenY);
    }

    public Point screenToWorld(int screenX, int screenY) {
        List<Warehouse> warehouses = distributor.getWarehouses();
        if (warehouses.isEmpty()) {
            return new Point(screenX, screenY);
        }

        // Center of the panel in world coordinates
        double centerWorldX = (minX + maxX) / 2.0 + panX;
        double centerWorldY = (minY + maxY) / 2.0 + panY;
        
        // Calculate scale
        int panelWidth = getWidth() - 2 * PADDING;
        int panelHeight = getHeight() - 2 * PADDING;
        
        double baseScaleX = (double) panelWidth / (maxX - minX);
        double baseScaleY = (double) panelHeight / (maxY - minY);
        double baseScale = Math.min(baseScaleX, baseScaleY);
        double scale = baseScale * zoomLevel;
        
        // Convert to world coordinates
        double worldX = centerWorldX + (screenX - getWidth() / 2.0) / scale;
        double worldY = centerWorldY - (screenY - getHeight() / 2.0) / scale; // Invert Y
        
        return new Point((int) worldX, (int) worldY);
    }
}
