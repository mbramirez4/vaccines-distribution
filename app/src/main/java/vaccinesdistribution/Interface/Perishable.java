package vaccinesdistribution.Interface;

public interface Perishable {
    int getId();
    int getPerishDate();
    void setExpired();
    boolean isExpired();
    int getQuantity();
    int getStorageId();
    Perishable dispatch(int quantity);
}
