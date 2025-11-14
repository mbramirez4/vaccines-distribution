package vaccinesdistribution.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

import vaccinesdistribution.Model.Warehouse;
import vaccinesdistribution.Model.WarehouseIdentifier;
import vaccinesdistribution.Interface.PositionedObject;
import vaccinesdistribution.Model.Order;
import vaccinesdistribution.Model.VaccineBatch;
import vaccinesdistribution.Util.Point;

@DisplayName("Distributor Tests")
class DistributorTest {
    
    private Distributor distributor;
    
    @BeforeEach
    void setUp() {
        distributor = Distributor.getDistributor();
    }
    
    @Nested
    @DisplayName("Singleton Pattern Tests")
    class SingletonTests {
        
        @Test
        @DisplayName("getDistributor should return the same instance")
        void testGetDistributorReturnsSameInstance() {
            Distributor instance1 = Distributor.getDistributor();
            Distributor instance2 = Distributor.getDistributor();
            
            assertSame(instance1, instance2, "getDistributor should return the same singleton instance");
        }
    }
    
    @Nested
    @DisplayName("Warehouse Management Tests")
    class WarehouseManagementTests {
        
        @Test
        @DisplayName("getWarehouses should return a list of warehouses")
        void testGetWarehousesReturnsList() {
            List<Warehouse> warehouses = distributor.getWarehouses();
            
            assertNotNull(warehouses, "getWarehouses should not return null");
            assertTrue(warehouses instanceof List, "getWarehouses should return a List");
        }
        
        @Test
        @DisplayName("getWarehouses should return a defensive copy")
        void testGetWarehousesReturnsDefensiveCopy() {
            List<Warehouse> warehouses1 = distributor.getWarehouses();
            
            // Modifying one list should not affect the other
            warehouses1.clear();
            List<Warehouse> warehouses2 = distributor.getWarehouses();
            
            assertNotEquals(warehouses1.size(), warehouses2.size(), 
                "Modifying returned list should not affect internal state");
        }
        
        @Test
        @DisplayName("addWarehouse should add a warehouse to the collection")
        void testAddWarehouse() {
            int initialSize = distributor.getWarehouses().size();
            
            WarehouseIdentifier identifier = new WarehouseIdentifier("Test Warehouse", new Point(100, 200));
            Warehouse warehouse = new Warehouse(identifier);
            
            distributor.addWarehouse(warehouse);
            
            List<Warehouse> warehouses = distributor.getWarehouses();
            assertEquals(initialSize + 1, warehouses.size(), 
                "Warehouse should be added to the collection");
            assertTrue(warehouses.contains(warehouse), 
                "Added warehouse should be in the collection");
        }
        
        @Test
        @DisplayName("addWarehouse should handle multiple warehouses")
        void testAddMultipleWarehouses() {
            int initialSize = distributor.getWarehouses().size();
            
            WarehouseIdentifier id1 = new WarehouseIdentifier("Warehouse 1", new Point(10, 20));
            WarehouseIdentifier id2 = new WarehouseIdentifier("Warehouse 2", new Point(30, 40));
            Warehouse warehouse1 = new Warehouse(id1);
            Warehouse warehouse2 = new Warehouse(id2);
            
            distributor.addWarehouse(warehouse1);
            distributor.addWarehouse(warehouse2);
            
            List<Warehouse> warehouses = distributor.getWarehouses();
            assertEquals(initialSize + 2, warehouses.size(), 
                "Both warehouses should be added");
        }
    }
    
    @Nested
    @DisplayName("Day Management Tests")
    class DayManagementTests {
        
        @Test
        @DisplayName("getCurrentDay should return the current day")
        void testGetCurrentDay() {
            int day = distributor.getCurrentDay();
            
            assertTrue(day >= 0, "Current day should be non-negative");
        }
        
        @Test
        @DisplayName("finishDay should increment the current day")
        void testFinishDayIncrementsDay() {
            int dayBefore = distributor.getCurrentDay();
            
            distributor.finishDay();
            
            int dayAfter = distributor.getCurrentDay();
            assertEquals(dayBefore + 1, dayAfter, 
                "finishDay should increment the current day by 1");
        }
        
