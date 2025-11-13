package vaccinesdistribution.Model;

import java.util.Objects;

import vaccinesdistribution.Util.Point;
import vaccinesdistribution.Interface.PositionedObject;

public class WarehouseIdentifier implements PositionedObject {
    private static int idCounter = 0;
    private int id;
    private String name;
    private Point location;
    

    public WarehouseIdentifier(String name, Point location) {
        this.id = idCounter++;
        this.name = name;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Point getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Store{" +
                "name='" + name + "'" +
                ", location=" + location +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseIdentifier that = (WarehouseIdentifier) o;

        return id == that.getId() && name.equals(that.getName()) && location.equals(that.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, location);
    }
}
