package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.exception.AuthenticationException;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.User;

import java.util.List;

public interface UserService {
    User registerUser(String username, String password, String fullName, String email, String roleName) throws ServiceException, ValidationException;
    User authenticateUser(String username, String password) throws AuthenticationException, ServiceException;
    User getUserById(int id) throws EntityNotFoundException, ServiceException;
    User getUserByUsername(String username) throws EntityNotFoundException, ServiceException;
    List<User> getAllUsers() throws ServiceException;
    void updateUser(User user) throws ServiceException, ValidationException, EntityNotFoundException;
    void changeUserPassword(int userId, String oldPassword, String newPassword) throws AuthenticationException, ValidationException, EntityNotFoundException, ServiceException;
    void setUserActiveStatus(int userId, boolean isActive) throws EntityNotFoundException, ServiceException;
    void assignRoleToUser(int userId, String roleName) throws EntityNotFoundException, ServiceException, ValidationException;
}