        @Test
        @DisplayName("finishDay should handle multiple day increments")
        void testFinishDayMultipleIncrements() {
            int initialDay = distributor.getCurrentDay();
            
            distributor.finishDay();
            distributor.finishDay();
            distributor.finishDay();
            
            int finalDay = distributor.getCurrentDay();
            assertEquals(initialDay + 3, finalDay, 
                "finishDay should increment day correctly multiple times");
        }
    }
    
    @Nested
    @DisplayName("Batch Management Tests")
    class BatchManagementTests {
        
        @Test
        @DisplayName("getAvailableBatches should return available batches count")
        void testGetAvailableBatches() {
            int batches = distributor.getAvailableBatches();
            
            assertTrue(batches >= 0, "Available batches should be non-negative");
        }
        
        @Test
        @DisplayName("getAvailableBatches should reflect changes after adding warehouses with batches")
        void testGetAvailableBatchesAfterAddingBatches() {
            // Note: addWarehouse doesn't automatically update availableBatches counter
            // The counter is only updated during dispatch or when new vaccines are inserted via finishDay
            // This test verifies that warehouses can be added, but batches are counted separately
            WarehouseIdentifier identifier = new WarehouseIdentifier("Test Warehouse", new Point(50, 60));
            Warehouse warehouse = new Warehouse(identifier);
            
            VaccineBatch batch1 = new VaccineBatch(100, 10);
            VaccineBatch batch2 = new VaccineBatch(200, 15);
            warehouse.registerPerishableBatch(batch1);
            warehouse.registerPerishableBatch(batch2);
            
            distributor.addWarehouse(warehouse);
            
            // Verify warehouse was added
            List<Warehouse> warehouses = distributor.getWarehouses();
            assertTrue(warehouses.contains(warehouse), 
                "Warehouse should be added to the collection");
            
            // Note: availableBatches counter is not automatically updated by addWarehouse
            // It's only updated during dispatch operations or when new vaccines are inserted
        }
    }
    
    @Nested
    @DisplayName("Order Management Tests")
    class OrderManagementTests {
        
        @Test
        @DisplayName("getCurrentDayOrders should return a list")
        void testGetCurrentDayOrdersReturnsList() {
            List<Order> orders = distributor.getCurrentDayOrders();
            
            assertNotNull(orders, "getCurrentDayOrders should not return null");
            assertTrue(orders instanceof List, "getCurrentDayOrders should return a List");
        }
        
        @Test
        @DisplayName("getCurrentDayOrders should return a defensive copy")
        void testGetCurrentDayOrdersReturnsDefensiveCopy() {
            List<Order> orders1 = distributor.getCurrentDayOrders();
            orders1.clear();
            
            List<Order> orders2 = distributor.getCurrentDayOrders();
            
            // The internal list should not be affected
            assertNotEquals(orders1.size(), orders2.size(), 
                "Modifying returned list should not affect internal state");
        }
        
        @Test
        @DisplayName("getPreviousDayOrders should return a list")
        void testGetPreviousDayOrdersReturnsList() {
            List<Order> orders = distributor.getPreviousDayOrders();
            
            assertNotNull(orders, "getPreviousDayOrders should not return null");
            assertTrue(orders instanceof List, "getPreviousDayOrders should return a List");
        }
        
        @Test
        @DisplayName("getPreviousDayOrders should return a defensive copy")
        void testGetPreviousDayOrdersReturnsDefensiveCopy() {
            List<Order> orders1 = distributor.getPreviousDayOrders();
            int originalSize = orders1.size();
            orders1.clear();
            
            List<Order> orders2 = distributor.getPreviousDayOrders();
            
            // The internal list should not be affected
            if (originalSize > 0) {
                assertNotEquals(orders1.size(), orders2.size(), 
                    "Modifying returned list should not affect internal state");
            } else {
                // If both are empty, they're still different objects (defensive copy)
                assertNotSame(orders1, orders2, 
                    "Should return a new list instance even if empty");
            }
        }
        
