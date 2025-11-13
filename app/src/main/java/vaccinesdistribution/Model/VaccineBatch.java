package vaccinesdistribution.Model;

import vaccinesdistribution.Interface.Perishable;
import vaccinesdistribution.Interface.PositionedObject;

public class VaccineBatch implements Perishable, Comparable<Perishable> {
    private static int idCounter = 0;

    private int id;
    private int availableVaccines;
    private int perishDate;
    private boolean expired = false;
    private PositionedObject storeIdentifier;
    
    public VaccineBatch(int quantity, int perishDate) {
        this.id = idCounter++;
        this.availableVaccines = quantity;
        this.perishDate = perishDate;
    }

    public VaccineBatch(VaccineBatch batch, int quantity) {
        this.id = batch.getId();
        this.availableVaccines = quantity;
        this.perishDate = batch.getExpirationDate();
        this.expired = batch.isExpired();
        this.storeIdentifier = batch.getStoreIdentifier();
    }

    public void sendToStore(PositionedObject storeIdentifier) {
        this.storeIdentifier = storeIdentifier;
    }

    public PositionedObject getStoreIdentifier() {
        return storeIdentifier;
    }

    @Override
    public int getExpirationDate() {
        return perishDate;
    }

    @Override
    public int getQuantity() {
        return availableVaccines;
    }

    @Override
    public void setExpired() {
        expired = true;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public Perishable dispatch(int quantity) {
        if (quantity > availableVaccines) quantity = availableVaccines;
        availableVaccines -= quantity;
        return new VaccineBatch(this, quantity);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getStorageId() {
        return storeIdentifier.getId();
    }

    @Override
    public String toString() {
        return "VaccineBatch{" +
                "availableVaccines=" + availableVaccines +
                ", perishDate=" + perishDate +
                ", expired=" + expired +
                ", storedAt=" + storeIdentifier +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        VaccineBatch that = (VaccineBatch) o;
        return (
            Integer.valueOf(getExpirationDate()).equals(that.getExpirationDate())
            && Integer.valueOf(getQuantity()).equals(that.getQuantity())
            && Integer.valueOf(getStorageId()).equals(that.getStorageId())
            && Integer.valueOf(getId()).equals(that.getId())
        );
    }

    @Override
    public int compareTo(Perishable o) {
        int perishComparison = Integer.compare(getExpirationDate(), o.getExpirationDate());
        if (perishComparison != 0) return perishComparison;
        
        int availableVaccinesComparison = Integer.compare(getQuantity(), o.getQuantity());
        if (availableVaccinesComparison != 0) return availableVaccinesComparison;
        
        int storeComparison = Integer.compare(getStorageId(), o.getStorageId());
        if (storeComparison != 0) return storeComparison;
        
        return Integer.compare(getId(), o.getId());
    }
}
