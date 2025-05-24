package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.RoleDao;
import com.bsuir.bankliquiditserver.dao.UserDao;
import com.bsuir.bankliquiditserver.exception.AuthenticationException;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Role;
import com.bsuir.bankliquiditserver.model.User;
import com.bsuir.bankliquiditserver.service.AuditService;
import com.bsuir.bankliquiditserver.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final RoleDao roleDao;

    // Паттерн "Фабричный метод" или простой конструктор для создания сервиса
    public UserServiceImpl(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    @Override
    public User registerUser(String username, String password, String fullName, String email, String roleName) throws ServiceException, ValidationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password cannot be empty.");
        }
        if (email == null || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { // Простая валидация email
            throw new ValidationException("Invalid email format.");
        }

        try {
            if (userDao.findByUsername(username).isPresent()) {
                throw new ValidationException("Username '" + username + "' already exists.");
            }
            if (userDao.findByEmail(email).isPresent()){
                throw new ValidationException("Email '" + email + "' already registered.");
            }

            Role role = roleDao.findByName(roleName)
                    .orElseThrow(() -> new ValidationException("Role '" + roleName + "' not found."));

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPasswordHash(hashedPassword);
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setRoleId(role.getId());
            newUser.setRole(role); // Сразу устанавливаем объект роли
            newUser.setActive(true); // По умолчанию активен
            newUser.setCreatedAt(LocalDateTime.now());

            User savedUser = userDao.save(newUser);
            LOGGER.info("User registered successfully: {}", savedUser.getUsername());
            return savedUser;
        } catch (SQLException e) {
            LOGGER.error("Error during user registration for username {}: {}", username, e.getMessage(), e);
            throw new ServiceException("Failed to register user due to a database error.", e);
        }
    }

    @Override
    public User authenticateUser(String username, String password) throws AuthenticationException, ServiceException {
        try {
            User user = userDao.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("Invalid username or password.")); // Не уточняем, чтобы не давать подсказок

            if (!user.isActive()) {
                throw new AuthenticationException("User account is inactive.");
            }

            if (BCrypt.checkpw(password, user.getPasswordHash())) {
                LOGGER.info("User authenticated successfully: {}", username);
                // Загружаем роль для пользователя, если она еще не загружена
                if (user.getRole() == null) {
                    roleDao.findById(user.getRoleId()).ifPresent(user::setRole);
                }
                return user;
            } else {
                throw new AuthenticationException("Invalid username or password.");
            }
        } catch (SQLException e) {
            LOGGER.error("Error during user authentication for username {}: {}", username, e.getMessage(), e);
            throw new ServiceException("Authentication failed due to a system error.", e);
        }
    }

    @Override
    public User getUserById(int id) throws EntityNotFoundException, ServiceException {
        try {
            User user = userDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("User", id));
            // Загрузка роли, если не загружена
            if (user.getRole() == null && user.getRoleId() > 0) {
                roleDao.findById(user.getRoleId()).ifPresent(user::setRole);
            }
            return user;
        } catch (SQLException e) {
            LOGGER.error("Error fetching user by id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to fetch user.", e);
        }
    }

    @Override
    public User getUserByUsername(String username) throws EntityNotFoundException, ServiceException {
        try {
            User user = userDao.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("User with username '" + username + "' not found."));
            if (user.getRole() == null && user.getRoleId() > 0) {
                roleDao.findById(user.getRoleId()).ifPresent(user::setRole);
            }
            return user;
        } catch (SQLException e) {
            LOGGER.error("Error fetching user by username {}: {}", username, e.getMessage(), e);
            throw new ServiceException("Failed to fetch user.", e);
        }
    }

    @Override
    public List<User> getAllUsers() throws ServiceException {
        try {
            List<User> users = userDao.findAll();
            // Для каждого пользователя загружаем роль, если это не сделано в DAO
            for(User user : users) {
                if (user.getRole() == null && user.getRoleId() > 0) {
                    roleDao.findById(user.getRoleId()).ifPresent(user::setRole);
                }
            }
            return users;
        } catch (SQLException e) {
            LOGGER.error("Error fetching all users: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch all users.", e);
        }
    }

    @Override
    public void updateUser(User userUpdates) throws ServiceException, ValidationException, EntityNotFoundException {
        if (userUpdates == null || userUpdates.getId() == 0) {
            throw new ValidationException("User data for update is invalid or ID is missing.");
        }
        try {
            User existingUser = userDao.findById(userUpdates.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User", userUpdates.getId()));

            // Проверка уникальности email, если он меняется
            if (userUpdates.getEmail() != null && !userUpdates.getEmail().equals(existingUser.getEmail())) {
                if (userDao.findByEmail(userUpdates.getEmail()).filter(u -> u.getId() != existingUser.getId()).isPresent()) {
                    throw new ValidationException("Email '" + userUpdates.getEmail() + "' is already in use by another user.");
                }
                existingUser.setEmail(userUpdates.getEmail());
            }
            // Обновляем только те поля, которые переданы и разрешены к изменению
            if (userUpdates.getFullName() != null) {
                existingUser.setFullName(userUpdates.getFullName());
            }
            // Роль и статус активности обычно меняются отдельными методами для лучшего контроля
            // existingUser.setActive(userUpdates.isActive()); // Можно, если это входит в общий update

            userDao.update(existingUser);
            LOGGER.info("User updated: {}", existingUser.getUsername());
        } catch (SQLException e) {
            LOGGER.error("Error updating user {}: {}", userUpdates.getUsername(), e.getMessage(), e);
            throw new ServiceException("Failed to update user.", e);
        }
    }

    @Override
    public void changeUserPassword(int userId, String oldPassword, String newPassword) throws AuthenticationException, ValidationException, EntityNotFoundException, ServiceException {
        if (newPassword == null || newPassword.trim().length() < 6) { // Пример минимальной длины
            throw new ValidationException("New password must be at least 6 characters long.");
        }
        try {
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));

            if (!BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
                throw new AuthenticationException("Incorrect old password.");
            }

            user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            userDao.updatePassword(user.getId(), user.getPasswordHash()); // Предполагаем, что в UserDao есть такой метод
            LOGGER.info("Password changed for user ID: {}", userId);
        } catch (SQLException e) {
            LOGGER.error("Error changing password for user ID {}: {}", userId, e.getMessage(), e);
            throw new ServiceException("Failed to change password.", e);
        }
    }

    @Override
    public void setUserActiveStatus(int userId, boolean isActive) throws EntityNotFoundException, ServiceException {
        try {
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));
            user.setActive(isActive);
            userDao.updateActiveStatus(userId, isActive); // Предполагаем, что в UserDao есть такой метод
            LOGGER.info("User ID {} active status set to: {}", userId, isActive);
        } catch (SQLException e) {
            LOGGER.error("Error setting active status for user ID {}: {}", userId, e.getMessage(), e);
            throw new ServiceException("Failed to set user active status.", e);
        }
    }

    @Override
    public void assignRoleToUser(int userId, String roleName) throws EntityNotFoundException, ServiceException, ValidationException {
        try {
            User user = userDao.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));
            Role newRole = roleDao.findByName(roleName)
                    .orElseThrow(() -> new ValidationException("Role '" + roleName + "' not found."));

            user.setRoleId(newRole.getId());
            user.setRole(newRole); // Обновляем и объект
            userDao.updateRole(userId, newRole.getId()); // Предполагаем, что в UserDao есть такой метод
            LOGGER.info("Role '{}' assigned to user ID {}", roleName, userId);
        } catch (SQLException e) {
            LOGGER.error("Error assigning role {} to user ID {}: {}", roleName, userId, e.getMessage(), e);
            throw new ServiceException("Failed to assign role to user.", e);
        }
    }
}