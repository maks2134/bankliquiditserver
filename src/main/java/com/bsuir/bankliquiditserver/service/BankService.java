package com.bsuir.bankliquiditserver.service;

import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Bank;

import java.util.List;

public interface BankService {
    Bank createBank(String name, String registrationNumber, String address) throws ValidationException, ServiceException;
    Bank getBankById(int id) throws EntityNotFoundException, ServiceException;
    Bank getBankByName(String name) throws EntityNotFoundException, ServiceException; // Если нужен поиск по точному имени
    List<Bank> getAllBanks() throws ServiceException;
    List<Bank> searchBanks(String searchTerm) throws ServiceException; // Поиск по части имени или рег. номеру
    void updateBank(int id, String newName, String newRegistrationNumber, String newAddress) throws ValidationException, EntityNotFoundException, ServiceException;
    void deleteBank(int id) throws EntityNotFoundException, ServiceException; // Осторожно, если есть связанные данные (отчеты, анализы)
}