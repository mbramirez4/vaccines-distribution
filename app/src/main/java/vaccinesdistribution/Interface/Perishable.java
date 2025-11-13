package vaccinesdistribution.Interface;

public interface Perishable {
    int getId();
    int getExpirationDate();
    void setExpired();
    boolean isExpired();
    int getQuantity();
    int getStorageId();
    Perishable dispatch(int quantity);
}
