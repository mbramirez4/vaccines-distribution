package vaccinesdistribution.Service;

import java.util.Map;
import java.util.List;
import java.util.Queue;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Random;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.SpatialCollection;
import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.Order;
import vaccinesdistribution.Model.VaccineBatch;
import vaccinesdistribution.Util.ArraySpatialCollection;
import vaccinesdistribution.Util.Point;

public class Distributor {
    private static final Distributor distributor = new Distributor();
    private static final String WAREHOUSE_FILE_PATH = "app/src/main/resources/warehouses_storage.json";

    private SpatialCollection<Warehouse> stores = new ArraySpatialCollection<>();
    private Queue<Order> dailyOrders = new ArrayDeque<>();
    
    private int availableBatches;
    private int currentDay;

    private Distributor() {
        availableBatches = 0;
        currentDay = 0;
        try {
            stores.setItemsFromList(
                Storage.loadWarehousesFromJsonFile(WAREHOUSE_FILE_PATH)
            );
        } catch (IOException e) {
            System.out.println("Error loading warehouses from JSON file: " + e.getMessage());
            // Implement logs
        }

        for (Warehouse warehouse : stores.getItems()) {
            availableBatches += warehouse.getAvailableBatches();
        }
    }

    public static Distributor getDistributor() {
        return distributor;
    }

    public List<Warehouse> getWarehouses() {
        return stores.getItems();
    }

    public void addWarehouse(Warehouse warehouse) {
        stores.add(warehouse);
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public int getAvailableBatches() {
        return availableBatches;
    }

    public void finishDay() {
        dispatchOrders();
        currentDay++;

        disposeExpiredObjects();
        insertNewVaccines();
    }

    public void dispatchNextOrder() {
        Order order;
        List<Perishable> dispatchedBatches;

        order = dailyOrders.poll();
        dispatchedBatches = dispatchOrder(order);
        
        if (order.isRejected()) return;

        if (order.getQuantity() != computeBatchSize(dispatchedBatches)){
            throw new RuntimeException("Dispatched quantity does not match order quantity for not rejected order. Order: " + order);
        }
    }

    public void dispatchOrders() throws RuntimeException {
        while (!dailyOrders.isEmpty()) {
            dispatchNextOrder();
        }
    }

    public void createOrder(int quantity, Point deliveryLocation) throws IllegalArgumentException {
        if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity");
        if (quantity > 0.1 * availableBatches) throw new IllegalArgumentException("Cannot order more than 10% of available vaccines");

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
        if (quantity > availableBatches) {
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

            dispatchedQuantity = computeBatchSize(dispatchedBatches);
            quantity -= dispatchedQuantity;
            availableBatches -= dispatchedQuantity;
        }

        // if there is no more quantity to dispatch or the neighborhood size
        // is greater than the number of warehouses in the system then return
        if (quantity <= 0) {
            order.setDispatched();
            return totalDispatchedBatches;
        }

        if (neighborhoodSize >= stores.size()) {
            throw new RuntimeException("Neighborhood size is greater than the number of warehouses in the system and the order cannot be totally dispatched");
        }

        Order newOrder = new Order(order, quantity);
        totalDispatchedBatches.addAll(dispatchOrder(newOrder, 2 * neighborhoodSize, totalDispatchedBatches));

        return totalDispatchedBatches;
    }

    private static int computeBatchSize(List<Perishable> dispatchedBatches) {
        int quantity = 0;
        for (Perishable batch : dispatchedBatches) {
            quantity += batch.getQuantity();
        }
        return quantity;
    }

    private void disposeExpiredObjects() {
        for (Warehouse Warehouse : stores.getItems()) {
            Warehouse.disposeExpiredObjects(currentDay);
        }
    }

    private void insertNewVaccines() {
        Random random = new Random();
        // Number of batches of vaccines to insert in the the system. A
        // random number between 10 and 50.
        int nBatches = random.nextInt(41) + 10;
        
        List<Warehouse> warehouseList = stores.getItems();
        if (warehouseList.isEmpty()) {
            return;
        }

        int batchSize; // It'll be a random number between 50 and 200 for each batch.
        int daysToExpire; // It'll be a random number between 10 and 30 for each batch.
        VaccineBatch batch;
        Warehouse randomWarehouse;
        for (int i = 0; i < nBatches; i++) {
            batchSize = random.nextInt(151) + 50;
            daysToExpire = random.nextInt(21) + 10;
            
            batch = new VaccineBatch(batchSize, currentDay + daysToExpire);
            randomWarehouse = warehouseList.get(random.nextInt(warehouseList.size()));            
            randomWarehouse.registerPerishableBatch(batch);
            
            availableBatches += batchSize;
        }
    }
}
