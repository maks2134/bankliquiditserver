package com.bsuir.bankliquiditserver.service.impl;

import com.bsuir.bankliquiditserver.dao.BankDao;
import com.bsuir.bankliquiditserver.dao.FinancialStatementDao;
import com.bsuir.bankliquiditserver.dao.StatementItemDao;
import com.bsuir.bankliquiditserver.dao.UserDao;
import com.bsuir.bankliquiditserver.dto.FinancialStatementDTO;
import com.bsuir.bankliquiditserver.dto.StatementItemDTO;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.Bank;
import com.bsuir.bankliquiditserver.model.FinancialStatement;
import com.bsuir.bankliquiditserver.model.StatementItem;
import com.bsuir.bankliquiditserver.model.User;
import com.bsuir.bankliquiditserver.service.FinancialStatementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FinancialStatementServiceImpl implements FinancialStatementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialStatementServiceImpl.class);

    private final FinancialStatementDao financialStatementDao;
    private final StatementItemDao statementItemDao;
    private final BankDao bankDao;
    private final UserDao userDao; // Для получения информации о пользователе

    public FinancialStatementServiceImpl(FinancialStatementDao financialStatementDao,
                                         StatementItemDao statementItemDao,
                                         BankDao bankDao,
                                         UserDao userDao) {
        this.financialStatementDao = financialStatementDao;
        this.statementItemDao = statementItemDao;
        this.bankDao = bankDao;
        this.userDao = userDao;
    }

    @Override
    public FinancialStatement createFinancialStatement(FinancialStatementDTO statementDto, User createdByUser)
            throws ValidationException, ServiceException, EntityNotFoundException {
        validateFinancialStatementDTO(statementDto, true);

        Bank bank; // Объявляем переменную здесь
        try {
            bank = bankDao.findById(statementDto.getBankId())
                    .orElseThrow(() -> new EntityNotFoundException("Bank", statementDto.getBankId()));
        } catch (SQLException e) {
            LOGGER.error("Error fetching bank with id {}: {}", statementDto.getBankId(), e.getMessage(), e);
            throw new ServiceException("Failed to retrieve bank details.", e);
        }


        FinancialStatement statement = new FinancialStatement();
        statement.setBankId(bank.getId());
        statement.setBank(bank); // Сразу ставим объект
        statement.setReportDate(statementDto.getReportDate());
        statement.setStatementType(statementDto.getStatementType());
        statement.setCurrency(statementDto.getCurrency() != null ? statementDto.getCurrency() : "BYN");
        statement.setCreatedAt(LocalDateTime.now());
        if (createdByUser != null) {
            statement.setCreatedByUserId(createdByUser.getId());
            statement.setCreatedByUser(createdByUser);
        }

        // Транзакционное сохранение отчета и его статей
        // Для простоты JDBC, используем try-with-resources для Connection и управляем транзакцией вручную
        // В более сложных сценариях или с фреймворками это делается проще
        // НО! DatabaseConnector.getConnection() каждый раз создает новое соединение,
        // для транзакции нужно одно соединение. Этот подход нужно будет пересмотреть
        // если DatabaseConnector не поддерживает передачу Connection или пул соединений с транзакциями.
        // Пока что сделаем "наивно", предполагая, что DAO методы выполнятся быстро
        // или DatabaseConnector.getConnection() будет использовать пул и автокоммит будет выключен.
        // ИДЕАЛЬНО: передавать Connection в методы DAO или иметь сервисный метод, обернутый в транзакцию.

        try {
            FinancialStatement savedStatement = financialStatementDao.save(statement);
            LOGGER.info("FinancialStatement saved with ID: {}", savedStatement.getId());

            if (statementDto.getItems() != null && !statementDto.getItems().isEmpty()) {
                List<StatementItem> itemsToSave = statementDto.getItems().stream()
                        .map(dto -> {
                            StatementItem item = new StatementItem();
                            item.setStatementId(savedStatement.getId()); // Связываем с сохраненным отчетом
                            item.setItemCode(dto.getItemCode());
                            item.setItemName(dto.getItemName());
                            item.setItemValue(dto.getItemValue());
                            item.setParentItemId(dto.getParentItemId());
                            return item;
                        }).collect(Collectors.toList());
                statementItemDao.saveAll(itemsToSave, savedStatement.getId()); // Используем пакетную вставку
                savedStatement.setItems(itemsToSave); // Добавляем сохраненные статьи в объект
                LOGGER.info("{} StatementItems saved for statement ID: {}", itemsToSave.size(), savedStatement.getId());
            }
            return savedStatement;
        } catch (SQLException e) {
            LOGGER.error("Error saving financial statement or its items for bankId {}: {}", statementDto.getBankId(), e.getMessage(), e);
            // Здесь мог бы быть откат транзакции, если бы мы ею управляли явно
            throw new ServiceException("Failed to save financial statement.", e);
        }
    }

    @Override
    public FinancialStatement getFinancialStatementById(int statementId) throws EntityNotFoundException, ServiceException {
        try {
            FinancialStatement statement = financialStatementDao.findById(statementId)
                    .orElseThrow(() -> new EntityNotFoundException("FinancialStatement", statementId));
            // Дозагрузка связанных сущностей, если они не были загружены в DAO
            if (statement.getBank() == null && statement.getBankId() > 0) {
                bankDao.findById(statement.getBankId()).ifPresent(statement::setBank);
            }
            if (statement.getCreatedByUser() == null && statement.getCreatedByUserId() != null && statement.getCreatedByUserId() > 0) {
                userDao.findById(statement.getCreatedByUserId()).ifPresent(statement::setCreatedByUser);
            }
            return statement;
        } catch (SQLException e) {
            LOGGER.error("Error fetching financial statement by id {}: {}", statementId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch financial statement.", e);
        }
    }

    @Override
    public FinancialStatement getFinancialStatementWithItemsById(int statementId) throws EntityNotFoundException, ServiceException {
        FinancialStatement statement = getFinancialStatementById(statementId); // Получаем основные данные
        try {
            List<StatementItem> items = statementItemDao.findByStatementId(statementId);
            statement.setItems(items);
            return statement;
        } catch (SQLException e) {
            LOGGER.error("Error fetching statement items for statement id {}: {}", statementId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch statement items.", e);
        }
    }

    @Override
    public List<FinancialStatement> getFinancialStatementsByBank(int bankId) throws ServiceException {
        try {
            List<FinancialStatement> statements = financialStatementDao.findByBankId(bankId);
            // Дозагрузка банка для каждого отчета, если нужно (может быть N+1)
            // Лучше, если findByBankId в DAO уже делает JOIN или сервис решает, когда нужна полная загрузка
            for (FinancialStatement fs : statements) {
                if (fs.getBank() == null) {
                    bankDao.findById(fs.getBankId()).ifPresent(fs::setBank);
                }
                if (fs.getCreatedByUser() == null && fs.getCreatedByUserId() != null && fs.getCreatedByUserId() > 0) {
                    userDao.findById(fs.getCreatedByUserId()).ifPresent(fs::setCreatedByUser);
                }
            }
            return statements;
        } catch (SQLException e) {
            LOGGER.error("Error fetching financial statements for bank id {}: {}", bankId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch financial statements.", e);
        }
    }

    @Override
    public List<FinancialStatementDTO> getFinancialStatementOverviewsByBank(int bankId) throws ServiceException {
        try {
            List<FinancialStatement> statements = financialStatementDao.findByBankId(bankId); // Получаем основные данные
            Bank bank = null; // Оптимизация: загрузить банк один раз
            if (!statements.isEmpty()) {
                bank = bankDao.findById(statements.get(0).getBankId()).orElse(null);
            }

            final Bank finalBank = bank; // Для лямбды
            return statements.stream()
                    .map(fs -> {
                        FinancialStatementDTO dto = new FinancialStatementDTO();
                        dto.setId(fs.getId());
                        dto.setBankId(fs.getBankId());
                        if (finalBank != null) dto.setBankName(finalBank.getName());
                        dto.setReportDate(fs.getReportDate());
                        dto.setStatementType(fs.getStatementType());
                        dto.setCurrency(fs.getCurrency());
                        dto.setCreatedAt(fs.getCreatedAt());
                        dto.setCreatedByUserId(fs.getCreatedByUserId());
                        if (fs.getCreatedByUserId() != null) {
                            // Загрузка имени пользователя (может быть N+1, если не кэшировать)
                            try {
                                userDao.findById(fs.getCreatedByUserId()).ifPresent(u -> dto.setCreatedByUsername(u.getUsername()));
                            } catch (SQLException e) {
                                LOGGER.warn("Could not load username for user id {}", fs.getCreatedByUserId());
                            }
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.error("Error fetching financial statement overviews for bank id {}: {}", bankId, e.getMessage(), e);
            throw new ServiceException("Failed to fetch financial statement overviews.", e);
        }
    }

    @Override
    public FinancialStatement getFinancialStatementByBankDateAndType(int bankId, LocalDate reportDate, String statementType) throws ServiceException, EntityNotFoundException {
        try {
            return financialStatementDao.findByBankIdAndReportDateAndType(bankId, reportDate, statementType)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("FinancialStatement for bankId %d, date %s, type %s not found", bankId, reportDate, statementType)));
        } catch (SQLException e) {
            LOGGER.error("Error fetching financial statement by bank, date, and type: {}", e.getMessage(), e);
            throw new ServiceException("Failed to fetch financial statement.", e);
        }
    }

    @Override
    public void updateFinancialStatementMetadata(int statementId, FinancialStatementDTO statementDto, User updatedByUser)
            throws ValidationException, EntityNotFoundException, ServiceException {
        validateFinancialStatementDTO(statementDto, false); // false - id должен быть

        FinancialStatement existingStatement;
        Bank bankForUpdate; // Новая переменная для банка, который будет в обновляемом отчете

        try {
            existingStatement = financialStatementDao.findById(statementId)
                    .orElseThrow(() -> new EntityNotFoundException("FinancialStatement", statementId));

            // Получаем банк, который указан в DTO для обновления
            bankForUpdate = bankDao.findById(statementDto.getBankId())
                    .orElseThrow(() -> new EntityNotFoundException("Bank specified in DTO for update not found, ID: " + statementDto.getBankId()));

            // Проверка, что не пытаемся изменить на комбинацию (банк, дата, тип), которая уже существует для ДРУГОГО отчета
            // Эта проверка нужна, если значения bankId, reportDate или statementType изменились
            if (existingStatement.getBankId() != statementDto.getBankId() ||
                    !existingStatement.getReportDate().equals(statementDto.getReportDate()) ||
                    !existingStatement.getStatementType().equals(statementDto.getStatementType()))
            {
                Optional<FinancialStatement> conflictingStatement = financialStatementDao.findByBankIdAndReportDateAndType(
                        statementDto.getBankId(), statementDto.getReportDate(), statementDto.getStatementType());

                if (conflictingStatement.isPresent() && conflictingStatement.get().getId() != statementId) {
                    // Используем имя банка, который мы уже загрузили (bankForUpdate)
                    throw new ValidationException(String.format("Another financial statement for bank '%s' on date '%s' of type '%s' already exists.",
                            bankForUpdate.getName(), statementDto.getReportDate(), statementDto.getStatementType()));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Error during pre-update checks for financial statement ID {}: {}", statementId, e.getMessage(), e);
            throw new ServiceException("Failed during pre-update checks for financial statement.", e);
        }

        // Теперь обновляем существующий отчет данными из DTO, используя загруженный bankForUpdate
        existingStatement.setBankId(bankForUpdate.getId());
        existingStatement.setBank(bankForUpdate); // Присваиваем объект Bank
        existingStatement.setReportDate(statementDto.getReportDate());
        existingStatement.setStatementType(statementDto.getStatementType());
        existingStatement.setCurrency(statementDto.getCurrency());
        // createdBy и createdAt не меняем при обновлении метаданных,
        // но можно добавить поле lastUpdatedAt и lastUpdatedByUserId (если нужно отслеживать, кто обновил)
        // if (updatedByUser != null) {
        //     existingStatement.setLastUpdatedByUserId(updatedByUser.getId());
        // }
        // existingStatement.setLastUpdatedAt(LocalDateTime.now());

        try {
            financialStatementDao.update(existingStatement);
            LOGGER.info("FinancialStatement metadata updated for ID: {}", statementId);
        } catch (SQLException e) {
            LOGGER.error("Error updating financial statement metadata for ID {}: {}", statementId, e.getMessage(), e);
            // Обработка уникальных ключей, если update может их нарушить (например, если уникальный ключ включает другие поля, не только id)
            if ("23505".equals(e.getSQLState())) {
                throw new ServiceException("Update failed due to a unique constraint violation. Please check the data.", e);
            }
            throw new ServiceException("Failed to update financial statement metadata.", e);
        }
    }


    @Override
    public void deleteFinancialStatement(int statementId) throws EntityNotFoundException, ServiceException {
        try {
            // Сначала проверяем, существует ли такой отчет
            if (!financialStatementDao.findById(statementId).isPresent()) {
                throw new EntityNotFoundException("FinancialStatement", statementId);
            }

            // Статьи отчета должны удаляться каскадно (ON DELETE CASCADE в БД)
            // Либо удаляем их явно здесь перед удалением самого отчета:
            // statementItemDao.deleteByStatementId(statementId); // Если нет ON DELETE CASCADE

            financialStatementDao.deleteById(statementId);
            LOGGER.info("FinancialStatement deleted with ID: {}", statementId);
        } catch (SQLException e) {
            LOGGER.error("Error deleting financial statement with ID {}: {}", statementId, e.getMessage(), e);
            if ("23503".equals(e.getSQLState())) { // foreign_key_violation (например, AnalysisReport ссылается на него)
                throw new ServiceException("Cannot delete financial statement as it is referenced by other entities (e.g., analysis reports).", e);
            }
            throw new ServiceException("Failed to delete financial statement.", e);
        }
    }

    private void validateFinancialStatementDTO(FinancialStatementDTO dto, boolean isNew) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("Financial statement data cannot be null.");
        }
        if (!isNew && dto.getId() == null) {
            throw new ValidationException("Financial statement ID is required for update.");
        }
        if (dto.getBankId() <= 0) {
            throw new ValidationException("Valid Bank ID is required.");
        }
        if (dto.getReportDate() == null) {
            throw new ValidationException("Report date is required.");
        }
        if (dto.getStatementType() == null || dto.getStatementType().trim().isEmpty()) {
            throw new ValidationException("Statement type is required.");
        }
        if (dto.getItems() != null) {
            for (StatementItemDTO itemDto : dto.getItems()) {
                if (itemDto.getItemName() == null || itemDto.getItemName().trim().isEmpty()) {
                    throw new ValidationException("Statement item name cannot be empty.");
                }
                if (itemDto.getItemValue() == null) {
                    throw new ValidationException("Statement item value for '" + itemDto.getItemName() + "' cannot be null.");
                }
                // Дополнительные валидации для itemCode, если он обязателен или имеет формат
            }
        }
    }
}