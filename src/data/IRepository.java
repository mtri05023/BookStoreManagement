package data;

import java.util.List;

/** add rejects null/duplicate IDs before changing state; findById returns null when absent.
 * getAll returns a copy of the collection, not deep copies of its elements.
 */
public interface IRepository<T> {
    void add(T entity);
    List<T> getAll();
    T findById(String id);
}
