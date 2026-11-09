/*this is the generic dao interface,
that contains the basic methods for CRUD operations*/ 

import java.util.List;

public interface GenericDao<T> {
    void save(T entity);
    T findById(String id);
    boolean update(T entity);
    boolean delete(T entity);
    List<T> getAll();
}