package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Role;

import java.util.List;

public interface RoleService {
    Role createRole(String roleName) throws ValidationException, ServiceException;
    Role getRoleById(int id) throws EntityNotFoundException, ServiceException;
    Role getRoleByName(String roleName) throws EntityNotFoundException, ServiceException;
    List<Role> getAllRoles() throws ServiceException;
    void updateRole(int id, String newRoleName) throws ValidationException, EntityNotFoundException, ServiceException;
    void deleteRole(int id) throws EntityNotFoundException, ServiceException; // Осторожно с удалением ролей, если они используются!
}