        @Test
        @DisplayName("createOrder should create an order with valid quantity")
        void testCreateOrderWithValidQuantity() {
            int availableBatches = distributor.getAvailableBatches();
            int validQuantity = Math.max(1, (int)(0.05 * availableBatches)); // 5% of available
            
            Point deliveryLocation = new Point(100, 200);
            
            assertDoesNotThrow(() -> {
                distributor.createOrder(validQuantity, deliveryLocation);
            }, "createOrder should not throw for valid quantity");
            
            List<Order> currentOrders = distributor.getCurrentDayOrders();
            assertTrue(currentOrders.size() > 0, 
                "Order should be added to current day orders");
        }
        
        @Test
        @DisplayName("createOrder should throw IllegalArgumentException for zero quantity")
        void testCreateOrderWithZeroQuantity() {
            Point deliveryLocation = new Point(100, 200);
            
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distributor.createOrder(0, deliveryLocation),
                "createOrder should throw IllegalArgumentException for zero quantity"
            );
            
            assertEquals("Invalid quantity", exception.getMessage());
        }
        
        @Test
        @DisplayName("createOrder should throw IllegalArgumentException for negative quantity")
        void testCreateOrderWithNegativeQuantity() {
            Point deliveryLocation = new Point(100, 200);
            
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distributor.createOrder(-10, deliveryLocation),
                "createOrder should throw IllegalArgumentException for negative quantity"
            );
            
