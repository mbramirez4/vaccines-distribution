package vaccinesdistribution.Interface;

import vaccinesdistribution.Util.Point;

public interface PositionedObject {
    int getId();
    String getName();
    Point getLocation();
}
