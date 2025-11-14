package vaccinesdistribution.Model;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.PositionedObject;
import vaccinesdistribution.Util.Point;

public class Order {
    private static int idCounter = 0;

    private int id;
    private int quantity;
    private Point deliveryLocation;
    private boolean isDispatched = false;
    private boolean isRejected = false;
    private Map<PositionedObject, Integer> dispatchers = new HashMap<>();

    public Order(int quantity, Point deliveryLocation) {
        this.id = idCounter++;
        this.quantity = quantity;
        this.deliveryLocation = deliveryLocation;
    }

    public Order(Order order, int quantity) {
        this.id = order.getId();
        this.quantity = quantity;
        this.deliveryLocation = order.getDeliveryLocation();
        this.isDispatched = order.isDispatched();
        this.isRejected = order.isRejected();
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

    public void setDispatched() {
        isDispatched = true;
    }

    public boolean isRejected() {
        return isRejected;
    }

    public void setRejected() {
        isRejected = true;
    }

    public void computeDispatchers(List<Perishable> dispatchedBatches) {
        PositionedObject identifier;
        for (Perishable batch : dispatchedBatches) {
            identifier = batch.getStoreIdentifier();
            dispatchers.put(
                identifier,
                (dispatchers.get(identifier) == null ? 0 : dispatchers.get(identifier)) + batch.getQuantity());
        }
    }

    @Override
    public String toString() {
        String message = "Order{" +
            "id=" + id +
            ", quantity=" + quantity +
            ", deliveryLocation=" + deliveryLocation +
            ", isDispatched=" + isDispatched +
            ", isRejected=" + isRejected;
        
        if (isDispatched) message += ", dispatchedFrom=" + dispatchers;
        message += '}';
        
        return message;
    }
}
