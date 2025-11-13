package vaccinesdistribution.Model;

import java.util.Map;
import java.util.HashMap;

import vaccinesdistribution.Util.Point;

public class Order {
    private static int idCounter = 0;

    private int id;
    private int quantity;
    private Point deliveryLocation;
    private boolean isDispatched = false;
    private Map<StoreIdentifier, Integer> dispatchers = new HashMap<>();

    public Order(int quantity, Point deliveryLocation) {
        this.id = idCounter++;
        this.quantity = quantity;
        this.deliveryLocation = deliveryLocation;
    }

    public int getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public Point getDeliveryLocation() {
        return deliveryLocation;
    }

    public boolean isDispatched() {
        return isDispatched;
    }

    // public List<StoreIdentifier> getDispatchers() {
    //     return dispatchers;
    // }

    @Override
    public String toString() {
        String message = "Order{" +
            "id=" + id +
            ", quantity=" + quantity +
            ", deliveryLocation=" + deliveryLocation +
            ", isDispatched=" + isDispatched;
        
        if (isDispatched) message += ", dispatchedFrom=" + dispatchers;
        message += '}';
        
        return message;
    }
}
