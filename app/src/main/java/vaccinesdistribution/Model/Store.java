package vaccinesdistribution.Model;

import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

import vaccinesdistribution.Interface.Perishable;

public class Store {
    private StoreIdentifier identifier;
    private PriorityQueue<Perishable> vaccineBatches = new PriorityQueue<>();

    @Override
    public String toString() {
        return identifier.toString();
    }

    public Store(StoreIdentifier identifier) {
        this.identifier = identifier;
    }

    public StoreIdentifier getIdentifier() {
        return new StoreIdentifier(identifier.getName(), identifier.getLocation());
    }

    public List<Perishable> dispatch(int quantity) {
        Perishable batch;
        Perishable dispatchedBatch;
        List<Perishable> dispatchedBatches = new ArrayList<>();
        
        while (quantity > 0 && !vaccineBatches.isEmpty()) {
            batch = vaccineBatches.peek();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;

        return identifier.equals(store.getIdentifier());
    }
}
