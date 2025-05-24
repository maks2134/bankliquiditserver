package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.RoleDao;
import com.bsuir.bankliquiditserver.dao.UserDao; // Может понадобиться для проверки использования роли
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Role;
import com.bsuir.bankliquiditserver.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RoleServiceImpl implements RoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleDao roleDao;
    private final UserDao userDao; // Для проверки, используется ли роль

    public RoleServiceImpl(RoleDao roleDao, UserDao userDao) {
        this.roleDao = roleDao;
        this.userDao = userDao;
    }

    @Override
    public Role createRole(String roleName) throws ValidationException, ServiceException {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new ValidationException("Role name cannot be empty.");
        }
        String normalizedRoleName = roleName.trim().toUpperCase(); // Роли обычно в верхнем регистре

        try {
            if (roleDao.findByName(normalizedRoleName).isPresent()) {
                throw new ValidationException("Role '" + normalizedRoleName + "' already exists.");
            }
            Role newRole = new Role();
            newRole.setRoleName(normalizedRoleName);
            Role savedRole = roleDao.save(newRole);
            LOGGER.info("Role created: {}", savedRole);
            return savedRole;
        } catch (SQLException e) {
            LOGGER.error("Error creating role '{}': {}", normalizedRoleName, e.getMessage(), e);
            throw new ServiceException("Failed to create role due to a database error.", e);
        }
    }

    @Override
    public Role getRoleById(int id) throws EntityNotFoundException, ServiceException {
        try {
            return roleDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Role", id));
        } catch (SQLException e) {
            LOGGER.error("Error fetching role by id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to fetch role.", e);
        }
    }

    @Override
    public Role getRoleByName(String roleName) throws EntityNotFoundException, ServiceException {
        if (roleName == null || roleName.trim().isEmpty()) {
            // Можно бросить ValidationException или сразу EntityNotFound, т.к. пустая строка не будет найдена
            throw new EntityNotFoundException("Role with empty name not found.");
        }
        String normalizedRoleName = roleName.trim().toUpperCase();
        try {
            return roleDao.findByName(normalizedRoleName)
                    .orElseThrow(() -> new EntityNotFoundException("Role with name '" + normalizedRoleName + "' not found."));
        } catch (SQLException e) {
            LOGGER.error("Error fetching role by name '{}': {}", normalizedRoleName, e.getMessage(), e);
            throw new ServiceException("Failed to fetch role.", e);
        }
    }

    @Override
    public List<Role> getAllRoles() throws ServiceException {
        try {
            return roleDao.findAll();
        } catch (SQLException e) {
            LOGGER.error("Error fetching all roles: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch all roles.", e);
        }
    }

    @Override
    public void updateRole(int id, String newRoleName) throws ValidationException, EntityNotFoundException, ServiceException {
        if (newRoleName == null || newRoleName.trim().isEmpty()) {
            throw new ValidationException("New role name cannot be empty.");
        }
        String normalizedNewRoleName = newRoleName.trim().toUpperCase();

        try {
            Role existingRole = roleDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Role", id));

            // Проверка, не пытается ли пользователь переименовать роль в имя, которое уже существует (и не является текущей ролью)
            Optional<Role> roleWithNewName = roleDao.findByName(normalizedNewRoleName);
            if (roleWithNewName.isPresent() && roleWithNewName.get().getId() != id) {
                throw new ValidationException("Role name '" + normalizedNewRoleName + "' is already in use.");
            }

            existingRole.setRoleName(normalizedNewRoleName);
            roleDao.update(existingRole);
            LOGGER.info("Role updated: {}", existingRole);
        } catch (SQLException e) {
            LOGGER.error("Error updating role with id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to update role.", e);
        }
    }

    @Override
    public void deleteRole(int id) throws EntityNotFoundException, ServiceException {
        try {
            Role roleToDelete = roleDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Role", id));

            // Проверка, используется ли роль пользователями
            // Это требует метода в UserDao, например, countUsersByRoleId(int roleId) или findUsersByRoleId(int roleId)
            // Для простоты, предположим, у UserDao есть метод `hasUsersWithRole(int roleId)`
            // Если его нет, эту проверку нужно будет добавить/реализовать.
            if (userDao.countUsersByRoleId(id) > 0) { // Пример, такого метода пока нет в UserDao
                 throw new ServiceException("Cannot delete role '" + roleToDelete.getRoleName() + "' as it is currently assigned to users.");
            }
            // Пока что, без этой проверки:
            if (isRoleProtected(roleToDelete.getRoleName())) {
                throw new ServiceException("Cannot delete protected role: " + roleToDelete.getRoleName());
            }


            roleDao.deleteById(id);
            LOGGER.info("Role deleted: {}", roleToDelete);
        } catch (SQLException e) {
            LOGGER.error("Error deleting role with id {}: {}", id, e.getMessage(), e);
            // Дополнительно можно проверить SQLState на ошибки нарушения внешних ключей, если проверка выше не сделана
            if ("23503".equals(e.getSQLState())) { // foreign_key_violation
                throw new ServiceException("Cannot delete role as it is referenced by other entities (e.g., users).", e);
            }
            throw new ServiceException("Failed to delete role.", e);
        }
    }

    // Вспомогательный метод для защиты системных ролей от удаления
    private boolean isRoleProtected(String roleName) {
        // Например, не даем удалять базовые роли ADMIN, ANALYST, GUEST
        return "ADMIN".equals(roleName) || "ANALYST".equals(roleName) || "GUEST".equals(roleName);
    }
}