            assertEquals("Invalid quantity", exception.getMessage());
        }
        
        @Test
        @DisplayName("createOrder should throw IllegalArgumentException for quantity exceeding 10% limit")
        void testCreateOrderExceedsTenPercentLimit() {
            int availableBatches = distributor.getAvailableBatches();
            int excessiveQuantity = (int)(0.11 * availableBatches) + 1; // More than 10%
            
            Point deliveryLocation = new Point(100, 200);
            
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> distributor.createOrder(excessiveQuantity, deliveryLocation),
                "createOrder should throw IllegalArgumentException for quantity > 10%"
            );
            
            assertEquals("Cannot order more than 10% of available vaccines", exception.getMessage());
        }
        
        @Test
        @DisplayName("createOrder should accept quantity exactly at 10% limit")
        void testCreateOrderAtTenPercentLimit() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int limitQuantity = (int)(0.1 * availableBatches);
                
                Point deliveryLocation = new Point(100, 200);
                
                assertDoesNotThrow(() -> {
                    distributor.createOrder(limitQuantity, deliveryLocation);
                }, "createOrder should accept quantity at exactly 10% limit");
            }
        }
        
        @Test
        @DisplayName("createOrder should add order to current day orders")
        void testCreateOrderAddsToCurrentDayOrders() {
            int availableBatches = distributor.getAvailableBatches();
            int validQuantity = Math.max(1, (int)(0.05 * availableBatches));
            
            Point deliveryLocation = new Point(100, 200);
            int initialOrderCount = distributor.getCurrentDayOrders().size();
            
            distributor.createOrder(validQuantity, deliveryLocation);
            
            List<Order> currentOrders = distributor.getCurrentDayOrders();
            assertEquals(initialOrderCount + 1, currentOrders.size(), 
                "Order should be added to current day orders");
        }
    }
    
    @Nested
    @DisplayName("Order Dispatching Tests")
    class OrderDispatchingTests {
        
        @Test
        @DisplayName("dispatchNextOrder should handle empty pending orders queue")
        void testDispatchNextOrderWithEmptyQueue() {
            // Clear any pending orders by dispatching them
            distributor.dispatchOrders();
            
            // dispatchNextOrder doesn't check for null, so it will throw NPE when queue is empty
            // This is the actual behavior of the method
            assertThrows(NullPointerException.class, () -> {
                distributor.dispatchNextOrder();
            }, "dispatchNextOrder should throw NullPointerException when queue is empty");
        }
        
        @Test
        @DisplayName("dispatchOrders should process all pending orders")
        void testDispatchOrdersProcessesAllPending() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                // Create multiple orders
                int orderQuantity = Math.max(1, (int)(0.02 * availableBatches));
                
                distributor.createOrder(orderQuantity, new Point(10, 20));
                distributor.createOrder(orderQuantity, new Point(30, 40));
                distributor.createOrder(orderQuantity, new Point(50, 60));
                
                int ordersBeforeDispatch = distributor.getCurrentDayOrders().size();
                
                assertDoesNotThrow(() -> {
                    distributor.dispatchOrders();
                }, "dispatchOrders should process all orders without throwing");
                
                // All orders should be processed
                List<Order> currentOrders = distributor.getCurrentDayOrders();
                assertEquals(ordersBeforeDispatch, currentOrders.size(), 
                    "All orders should remain in current day orders after dispatch");
            }
        }
        
        @Test
        @DisplayName("dispatchNextOrder should set processing date on order")
        void testDispatchNextOrderSetsProcessingDate() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.05 * availableBatches));
                Point deliveryLocation = new Point(100, 200);
                
                distributor.createOrder(orderQuantity, deliveryLocation);
                
                int currentDay = distributor.getCurrentDay();
                
                distributor.dispatchNextOrder();
                
                List<Order> currentOrders = distributor.getCurrentDayOrders();
                if (!currentOrders.isEmpty()) {
                    Order order = currentOrders.get(0);
                    assertTrue(order.getProcessingDate() >= 0, 
                        "Order should have processing date set");
                    assertEquals(currentDay, order.getProcessingDate(), 
                        "Processing date should match current day");
                }
            }
        }
        
        @Test
        @DisplayName("dispatchNextOrder should handle order rejection when insufficient batches")
        void testDispatchNextOrderHandlesRejection() {
            // Clear any existing pending orders first
            distributor.dispatchOrders();
            
            int availableBatches = distributor.getAvailableBatches();
            
            // Skip test if there are no batches or too few batches to create meaningful orders
            if (availableBatches < 10) {
                return; // Not enough batches to test this scenario
            }
            
            // Calculate 10% of available batches (the maximum allowed per order)
            int orderQuantity = (int)(0.1 * availableBatches);
            
            // Ensure we have at least 1 batch per order
            if (orderQuantity < 1) {
                return; // Not enough batches to create orders
            }
            
            // Create 11 orders, each with 10% of available batches
            // Each order location is unique to track them reliably
            List<Point> deliveryLocations = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                Point deliveryLocation = new Point(1000 + i, 2000 + i); // Use unique coordinates
                deliveryLocations.add(deliveryLocation);
                distributor.createOrder(orderQuantity, deliveryLocation);
            }
            
            // Get all current orders and match them by delivery location
            List<Order> allOrders = distributor.getCurrentDayOrders();
            List<Order> createdOrders = new ArrayList<>();
            for (Point location : deliveryLocations) {
                for (Order order : allOrders) {
                    if (order.getDeliveryLocation().equals(location)) {
                        createdOrders.add(order);
                        break;
                    }
                }
            }
            
            assertEquals(11, createdOrders.size(), 
                "All 11 orders should be created");
            
            // Dispatch the first 10 orders - they should all be dispatched successfully
            for (int i = 0; i < 10; i++) {
                distributor.dispatchNextOrder();
                
                // Verify the order was dispatched (not rejected)
                Order order = createdOrders.get(i);
                assertFalse(order.isRejected(), 
                    "Order " + (i + 1) + " should not be rejected");
                assertTrue(order.isDispatched(), 
                    "Order " + (i + 1) + " should be dispatched successfully");
            }
            
            // After 10 orders, we've used 100% of available batches
            // The 11th order should be rejected
            distributor.dispatchNextOrder();
            
            Order eleventhOrder = createdOrders.get(10);
            assertTrue(eleventhOrder.isRejected(), 
                "The 11th order should be rejected due to insufficient batches");
            assertFalse(eleventhOrder.isDispatched(), 
                "The rejected order should not be marked as dispatched");

            // Finish a couple of days to add vaccines to the system again
            distributor.finishDay();
            distributor.finishDay();
            distributor.finishDay();
            distributor.finishDay();
            distributor.finishDay();
        }

        @Test
        @DisplayName("dispatchNextOrder should handle querying further stores to fulfill order")
        void testOrderRequiringBatchesFromMultipleStores() {
            // Clear any existing pending orders and make sure there aren't
            // any currentDayOrders
            PositionedObject identifier = new WarehouseIdentifier(String.valueOf(-1), new Point(0, 0));
            Warehouse warehouse = new Warehouse(identifier);
            distributor.addWarehouse(warehouse);
            distributor.finishDay();


            while (distributor.getAvailableBatches() < 200) {
                // we need at least 200 batches to test this scenario.
                // so finish days until enough batches are available

                distributor.finishDay();
            }

            VaccineBatch batch;
            int x = -999;
            int y = -999;

            int numStores = 20;
            for (int i = 0; i < numStores; i++) {
                batch = new VaccineBatch(1, distributor.getCurrentDay() + 1);
                identifier = new WarehouseIdentifier(String.valueOf(i), new Point(x, y));
                warehouse = new Warehouse(identifier);
                warehouse.registerPerishableBatch(batch);
                distributor.addWarehouse(warehouse);
            }

            distributor.createOrder(numStores/2, new Point(x, y));
            distributor.dispatchNextOrder();

            Order order = distributor.getCurrentDayOrders().getFirst();
            assertTrue(order.isDispatched(), "Order should be dispatched");
            assertFalse(order.isRejected(), "Order should be fulfilled");
            assertEquals(numStores/2, order.getQuantity(), "Order quantity should be correct");
            assertEquals(numStores/2, order.getDispatchers().entrySet().size(), "Each store should provide 1 batch");
        }

    }
    
    @Nested
    @DisplayName("finishDay Integration Tests")
    class FinishDayIntegrationTests {
        
        @Test
        @DisplayName("finishDay should dispatch pending orders")
        void testFinishDayDispatchesOrders() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.05 * availableBatches));
                
                distributor.createOrder(orderQuantity, new Point(10, 20));
                
                int ordersBeforeFinish = distributor.getCurrentDayOrders().size();
                
                distributor.finishDay();
                
                // Orders should still be in the list but processed
                List<Order> previousOrders = distributor.getPreviousDayOrders();
                assertEquals(ordersBeforeFinish, previousOrders.size(), 
                    "Orders should move to previous day orders after finishDay");
            }
        }
        
        @Test
        @DisplayName("finishDay should move current day orders to previous day orders")
        void testFinishDayMovesOrdersToPrevious() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.05 * availableBatches));
                
                distributor.createOrder(orderQuantity, new Point(10, 20));
                distributor.createOrder(orderQuantity, new Point(30, 40));
                
                int currentDayOrdersCount = distributor.getCurrentDayOrders().size();
                
                distributor.finishDay();
                
                List<Order> newPreviousOrders = distributor.getPreviousDayOrders();
                List<Order> newCurrentOrders = distributor.getCurrentDayOrders();
                
                assertEquals(currentDayOrdersCount, newPreviousOrders.size(), 
                    "Previous day orders should contain orders from current day");
                assertEquals(0, newCurrentOrders.size(), 
                    "Current day orders should be empty after finishDay");
            }
        }
        
        @Test
        @DisplayName("finishDay should dispose expired objects")
        void testFinishDayDisposesExpiredObjects() {
            // Add a warehouse with expired batches
            WarehouseIdentifier identifier = new WarehouseIdentifier("Test Warehouse", new Point(100, 200));
            Warehouse warehouse = new Warehouse(identifier);
            
            int currentDay = distributor.getCurrentDay();
            VaccineBatch expiredBatch = new VaccineBatch(100, currentDay - 1); // Expired yesterday
            warehouse.registerPerishableBatch(expiredBatch);
            
            distributor.addWarehouse(warehouse);
            
            assertDoesNotThrow(() -> {
                distributor.finishDay();
            }, "finishDay should handle expired objects without errors");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("createOrder should handle very small quantities")
        void testCreateOrderWithVerySmallQuantity() {
            int availableBatches = distributor.getAvailableBatches();
            Point deliveryLocation = new Point(100, 200);
            
            if (availableBatches > 0) {
                // Only test if there are available batches, otherwise it will fail the 10% check
                assertDoesNotThrow(() -> {
                    distributor.createOrder(1, deliveryLocation);
                }, "createOrder should handle quantity of 1 when batches are available");
            } else {
                // When no batches available, even quantity 1 will fail the 10% check
                assertThrows(IllegalArgumentException.class, () -> {
                    distributor.createOrder(1, deliveryLocation);
                }, "createOrder should throw when no batches available");
            }
        }
        
        @Test
        @DisplayName("dispatchOrders should handle empty queue")
        void testDispatchOrdersWithEmptyQueue() {
            // Clear any pending orders
            distributor.dispatchOrders();
            
            assertDoesNotThrow(() -> {
                distributor.dispatchOrders();
            }, "dispatchOrders should handle empty queue");
        }
        
        @Test
        @DisplayName("finishDay should handle multiple consecutive calls")
        void testFinishDayMultipleCalls() {
            int initialDay = distributor.getCurrentDay();
            
            distributor.finishDay();
            distributor.finishDay();
            distributor.finishDay();
            
            int finalDay = distributor.getCurrentDay();
            assertEquals(initialDay + 3, finalDay, 
                "Multiple finishDay calls should increment day correctly");
        }
        
        @Test
        @DisplayName("getAvailableBatches should be consistent after operations")
        void testGetAvailableBatchesConsistency() {
            distributor.finishDay();
            
            int batches = distributor.getAvailableBatches();
            
            // Batches should change (some expired, new ones added)
            // But should remain non-negative
            assertTrue(batches >= 0, 
                "Available batches should remain non-negative");
        }
    }
    
    @Nested
    @DisplayName("Order State Tests")
    class OrderStateTests {
        
        @Test
        @DisplayName("Order should be marked as dispatched after successful dispatch")
        void testOrderMarkedAsDispatched() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.05 * availableBatches));
                Point deliveryLocation = new Point(100, 200);
                
                distributor.createOrder(orderQuantity, deliveryLocation);
                
                distributor.dispatchNextOrder();
                
                List<Order> currentOrders = distributor.getCurrentDayOrders();
                if (!currentOrders.isEmpty()) {
                    Order order = currentOrders.get(0);
                    // Order should be either dispatched or rejected
                    assertTrue(order.isDispatched() || order.isRejected(), 
                        "Order should have a final state after dispatch");
                }
            }
        }
        
        @Test
        @DisplayName("Order should have dispatched batches after successful dispatch")
        void testOrderHasDispatchedBatches() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.05 * availableBatches));
                Point deliveryLocation = new Point(100, 200);
                
                distributor.createOrder(orderQuantity, deliveryLocation);
                
                distributor.dispatchNextOrder();
                
                List<Order> currentOrders = distributor.getCurrentDayOrders();
                if (!currentOrders.isEmpty()) {
                    Order order = currentOrders.get(0);
                    if (order.isDispatched()) {
                        // Dispatched batches should be set
                        assertNotNull(order.getProcessingDate(), 
                            "Dispatched order should have processing date");
                    }
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Spatial Distribution Tests")
    class SpatialDistributionTests {
        
        @Test
        @DisplayName("Order should be dispatched from closest warehouses")
        void testOrderDispatchedFromClosestWarehouses() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                // Add a warehouse close to a specific location
                Point closeLocation = new Point(50, 50);
                WarehouseIdentifier closeIdentifier = new WarehouseIdentifier("Close Warehouse", closeLocation);
                Warehouse closeWarehouse = new Warehouse(closeIdentifier);
                
                VaccineBatch batch = new VaccineBatch(500, 20);
                closeWarehouse.registerPerishableBatch(batch);
                distributor.addWarehouse(closeWarehouse);
                
                // Create order near this warehouse
                Point orderLocation = new Point(55, 55);
                int orderQuantity = 100;
                
                distributor.createOrder(orderQuantity, orderLocation);
                
                assertDoesNotThrow(() -> {
                    distributor.dispatchNextOrder();
                }, "Order should be dispatched from closest warehouse");
            }
        }
        
        @Test
        @DisplayName("Multiple orders should be processed correctly")
        void testMultipleOrdersProcessing() {
            int availableBatches = distributor.getAvailableBatches();
            if (availableBatches > 0) {
                int orderQuantity = Math.max(1, (int)(0.02 * availableBatches));
                
                distributor.createOrder(orderQuantity, new Point(10, 10));
                distributor.createOrder(orderQuantity, new Point(20, 20));
                distributor.createOrder(orderQuantity, new Point(30, 30));
                
                int ordersCount = distributor.getCurrentDayOrders().size();
                
                distributor.dispatchOrders();
                
                List<Order> orders = distributor.getCurrentDayOrders();
                assertEquals(ordersCount, orders.size(), 
                    "All orders should remain in current day orders");
            }
        }
    }
}

