# Vaccine Distribution System

## Project Description

This project emulates a vaccine distribution system. It manages warehouses, orders, and the spatial distribution of perishable vaccines. The system includes an interactive graphical interface to visualize warehouses and orders on a map, as well as backend functionalities to manage business logic.

The main problem in this emulation is the spatial distribution of perishable vaccines. When thinking about perishable objects, you might want to distribute those batches that are about to expire first. However, from a logistical perspective, you might want to distribute the vaccines from the warehouses that are closest to the order's delivery location.

Since the distance between warehouses and delivery locations is not constant, we can't combine these two variables into a single one to create a single priority queue, and creating a priority queue for each placed order would be inefficient. Instead, a new Abstract Data Type (ADT) is proposed to solve this problem: the SpatialCollection.

The SpatialCollection attempts to solve this problem by getting the k closest items to a given point. In this case, the k closest warehouses to a given delivery location. Then, these warehouses are ordered based on the expiration date of the batch with the closest expiration date.

The project is fully implemented in Java, using the Gradle build system.

## ⚠️ AI usage disclaimer
An AI-backed code assistant was extensively used to help the authors with the implementation of the Frontend and Unit Tests. This code was reviewed and extensively tested by the authors to ensure it was functional.

The functionalities added with the help of the AI assistant are not merged into the `master` branch. The full functionality of the project is in the `Add-user-interface-for-the-distribution-emulator` branch.

## Table of Contents

1. [Backend and Data Structures](#backend-and-data-structures)
   - [Implemented Data Structures](#1-use-of-data-structures)
   - [Unit Tests](#2-unit-tests-core-logic)
   - [Design Patterns](#3-design-patterns)
   - [Sorting Algorithm](#4-sorting-algorithm)
2. [UI/UX](#uiux)
3. [Installation and Usage](#installation-and-usage)

---

## Backend and Data Structures

### 1. Use of Data Structures

The project implements multiple data structures that are vital for the system's operation:

#### 1.1. PriorityQueue (Priority Queue)

**Location**: 
- `Warehouse.vaccineBatches` - Stores vaccine batches ordered by expiration date.
- `ArraySpatialCollection.getKClosestItems()` - Max-heap of size k to find the k closest elements to a given point. Used in `Distributor.dispatchOrder()` to find the warehouses closest to an order's delivery location. This is considered the best structure for this purpose as it avoids sorting the entire collection.

#### 1.2. Deque (ArrayDeque) - Double-Ended Queue

**Location**: 
- `Distributor.historicOrders` - History of processed orders. Used as a Stack to maintain the chronological order of processed orders. Chosen as the data structure because it provides efficient access to the last added element (O(1)) and insertion/deletion at the end (O(1)).
- `Distributor.pendingOrders` - Queue of pending orders. Chosen as the data structure because it provides efficient access to the last added element (O(1)) and insertion/deletion at both ends (O(1)).

#### 1.3. HashMap (Hash Table)

**Location**: 
- `Distributor.dispatchOrder()` - Maps warehouse IDs to Warehouse objects. Here, it was needed fast access (O(1)) to warehouses by their ID after ordering them by priority. A `HashMap` allows this efficient search without needing to iterate over the list of closest warehouses.
- `Order.dispatchers` - Maps warehouses to dispatched quantities. Allows tracking which warehouses contributed to an order and in what quantity, facilitating report generation and distribution tracking.

---

### 2. Unit Tests (Core Logic)

The project includes exhaustive unit tests that validate complex business logic:

**Location**: `app/src/test/java/vaccinesdistribution/Service/DistributorTest.java`

#### Types of Tests Implemented:

1. **Singleton Pattern Tests**: Verify that only one instance of Distributor exists
2. **Warehouse Management Tests**: Validate addition and retrieval of warehouses
3. **Order Management Tests**: Validate order creation with business validations
4. **Order Dispatch Tests**: Validate the vaccine distribution algorithm:
   - Dispatch from closest warehouses
   - Prioritization by expiration date
   - Handling of partial orders
   - Rejection of orders when there are insufficient vaccines
5. **Spatial Distribution Tests**: Validate the k-nearest neighbors search algorithm
6. **Day Completion Tests**: Validate the complete flow of:
   - Dispatching pending orders
   - Disposing expired objects
   - Inserting new vaccines

---

### 3. Design Patterns

The project implements several design patterns:

#### 3.1. Singleton Pattern

**Location**: `Distributor.java`

Guarantees a single instance of the distribution service throughout the application, avoiding state inconsistencies and allowing controlled global access.

#### 3.2. Strategy Pattern

**Location**: `SpatialCollection` interface and `ArraySpatialCollection` implementation

Allows swapping different implementations of spatial collections (for example, could be implemented with a KD-Tree) without modifying client code (`Distributor`).

#### 3.3. Adapter Pattern

**Location**: `Storage.WarehouseDeserializer`

**Justification**: Adapts the external JSON structure to the internal domain model (`Warehouse`), allowing the system to work with different input formats without modifying business code.

---

### 4. Sorting Algorithm

The project uses *insertion-sort* implictly from the PriorityQueue data structure used to store the k closest warehouses to an order's delivery location:

**Location**: `ArraySpatialCollection.getKClosestItems()`

Uses a max-heap to maintain the k closest elements without needing to sort the entire collection which is more efficient than sorting the entire list.

This sorting is used in `Distributor.dispatchOrder()` to find the warehouses closest to an order's delivery location, optimizing geographic distribution.

---

## UI/UX

The graphical interface is functional, usable, and useful:

- **MapPanel**: Interactive map visualization with:
  - Zoom with mouse wheel (centered on cursor)
  - Pan by dragging with the mouse
  - Grid with coordinates
  - Informative tooltips when hovering over warehouses and orders
  - Reset button to restore the initial view
  - Differentiated colors for orders: orange (pending), green (dispatched), red (rejected), gray (previous day)

- **SidePanel**: Side panel with:
  - Form to create orders (X, Y coordinates and quantity)
  - Buttons to dispatch orders (next or all)
  - Button to finish the day
  - Message area with scroll to view operation history

### Frontend-Backend Connection

The frontend correctly connects to the backend through consumption of the `Distributor` methods to:
  - Create orders (`createOrder`)
  - Dispatch orders (`dispatchNextOrder`, `dispatchOrders`)
  - Finish days (`finishDay`)
  - Get information (`getWarehouses`, `getCurrentDayOrders`, `getPreviousDayOrders`)

The frontend automatically updates after each operation through `refreshMap()`

---

## Installation and Usage

### Installation

Clone the repository:
```bash
git clone https://github.com/mbramirez4/vaccines-distribution
cd vaccines-distribution
```

Checkout to the branch with the full functionality:
```bash
git checkout Add-user-interface-for-the-distribution-emulator
```

From the IDE, run the `VaccineDistributionUI.main()` class.

### Application Usage

1. **Create an Order**:
   - Enter X and Y coordinates in the side panel
   - Enter the required quantity of vaccines
   - Click "Create Order"

2. **Dispatch Orders**:
   - "Dispatch Next Order": Dispatches the next order in the queue
   - "Dispatch All Orders": Dispatches all pending orders

3. **Finish Day**:
   - Click "Finish Day" to:
     - Dispatch all pending orders
     - Advance to the next day
     - Discard expired vaccines
     - Insert new random vaccine batches

4. **Interact with the Map**:
   - **Zoom**: Use the mouse wheel
   - **Pan**: Drag with the left mouse button
   - **Tooltips**: Hover over warehouses or orders to see information
   - **Reset**: Click "Reset View" to restore the initial view
