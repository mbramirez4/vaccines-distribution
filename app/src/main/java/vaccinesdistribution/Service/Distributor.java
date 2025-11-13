package vaccinesdistribution.Service;

import java.util.Map;
import java.util.List;
import java.util.Queue;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.SpatialCollection;
import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.Order;
import vaccinesdistribution.Util.ArraySpatialCollection;
import vaccinesdistribution.Util.Point;

public class Distributor {
    private static final Distributor distributor = new Distributor();

    private SpatialCollection<Warehouse> stores = new ArraySpatialCollection<>();
    private Queue<Order> dailyOrders = new ArrayDeque<>();
    
    private int vaccinesAvailable;
    private int currentDay;

    private Distributor() {
        vaccinesAvailable = 0;
        currentDay = 0;
    }

    public static Distributor getDistributor() {
        return distributor;
    }

    public void finishDay() {
        dispatchOrders();
        currentDay++;

        disposeExpiredObjects();
        insertNewVaccines();
    }

    public void createOrder(int quantity, Point deliveryLocation) throws IllegalArgumentException {
        if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity");
        if (quantity > 0.1 * vaccinesAvailable) throw new IllegalArgumentException("Cannot order more than 10% of available vaccines");

        addOrder(new Order(quantity, deliveryLocation));
    }

    private void addOrder(Order order) {
        dailyOrders.add(order);
    }
    
    private List<Perishable> dispatchOrder(Order order) {
        List<Perishable> totalDispatchedBatches = new ArrayList<>();
        // The List<Perishable> is passed by reference to avoid creating a new
        // object every time. This should reduce memory consumption
        return dispatchOrder(order, 5, totalDispatchedBatches);
    }

    private List<Perishable> dispatchOrder(Order order, int neighborhoodSize, List<Perishable> totalDispatchedBatches) {
        int quantity = order.getQuantity();
        if (quantity > vaccinesAvailable) {
            order.setRejected();
            return totalDispatchedBatches;
        }

        PriorityQueue<Perishable> vaccineBatches = new PriorityQueue<>();
        Map<Integer, Warehouse> storeMap = new HashMap<>();
        
        // Sort the stores based on the priority of their perishables
        List<Warehouse> closestStores = stores.getKClosestItems(order.getDeliveryLocation(), neighborhoodSize);
        for (Warehouse Warehouse : closestStores) {
            storeMap.put(Warehouse.getIdentifier().getId(), Warehouse);
            vaccineBatches.add(Warehouse.getTopPriorityObject());
        }

        int dispatchedQuantity;
        Perishable batch;
        Warehouse dispatcherWarehouse;
        List<Perishable> dispatchedBatches;
        while (quantity > 0 && !vaccineBatches.isEmpty()) {
            // get the Warehouse with the top priority perishable and dispatch
            // as much as possible from it
            batch = vaccineBatches.poll();
            dispatcherWarehouse = storeMap.get(batch.getStorageId());

            dispatchedBatches = dispatcherWarehouse.dispatch(quantity);
            totalDispatchedBatches.addAll(dispatchedBatches);

            dispatchedQuantity = getDispatchedQuantity(dispatchedBatches);
            quantity -= dispatchedQuantity;
            vaccinesAvailable -= dispatchedQuantity;
        }

        // if there is no more quantity to dispatch or the neighborhood size
        // is greater than the number of warehouses in the system then return
        if (quantity <= 0 || neighborhoodSize >= stores.size()) return totalDispatchedBatches;

        Order newOrder = new Order(order, quantity);
        totalDispatchedBatches.addAll(dispatchOrder(newOrder, 2 * neighborhoodSize, totalDispatchedBatches));

        return totalDispatchedBatches;
    }

    private static int getDispatchedQuantity(List<Perishable> dispatchedBatches) {
        int quantity = 0;
        for (Perishable batch : dispatchedBatches) {
            quantity += batch.getQuantity();
        }
        return quantity;
    }

    private void dispatchOrders() throws RuntimeException {
        Order order;
        List<Perishable> dispatchedBatches;
        while (!dailyOrders.isEmpty()) {
            order = dailyOrders.poll();
            dispatchedBatches = dispatchOrder(order);
            
            if (order.isRejected()) continue;

            if (order.getQuantity() != getDispatchedQuantity(dispatchedBatches)){
                throw new RuntimeException("Dispatched quantity does not match order quantity for not rejected order. Order: " + order);
            }

            order.setDispatched();
        }
    }

    private void disposeExpiredObjects() {
        for (Warehouse Warehouse : stores.getItems()) {
            Warehouse.disposeExpiredObjects(currentDay);
        }
    }

    private void insertNewVaccines() {

        // for (Warehouse Warehouse : stores) {
        //     Warehouse.insertNewVaccines();
        // }
    }
}
