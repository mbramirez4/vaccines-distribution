package vaccinesdistribution.Util;

public class Point {
    private int xCoordinate;
    private int yCoordinate;

    public Point(int xCoordinate, int yCoordinate) {
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public int getXCoordinate() {
        return xCoordinate;
    }

    public int getYCoordinate() {
        return yCoordinate;
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + xCoordinate +
                ", y=" + yCoordinate +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return xCoordinate == point.getXCoordinate() && yCoordinate == point.getYCoordinate();
    }
}
