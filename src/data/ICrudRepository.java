package data;

/** Repository for entities that may be deleted; invoices do not use this contract. */
public interface ICrudRepository<T> extends IRepository<T> {
    boolean delete(String id);
}
