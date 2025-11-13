package vaccinesdistribution.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import vaccinesdistribution.Interface.Locatable;
import vaccinesdistribution.Interface.SpatialCollection;

public class ArraySpatialCollection<T extends Locatable> implements SpatialCollection<T> {
    private List<T> items;
    
    public ArraySpatialCollection() {
        this.items = new ArrayList<>();
    }
    
    @Override
    public int size() {
        return items.size();
    }
    
    @Override
    public void add(T e) {
        items.add(e);
    }
    
    @Override
    public void remove(Object o) {
        items.remove(o);
    }
    
    @Override
    public List<T> getItems() {
        return new ArrayList<>(items);
    }
    
    @Override
    public List<T> getKClosestItems(Point p, int k) {
        // If k is less than or equal to 0, return an empty list
        if (k <= 0) return new ArrayList<>();
        // If k is greater than or equal to the number of items, return all items
        if (k >= items.size()) return getItems();

        // To sort the items by distance, a max-heap is used. The size
        // of the heap is always limited to k. At any time, the farthest
        // item (from the current collection) is at the top of the heap.
        // A new item is added to the heap if it is closer than the
        // farthest. In that case, the farthest item is removed and the
        // new item is added.
        
        PriorityQueue<ItemWithDistance<T>> maxHeap = new PriorityQueue<>((a, b) -> {
            return Double.compare(b.distanceToP(), a.distanceToP()); // Reverse order for max-heap
        });
        
        double itemDist;
        double maxDist;
        ItemWithDistance<T> itemWithDistance;
        for (T item : items) {
            itemDist = p.calculateDistance(item.getLocation());
            itemWithDistance = new ItemWithDistance<>(item, itemDist);
            
            if (maxHeap.size() < k) {
                maxHeap.offer(itemWithDistance);
                continue;
            }
            
            maxDist = maxHeap.peek().distanceToP();
            if (itemDist < maxDist) {
                maxHeap.poll();
                maxHeap.offer(itemWithDistance);    
            }
        }
        
        List<T> result = new ArrayList<>();
        for (ItemWithDistance<T> itemWithDist : maxHeap) {
            result.add(itemWithDist.item());
        }
        return result;
    }

    @Override
    public void setItemsFromList(List<T> items) {
        this.items = new ArrayList<>(items);
    }

    private record ItemWithDistance<T extends Locatable>(T item, double distanceToP) {}
}
