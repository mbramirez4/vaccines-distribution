package vaccinesdistribution.Interface;

import java.util.List;

import vaccinesdistribution.Util.Point;

public interface SpatialCollection<E> {
    /**
     * Returns the number of elements in this collection
     * @return the number of elements in this collection
     */
    int size();

    /**
     * Add an item to the collection
     * @param e The item to be added
     */
    void add(E e);

    /**
     * Removes the first occurrence of the specified element from this collection,
     * if it is present (optional operation).  If this collection does not contain
     * the element, it is unchanged. Returns {@code true} if this collection
     * contained the specified element (or equivalently, if this collection changed
     * as a result of the call).
     * @param o element to be removed from this collection, if present
     * @return {@code true} if this collection contained the specified element
     */
    void remove(Object o);

    List<E> getItems();

    List<E> getKClosestItems(Point p, int k);

    SpatialCollection<E> fromList(List<E> items);
}
