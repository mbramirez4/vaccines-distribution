package vaccinesdistribution.Model;

import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.Locatable;
import vaccinesdistribution.Interface.PositionedObject;
import vaccinesdistribution.Util.Point;

public class Warehouse implements Locatable {
    private static final Logger logger = LogManager.getLogger(Warehouse.class);
    
    private int availableBatches;
    private PositionedObject identifier;
    private List<Perishable> expiredBatches = new ArrayList<>();
    private PriorityQueue<Perishable> vaccineBatches = new PriorityQueue<>();

    @Override
    public String toString() {
        return identifier.toString();
    }

    public Warehouse(PositionedObject identifier) {
        this.availableBatches = 0;
        this.identifier = identifier;
    }

    public int getAvailableBatches() {
        return availableBatches;
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
            availableBatches -= dispatchedBatch.getQuantity();
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

            if (batch.getQuantity() <= 0) {
                vaccineBatches.poll();
                continue;
            }

            batch.setExpired();
            availableBatches -= batch.getQuantity();
            expiredBatches.add(batch);
            vaccineBatches.poll();
            logger.info("Batch expired successfully", batch, "current date: " + currentDate);
            continue;
        }
    }

    public void registerPerishableBatch(Perishable batch) {
        if (batch instanceof VaccineBatch) {
            ((VaccineBatch) batch).sendToStore(identifier);
        }

        availableBatches += batch.getQuantity();
        vaccineBatches.add(batch);
    }

    @Override
    public Point getLocation() {
        return identifier.getLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warehouse Warehouse = (Warehouse) o;

        return identifier.equals(Warehouse.getIdentifier());
    }
}
