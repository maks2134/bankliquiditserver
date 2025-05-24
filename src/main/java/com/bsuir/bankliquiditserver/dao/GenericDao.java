package com.bsuir.bankliquiditserver.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface GenericDao<T, ID> {
    Optional<T> findById(ID id) throws SQLException;
    List<T> findAll() throws SQLException;
    T save(T entity) throws SQLException; // Может возвращать сохраненную сущность с ID
    void update(T entity) throws SQLException;
    void deleteById(ID id) throws SQLException;
}