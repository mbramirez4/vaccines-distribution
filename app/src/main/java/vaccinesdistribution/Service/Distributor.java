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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.SpatialCollection;
import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.Order;
import vaccinesdistribution.Model.VaccineBatch;
import vaccinesdistribution.Util.ArraySpatialCollection;
import vaccinesdistribution.Util.Point;

public class Distributor {
    private static final Logger logger = LogManager.getLogger(Distributor.class);
    private static final Logger timeLogger = LogManager.getLogger("times");

    private static final Distributor distributor = new Distributor();
    private static final String WAREHOUSE_FILE_PATH = "app/src/main/resources/warehouses_storage.json";


    private SpatialCollection<Warehouse> stores = new ArraySpatialCollection<>();
    private List<Order> historicOrders = new ArrayList<>();
    private List<Order> previousDayOrders = new ArrayList<>();
    private List<Order> currentDayOrders = new ArrayList<>();
    private Queue<Order> pendingOrders = new ArrayDeque<>();
    
    private int availableBatches;
    private int currentDay;

    private Distributor() {
        availableBatches = 0;
        currentDay = 0;
        try {
            stores.setItemsFromList(
                Storage.loadWarehousesFromJsonFile(WAREHOUSE_FILE_PATH)
            );
            logger.info("Warehouses loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load warehouses from file " + e);
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

    public List<Order> getPreviousDayOrders() {
        return new ArrayList<>(previousDayOrders);
    }

    public void finishDay() {
        dispatchOrders();
        currentDay++;

        disposeExpiredObjects();
        insertNewVaccines();
        setPreviousDayOrders();
    }

    public void dispatchNextOrder() {
        Order order;
        List<Perishable> dispatchedBatches;

        long startTime = System.currentTimeMillis();

        order = pendingOrders.poll();
        logger.info("Order dispatching started " + order);
        dispatchedBatches = dispatchOrder(order);
        
        order.setDispatchedBatches(dispatchedBatches);
        logger.info("Dispatchers sucessfully computed");
        logger.info("Order dispatching finished " + order);

        long endTime = System.currentTimeMillis();

        order.setProcessingDate(currentDay);
        historicOrders.add(order);
        timeLogger.info("Order dispatching time: " + (endTime - startTime) + "ms");
        
        if (order.isRejected()) return;

        if (order.getQuantity() != computeBatchSize(dispatchedBatches)){
            logger.error("Dispatched quantity " + computeBatchSize(dispatchedBatches) + " does not match order quantity for not rejected order. Order: " + order + "\nDispatched: " + dispatchedBatches);
            throw new RuntimeException("Dispatched quantity does not match order quantity for not rejected order. Order: " + order);
        }
    }

    public void dispatchOrders() throws RuntimeException {
        while (!pendingOrders.isEmpty()) {
            dispatchNextOrder();
        }
    }

    public void createOrder(int quantity, Point deliveryLocation) throws IllegalArgumentException {
        if (quantity <= 0) {
            logger.warn("Attempted to create order with invalid quantity: " + quantity);
            throw new IllegalArgumentException("Invalid quantity");
        }
        if (quantity > 0.1 * availableBatches) {
            logger.warn("Attempted to create order to " + deliveryLocation + " with too many vaccines: " + quantity);
            throw new IllegalArgumentException("Cannot order more than 10% of available vaccines");
        }

        Order order = new Order(quantity, deliveryLocation);
        addOrder(order);
        logger.info("Order created successfully " + order);
        logger.debug("Daily orders updated " + pendingOrders);
    }

    private void addOrder(Order order) {
        pendingOrders.add(order);
        currentDayOrders.add(order);
    }
    
    private List<Perishable> dispatchOrder(Order order) {
        List<Perishable> totalDispatchedBatches = new ArrayList<>();
        // The List<Perishable> is passed by reference to avoid creating a new
        // object every time. This should reduce memory consumption
        return dispatchOrder(order, 5, totalDispatchedBatches);
    }

    private List<Perishable> dispatchOrder(Order order, int closestNeighboursSize, List<Perishable> totalDispatchedBatches) {
        int quantity = order.getQuantity();
        if (quantity > availableBatches) {
            order.setRejected();
            logger.info("Not enough batches available to dispatch order " + order);
            return totalDispatchedBatches;
        }
        
        logger.info("Started computation of " + closestNeighboursSize + " closest stores to the deliveryLocation");
        // long initialTime = System.nanoTime();
        long initialTime = System.currentTimeMillis();
        List<Warehouse> closestStores = stores.getKClosestItems(order.getDeliveryLocation(), closestNeighboursSize);
        long finalTime = System.currentTimeMillis();
        timeLogger.info("Computation of " + closestNeighboursSize + " closest stores: " + (finalTime - initialTime) + "ms");
        logger.debug("Closest stores found for order " + closestStores);
        
        // Sort the stores based on the priority of their perishables
        Perishable topPriorityObject;
        Map<Integer, Warehouse> storeMap = new HashMap<>();
        PriorityQueue<Perishable> vaccineBatches = new PriorityQueue<>();
        for (Warehouse warehouse : closestStores) {
            topPriorityObject = warehouse.getTopPriorityObject();
            if (topPriorityObject == null || warehouse.getAvailableBatches() == 0) continue;

            storeMap.put(warehouse.getIdentifier().getId(), warehouse);
            vaccineBatches.add(topPriorityObject);
        }
        logger.debug("Successfully sorted stores based on their top priority perishable " + vaccineBatches);

        int dispatchedQuantity;
        Perishable batch;
        Warehouse dispatcherWarehouse;
        List<Perishable> dispatchedBatches;

        logger.debug("Dequeuing started. Current available batches: " + availableBatches);
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
        logger.debug("Dequeuing finished. Current available batches: " + availableBatches + ", batches missing: " + quantity);

        // if there is no more quantity to dispatch or the neighborhood size
        // is greater than the number of warehouses in the system then return
        if (quantity <= 0) {
            order.setDispatched();
            return totalDispatchedBatches;
        }

        if (closestNeighboursSize >= stores.size()) {
            logger.error("There wasn't enough batches in the system to fully dispatch order " + order);
            throw new RuntimeException("There wasn't enough batches in the system to fully dispatch order " + order);
        }

        Order newOrder = new Order(order, quantity);
        logger.info("Not enough batches available to dispatch order from the " + closestNeighboursSize + " closest stores.");
        logger.info("New order created to dispatch remaining quantity using the " + 2*closestNeighboursSize + " closest stores", newOrder);

        logger.debug("totalDispatchedBatches BEFORE dispatching newOrder is " + totalDispatchedBatches);
        dispatchOrder(newOrder, 2 * closestNeighboursSize, totalDispatchedBatches);
        logger.debug("totalDispatchedBatches AFTER dispatching newOrder is " + totalDispatchedBatches);

        if (newOrder.isRejected()) {
            order.setRejected();
            return totalDispatchedBatches;
        }

        if (newOrder.isDispatched()) order.setDispatched();
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
        logger.info("Disposition of expired objects started");

        long initialTime = System.currentTimeMillis();
        for (Warehouse Warehouse : stores.getItems()) {
            Warehouse.disposeExpiredObjects(currentDay);
        }
        long finalTime = System.currentTimeMillis();
        timeLogger.info("Disposition of expired objects time: " + (finalTime - initialTime) + "ms");
        logger.info("Disposition of expired objects finished");
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

        logger.info("Insertion of new vaccines started. Current available batches: " + availableBatches);

        long initialTime = System.currentTimeMillis();
        for (int i = 0; i < nBatches; i++) {
            batchSize = random.nextInt(151) + 50;
            daysToExpire = random.nextInt(21) + 10;
            
            batch = new VaccineBatch(batchSize, currentDay + daysToExpire);
            randomWarehouse = warehouseList.get(random.nextInt(warehouseList.size()));            
            randomWarehouse.registerPerishableBatch(batch);
            
            availableBatches += batchSize;
        }
        long finalTime = System.currentTimeMillis();
        timeLogger.info("Insertion of new vaccines time: " + (finalTime - initialTime) + "ms");
        logger.info("Insertion of new vaccines finished. " + nBatches + " batches inserted. Current available batches: " + availableBatches);
    }

    private void setPreviousDayOrders() {
        previousDayOrders = new ArrayList<>(currentDayOrders);
        currentDayOrders = new ArrayList<>();
    }
}
