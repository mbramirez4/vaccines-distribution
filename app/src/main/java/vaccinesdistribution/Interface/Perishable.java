package vaccinesdistribution.Interface;

public interface Perishable {
    int getId();
    int getExpirationDate();
    void setExpired();
    boolean isExpired();
    int getQuantity();
    int getStorageId();
    PositionedObject getStoreIdentifier();
    Perishable dispatch(int quantity);
}
