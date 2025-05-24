package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.BankDao;
import com.bsuir.bankliquiditserver.dao.FinancialStatementDao; // Для проверки связанных отчетов перед удалением
import com.bsuir.bankliquiditserver.dao.AnalysisReportDao; // Для проверки связанных анализов перед удалением
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Bank;
import com.bsuir.bankliquiditserver.service.BankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BankServiceImpl implements BankService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankServiceImpl.class);

    private final BankDao bankDao;
    private final FinancialStatementDao financialStatementDao; // Для проверки при удалении
    // private final AnalysisReportDao analysisReportDao; // Понадобится, если будем проверять и отчеты анализа

    public BankServiceImpl(BankDao bankDao, FinancialStatementDao financialStatementDao) {
        this.bankDao = bankDao;
        this.financialStatementDao = financialStatementDao;
    }

    @Override
    public Bank createBank(String name, String registrationNumber, String address) throws ValidationException, ServiceException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Bank name cannot be empty.");
        }
        // Дополнительные валидации для registrationNumber и address по необходимости

        try {
            // Проверка на уникальность имени или регистрационного номера, если требуется бизнес-логикой
            if (bankDao.findByName(name.trim()).isPresent()) {
                throw new ValidationException("Bank with name '" + name.trim() + "' already exists.");
            }
            if (registrationNumber != null && !registrationNumber.trim().isEmpty()) {
                 Optional<Bank> existingByRegNum = bankDao.findByRegistrationNumber(registrationNumber.trim());
                 if (existingByRegNum.isPresent()) {
                     throw new ValidationException("Bank with registration number '" + registrationNumber.trim() + "' already exists.");
                 }
            }


            Bank newBank = new Bank();
            newBank.setName(name.trim());
            newBank.setRegistrationNumber(registrationNumber != null ? registrationNumber.trim() : null);
            newBank.setAddress(address != null ? address.trim() : null);

            Bank savedBank = bankDao.save(newBank);
            LOGGER.info("Bank created: {}", savedBank);
            return savedBank;
        } catch (SQLException e) {
            LOGGER.error("Error creating bank '{}': {}", name, e.getMessage(), e);
            if ("23505".equals(e.getSQLState())) { // unique_violation
                // Более точная проверка, какое именно ограничение нарушено, если есть несколько UNIQUE ключей
                if (e.getMessage().toLowerCase().contains("banks_name_key")) {
                    throw new ServiceException("Bank with name '" + name.trim() + "' already exists.", e);
                } else if (e.getMessage().toLowerCase().contains("banks_registration_number_key")) {
                    throw new ServiceException("Bank with registration number '" + registrationNumber.trim() + "' already exists.", e);
                }
            }
            throw new ServiceException("Failed to create bank due to a database error.", e);
        }
    }

    @Override
    public Bank getBankById(int id) throws EntityNotFoundException, ServiceException {
        try {
            return bankDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Bank", id));
        } catch (SQLException e) {
            LOGGER.error("Error fetching bank by id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to fetch bank.", e);
        }
    }

    @Override
    public Bank getBankByName(String name) throws EntityNotFoundException, ServiceException {
        if (name == null || name.trim().isEmpty()) {
            throw new EntityNotFoundException("Bank with empty name not found.");
        }
        try {
            return bankDao.findByName(name.trim()) // Предполагаем, что findByName есть в BankDao
                    .orElseThrow(() -> new EntityNotFoundException("Bank with name '" + name.trim() + "' not found."));
        } catch (SQLException e) {
            LOGGER.error("Error fetching bank by name '{}': {}", name, e.getMessage(), e);
            throw new ServiceException("Failed to fetch bank by name.", e);
        }
    }


    @Override
    public List<Bank> getAllBanks() throws ServiceException {
        try {
            return bankDao.findAll();
        } catch (SQLException e) {
            LOGGER.error("Error fetching all banks: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch all banks.", e);
        }
    }

    @Override
    public List<Bank> searchBanks(String searchTerm) throws ServiceException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllBanks(); // Если поиск пустой, вернуть все банки
        }
        String lowerCaseSearchTerm = searchTerm.trim().toLowerCase();
        try {
            // Это простой поиск в памяти. Для больших объемов данных лучше делать поиск на уровне БД.
            // Предположим, findAll() не слишком затратен для текущего объема данных.
            return bankDao.findAll().stream()
                    .filter(bank -> bank.getName().toLowerCase().contains(lowerCaseSearchTerm) ||
                            (bank.getRegistrationNumber() != null && bank.getRegistrationNumber().toLowerCase().contains(lowerCaseSearchTerm)))
                    .collect(Collectors.toList());
            // Если в BankDao будет метод типа findByNameContainingOrRegNumContaining, то лучше использовать его.
        } catch (SQLException e) {
            LOGGER.error("Error searching banks with term '{}': {}", searchTerm, e.getMessage(), e);
            throw new ServiceException("Failed to search banks.", e);
        }
    }

    @Override
    public void updateBank(int id, String newName, String newRegistrationNumber, String newAddress) throws ValidationException, EntityNotFoundException, ServiceException {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Bank name cannot be empty for update.");
        }

        try {
            Bank existingBank = bankDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Bank", id));

            // Проверка уникальности имени, если оно меняется
            if (!newName.trim().equals(existingBank.getName())) {
                if (bankDao.findByName(newName.trim()).isPresent()) {
                    throw new ValidationException("Bank with name '" + newName.trim() + "' already exists.");
                }
                existingBank.setName(newName.trim());
            }

            // Проверка уникальности рег. номера, если он меняется и не пуст
            String trimmedNewRegNum = (newRegistrationNumber != null) ? newRegistrationNumber.trim() : null;
            String currentRegNum = existingBank.getRegistrationNumber();

            if (trimmedNewRegNum != null && !trimmedNewRegNum.isEmpty()) {
                if (!trimmedNewRegNum.equals(currentRegNum)) {
                    // Optional<Bank> bankByNewRegNum = bankDao.findByRegistrationNumber(trimmedNewRegNum);
                    // if (bankByNewRegNum.isPresent() && bankByNewRegNum.get().getId() != id) {
                    //    throw new ValidationException("Registration number '" + trimmedNewRegNum + "' is already in use by another bank.");
                    // }
                    existingBank.setRegistrationNumber(trimmedNewRegNum);
                }
            } else { // Если новый рег. номер пустой или null, устанавливаем null
                existingBank.setRegistrationNumber(null);
            }


            existingBank.setAddress(newAddress != null ? newAddress.trim() : null);

            bankDao.update(existingBank);
            LOGGER.info("Bank updated: {}", existingBank);
        } catch (SQLException e) {
            LOGGER.error("Error updating bank with id {}: {}", id, e.getMessage(), e);
            if ("23505".equals(e.getSQLState())) { // unique_violation
                if (e.getMessage().toLowerCase().contains("banks_name_key")) {
                    throw new ServiceException("Cannot update: Bank with name '" + newName.trim() + "' already exists.", e);
                } else if (newRegistrationNumber != null && e.getMessage().toLowerCase().contains("banks_registration_number_key")) {
                    throw new ServiceException("Cannot update: Bank with registration number '" + newRegistrationNumber.trim() + "' already exists.", e);
                }
            }
            throw new ServiceException("Failed to update bank.", e);
        }
    }

    @Override
    public void deleteBank(int id) throws EntityNotFoundException, ServiceException {
        try {
            Bank bankToDelete = bankDao.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Bank", id));

            // Проверка, есть ли у банка связанные финансовые отчеты
            // Предполагаем, что в FinancialStatementDao есть метод countByBankId или findByBankId
            if (!financialStatementDao.findByBankId(id).isEmpty()) {
                throw new ServiceException("Cannot delete bank '" + bankToDelete.getName() + "' as it has associated financial statements.");
            }

            // Аналогично можно проверить AnalysisReportDao, если он будет внедрен
            // if (!analysisReportDao.findByBankId(id).isEmpty()) {
            //    throw new ServiceException("Cannot delete bank '" + bankToDelete.getName() + "' as it has associated analysis reports.");
            // }


            bankDao.deleteById(id);
            LOGGER.info("Bank deleted: {}", bankToDelete);
        } catch (SQLException e) {
            LOGGER.error("Error deleting bank with id {}: {}", id, e.getMessage(), e);
            // Если в БД настроены внешние ключи с RESTRICT, то ошибка 23503 будет выброшена
            if ("23503".equals(e.getSQLState())) { // foreign_key_violation
                throw new ServiceException("Cannot delete bank as it is referenced by other entities (e.g., financial statements, analysis reports).", e);
            }
            throw new ServiceException("Failed to delete bank.", e);
        }
    }
}