package com.bsuir.bankliquiditserver.exception;

public class EntityNotFoundException extends ServiceException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s with id %s not found", entityName, id));
    }
}