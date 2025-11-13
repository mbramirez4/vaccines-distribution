package vaccinesdistribution.Model;

import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.PositionedObject;

public class Warehouse {
    private PositionedObject identifier;
    private List<Perishable> expiredBatches = new ArrayList<>();
    private PriorityQueue<Perishable> vaccineBatches = new PriorityQueue<>();

    @Override
    public String toString() {
        return identifier.toString();
    }

    public Warehouse(PositionedObject identifier) {
        this.identifier = identifier;
    }

    public PositionedObject getIdentifier() {
        return identifier;
    }

    public Perishable getTopPriorityObject() {
        return vaccineBatches.peek();
    }

    public List<Perishable> dispatch(int quantity) {
        Perishable batch;
        Perishable dispatchedBatch;
        List<Perishable> dispatchedBatches = new ArrayList<>();
        
        while (quantity > 0 && !vaccineBatches.isEmpty()) {
            batch = getTopPriorityObject();
            if (batch.isExpired()) {
                vaccineBatches.poll();
                continue;
            };

            dispatchedBatch = batch.dispatch(quantity);
            dispatchedBatches.add(dispatchedBatch);

            quantity -= dispatchedBatch.getQuantity();
            if (batch.getQuantity() <= 0) {
                vaccineBatches.poll();
            }
        }

        return dispatchedBatches;
    }

    public void disposeExpiredObjects(int currentDate){
        Perishable batch;
        while (true) {
            batch = getTopPriorityObject();
            if (batch == null || batch.getExpirationDate() > currentDate) break;

            batch.setExpired();
            expiredBatches.add(batch);
            vaccineBatches.poll();
            continue;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warehouse Warehouse = (Warehouse) o;

        return identifier.equals(Warehouse.getIdentifier());
    }
}
