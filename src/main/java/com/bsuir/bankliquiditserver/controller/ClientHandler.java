package com.bsuir.bankliquiditserver.controller;

import com.bsuir.bankliquiditserver.dto.*;
import com.bsuir.bankliquiditserver.exception.AuthenticationException;
import com.bsuir.bankliquiditserver.exception.EntityNotFoundException;
import com.bsuir.bankliquiditserver.exception.ServiceException;
import com.bsuir.bankliquiditserver.exception.ValidationException;
import com.bsuir.bankliquiditserver.model.*;
import com.bsuir.bankliquiditserver.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID; // Для простого примера токена
import java.util.concurrent.ConcurrentHashMap; // Для хранения токенов (упрощенный вариант)
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private static final Map<String, User> activeUserSessions = new ConcurrentHashMap<>(); // token -> User

    private final Socket clientSocket;
    private final ObjectMapper objectMapper;

    private final UserService userService;
    private final RoleService roleService;
    private final BankService bankService;
    private final FinancialStatementService financialStatementService;
    private final AnalysisService analysisService;
    private final AuditService auditService;

    private User currentUser; // Аутентифицированный пользователь для текущего ClientHandler
    private String clientIpAddress;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules(); // На всякий случай для других модулей дат/времени

        // Инициализация сервисов через ServiceFactory
        this.userService = ServiceFactory.getUserService();
        this.roleService = ServiceFactory.getRoleService();
        this.bankService = ServiceFactory.getBankService();
        this.financialStatementService = ServiceFactory.getFinancialStatementService();
        this.analysisService = ServiceFactory.getAnalysisService();
        this.auditService = ServiceFactory.getAuditService();

        this.clientIpAddress = clientSocket.getInetAddress().getHostAddress();
        LOGGER.info("Client connected: {} ({})", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
    }

    @Override
    public void run() {
        try (
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                PrintWriter writer = new PrintWriter(outputStream, true)
        ) {
            String clientMessageJson;
            while ((clientMessageJson = reader.readLine()) != null) {
                LOGGER.debug("Received from [{}]: {}", clientIpAddress, clientMessageJson);
                ServerResponseDTO response;
                ClientRequestDTO request = null;
                try {
                    request = objectMapper.readValue(clientMessageJson, ClientRequestDTO.class);
                    response = processRequest(request);
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error parsing JSON request from [{}]: {}", clientIpAddress, clientMessageJson, e);
                    response = ServerResponseDTO.error("Invalid JSON request format.");
                    logFailedRequest(request, "PARSE_ERROR", e.getMessage());
                } catch (Exception e) {
                    LOGGER.error("Unexpected error processing request from [{}]: {}", clientIpAddress, clientMessageJson, e);
                    response = ServerResponseDTO.error("Internal server error: " + e.getMessage());
                    logFailedRequest(request, "INTERNAL_ERROR", e.getMessage());
                }

                String serverResponseJson = objectMapper.writeValueAsString(response);
                writer.println(serverResponseJson);
                LOGGER.debug("Sent to [{}]: {}", clientIpAddress, serverResponseJson);
            }
        } catch (SocketException e) {
            if ("Connection reset".equalsIgnoreCase(e.getMessage()) || "Socket closed".equalsIgnoreCase(e.getMessage()) || "Broken pipe".equalsIgnoreCase(e.getMessage())) {
                LOGGER.info("Client {} disconnected.", clientIpAddress);
            } else {
                LOGGER.error("SocketException with client {}: {}", clientIpAddress, e.getMessage());
            }
        } catch (EOFException e) {
            LOGGER.info("Client {} closed connection (EOF).", clientIpAddress);
        }
        catch (IOException e) {
            LOGGER.error("IOException with client {}: {}", clientIpAddress, e.getMessage());
        } finally {
            // Завершение сессии пользователя при отключении
            if (this.currentUser != null) {
                activeUserSessions.values().remove(this.currentUser); // Удаляем по значению, если токен неизвестен
                LOGGER.info("User session ended for: {}", this.currentUser.getUsername());
            }
            closeClientSocket();
            LOGGER.info("Client handler for {} finished.", clientIpAddress);
        }
    }

    private ServerResponseDTO processRequest(ClientRequestDTO request) {
        if (request.getAction() == null) {
            logFailedRequest(request, "BAD_REQUEST", "Action not specified");
            return ServerResponseDTO.badRequest("Action not specified in request.");
        }

        String action = request.getAction().toUpperCase();

        // Проверка аутентификации для защищенных эндпоинтов
        if (!isPublicAction(action)) {
            if (request.getToken() == null) {
                logFailedRequest(request, "UNAUTHORIZED", "Token missing");
                return ServerResponseDTO.unauthorized("Authentication token is missing.");
            }
            this.currentUser = activeUserSessions.get(request.getToken());
            if (this.currentUser == null) {
                logFailedRequest(request, "UNAUTHORIZED", "Invalid or expired token");
                return ServerResponseDTO.unauthorized("Invalid or expired token.");
            }
            // Обновляем роль пользователя, если она не загружена
            if (this.currentUser.getRole() == null && this.currentUser.getRoleId() > 0) {
                try {
                    roleService.getRoleById(this.currentUser.getRoleId()).ifPresent(this.currentUser::setRole);
                } catch (Exception e) {
                    LOGGER.warn("Could not load role for current user {}: {}", this.currentUser.getUsername(), e.getMessage());
                }
            }
        }


        try {
            // Используем switch-expressions (Java 14+) для большей читаемости
            return switch (action) {
                case "LOGIN" -> handleLogin(request);
                case "REGISTER" -> handleRegister(request);
                case "LOGOUT" -> handleLogout(request);

                // User operations (protected)
                case "GET_USER_PROFILE" -> requireAuth(() -> handleGetUserProfile(request));
                case "CHANGE_PASSWORD" -> requireAuth(() -> handleChangePassword(request));
                // Admin user operations
                case "GET_ALL_USERS" -> requireRole("ADMIN", () -> handleGetAllUsers(request));
                case "UPDATE_USER_STATUS" -> requireRole("ADMIN", () -> handleUpdateUserStatus(request));
                case "ASSIGN_USER_ROLE" -> requireRole("ADMIN", () -> handleAssignUserRole(request));

                // Role operations (admin)
                case "GET_ALL_ROLES" -> requireRole("ADMIN", () -> handleGetAllRoles(request));
                case "CREATE_ROLE" -> requireRole("ADMIN", () -> handleCreateRole(request));
                // ... другие CRUD для ролей

                // Bank operations
                case "CREATE_BANK" -> requireRole(new String[]{"ADMIN", "ANALYST"}, () -> handleCreateBank(request));
                case "GET_BANK_BY_ID" -> requireAuth(() -> handleGetBankById(request));
                case "GET_ALL_BANKS" -> requireAuth(() -> handleGetAllBanks(request));
                case "UPDATE_BANK" -> requireRole(new String[]{"ADMIN", "ANALYST"}, () -> handleUpdateBank(request));
                case "DELETE_BANK" -> requireRole("ADMIN", () -> handleDeleteBank(request));


                // Financial Statement operations
                case "CREATE_FINANCIAL_STATEMENT" -> requireRole(new String[]{"ADMIN", "ANALYST"}, () -> handleCreateFinancialStatement(request));
                case "GET_FINANCIAL_STATEMENT" -> requireAuth(() -> handleGetFinancialStatement(request)); // с деталями
                case "GET_BANK_FINANCIAL_STATEMENTS" -> requireAuth(() -> handleGetBankFinancialStatements(request)); // обзоры
                case "DELETE_FINANCIAL_STATEMENT" -> requireRole(new String[]{"ADMIN", "ANALYST"}, () -> handleDeleteFinancialStatement(request));

                // Analysis operations
                case "CALCULATE_LIQUIDITY" -> requireRole("ANALYST", () -> handleCalculateLiquidity(request));
                case "CALCULATE_SOLVENCY" -> requireRole("ANALYST", () -> handleCalculateSolvency(request));
                case "SAVE_LIQUIDITY_REPORT" -> requireRole("ANALYST", () -> handleSaveLiquidityReport(request));
                case "SAVE_SOLVENCY_REPORT" -> requireRole("ANALYST", () -> handleSaveSolvencyReport(request));
                case "GET_ANALYSIS_REPORT" -> requireAuth(() -> handleGetAnalysisReport(request));
                case "GET_BANK_ANALYSIS_REPORTS" -> requireAuth(() -> handleGetBankAnalysisReports(request));
                case "DELETE_ANALYSIS_REPORT" -> requireRole("ADMIN", () -> handleDeleteAnalysisReport(request));


                // Audit operations (admin)
                case "GET_ALL_AUDIT_LOGS" -> requireRole("ADMIN", () -> handleGetAllAuditLogs(request));
                case "GET_USER_AUDIT_LOGS" -> requireRole("ADMIN", () -> handleGetUserAuditLogs(request));


                default -> {
                    LOGGER.warn("Unknown action '{}' from client {}", request.getAction(), clientIpAddress);
                    auditService.logAction(this.currentUser != null ? this.currentUser.getId() : null, "UNKNOWN_ACTION", "Action: " + request.getAction(), clientIpAddress, false, false);
                    yield ServerResponseDTO.error("Unknown action: " + request.getAction());
                }
            };
        } catch (AuthenticationException e) {
            logFailedRequest(request, "AUTH_ERROR", e.getMessage());
            return ServerResponseDTO.unauthorized(e.getMessage());
        } catch (ValidationException e) {
            logFailedRequest(request, "VALIDATION_ERROR", e.getMessage());
            return ServerResponseDTO.badRequest(e.getMessage());
        } catch (EntityNotFoundException e) {
            logFailedRequest(request, "NOT_FOUND_ERROR", e.getMessage());
            return ServerResponseDTO.notFound(e.getMessage());
        } catch (ServiceException e) { // Общая ошибка сервиса
            logFailedRequest(request, "SERVICE_ERROR", e.getMessage());
            return ServerResponseDTO.error("Service error: " + e.getMessage());
        } catch (Exception e) { // Непредвиденные ошибки
            LOGGER.error("Critical error processing action '{}' for client {}: {}",
                    request != null ? request.getAction() : "UNKNOWN", clientIpAddress, e.getMessage(), e);
            logFailedRequest(request, "CRITICAL_ERROR", e.getMessage());
            return ServerResponseDTO.error("Critical internal server error. Please contact support.");
        }
    }

    private boolean isPublicAction(String action) {
        return "LOGIN".equals(action) || "REGISTER".equals(action);
    }

    // --- Утилиты для проверки прав ---
    @FunctionalInterface
    private interface ProtectedAction {
        ServerResponseDTO execute() throws ServiceException, ValidationException, EntityNotFoundException, AuthenticationException;
    }

    private ServerResponseDTO requireAuth(ProtectedAction action) throws ServiceException, ValidationException, EntityNotFoundException, AuthenticationException {
        if (this.currentUser == null) {
            auditService.logAction(null, "AUTH_REQUIRED_ATTEMPT", "Attempt to access protected resource without auth", clientIpAddress, false, false);
            return ServerResponseDTO.unauthorized("Authentication required for this action.");
        }
        return action.execute();
    }

    private ServerResponseDTO requireRole(String requiredRole, ProtectedAction action) throws ServiceException, ValidationException, EntityNotFoundException, AuthenticationException {
        return requireAuth(() -> {
            if (this.currentUser.getRole() == null || !requiredRole.equalsIgnoreCase(this.currentUser.getRole().getRoleName())) {
                auditService.logUserAction(this.currentUser, "FORBIDDEN_ACTION_ATTEMPT", "Required role: " + requiredRole, clientIpAddress, false, false);
                return ServerResponseDTO.forbidden("You do not have the required role (" + requiredRole + ") for this action.");
            }
            return action.execute();
        });
    }

    private ServerResponseDTO requireRole(String[] requiredRoles, ProtectedAction action) throws ServiceException, ValidationException, EntityNotFoundException, AuthenticationException {
        return requireAuth(() -> {
            if (this.currentUser.getRole() == null) {
                auditService.logUserAction(this.currentUser, "FORBIDDEN_ACTION_ATTEMPT", "Role not loaded", clientIpAddress, false, false);
                return ServerResponseDTO.forbidden("User role not determined.");
            }
            String currentUserRole = this.currentUser.getRole().getRoleName();
            boolean roleMatch = false;
            for (String role : requiredRoles) {
                if (role.equalsIgnoreCase(currentUserRole)) {
                    roleMatch = true;
                    break;
                }
            }
            if (!roleMatch) {
                auditService.logUserAction(this.currentUser, "FORBIDDEN_ACTION_ATTEMPT", "Required one of roles: " + String.join(",", requiredRoles), clientIpAddress, false, false);
                return ServerResponseDTO.forbidden("You do not have one of the required roles for this action.");
            }
            return action.execute();
        });
    }


    // --- Обработчики команд ---

    private ServerResponseDTO handleLogin(ClientRequestDTO request) throws ServiceException, AuthenticationException, ValidationException {
        Map<String, String> credentials = parsePayload(request.getPayload(), new TypeReference<Map<String, String>>() {});
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            throw new ValidationException("Username and password are required.");
        }

        User user = userService.authenticateUser(username, password);
        String token = UUID.randomUUID().toString();
        activeUserSessions.put(token, user);
        this.currentUser = user; // Устанавливаем текущего пользователя для обработчика

        auditService.logUserAction(user, "LOGIN", "Successful login", clientIpAddress, true, false);
        return ServerResponseDTO.success(new LoginResponseDTO(token, UserDTO.fromUser(user)));
    }

    private ServerResponseDTO handleRegister(ClientRequestDTO request) throws ServiceException, ValidationException {
        // Ожидаем payload: {"username": "u", "password": "p", "fullName": "fn", "email": "e", "roleName": "ANALYST"}
        // Для простоты пока GUEST или ANALYST, админа должен создавать админ
        Map<String, String> regData = parsePayload(request.getPayload(), new TypeReference<Map<String, String>>() {});
        User newUser = userService.registerUser(
                regData.get("username"),
                regData.get("password"),
                regData.get("fullName"),
                regData.get("email"),
                regData.getOrDefault("roleName", "GUEST") // По умолчанию GUEST
        );
        auditService.logAction(newUser.getId(), "REGISTER", "User registered: " + newUser.getUsername(), clientIpAddress, true, false);
        return ServerResponseDTO.success("User registered successfully: " + newUser.getUsername());
    }

    private ServerResponseDTO handleLogout(ClientRequestDTO request) throws ServiceException {
        if (request.getToken() != null) {
            User user = activeUserSessions.remove(request.getToken());
            if (user != null) {
                auditService.logUserAction(user, "LOGOUT", "User logged out", clientIpAddress, true, false);
                this.currentUser = null; // Сбрасываем текущего пользователя
                return ServerResponseDTO.success("Logout successful.");
            }
        }
        return ServerResponseDTO.error("No active session found for the token or token not provided.");
    }


    // --- User Operations ---
    private ServerResponseDTO handleGetUserProfile(ClientRequestDTO request) {
        return ServerResponseDTO.success(UserDTO.fromUser(this.currentUser));
    }

    private ServerResponseDTO handleChangePassword(ClientRequestDTO request) throws ServiceException, AuthenticationException, ValidationException, EntityNotFoundException {
        Map<String, String> passwords = parsePayload(request.getPayload(), new TypeReference<Map<String, String>>() {});
        userService.changeUserPassword(this.currentUser.getId(), passwords.get("oldPassword"), passwords.get("newPassword"));
        auditService.logUserAction(this.currentUser, "CHANGE_PASSWORD", "Password changed", clientIpAddress, true, true);
        return ServerResponseDTO.success("Password changed successfully.");
    }

    // --- Admin User Operations ---
    private ServerResponseDTO handleGetAllUsers(ClientRequestDTO request) throws ServiceException {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream().map(UserDTO::fromUser).collect(Collectors.toList());
        auditService.logUserAction(this.currentUser, "GET_ALL_USERS", "Retrieved list of all users", clientIpAddress, true, false);
        return ServerResponseDTO.success(userDTOs);
    }

    private ServerResponseDTO handleUpdateUserStatus(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer userId = (Integer) payload.get("userId");
        Boolean isActive = (Boolean) payload.get("isActive");
        if (userId == null || isActive == null) throw new ValidationException("userId and isActive are required.");

        userService.setUserActiveStatus(userId, isActive);
        auditService.logUserAction(this.currentUser, "UPDATE_USER_STATUS", "User ID " + userId + " status set to " + isActive, clientIpAddress, true, true);
        return ServerResponseDTO.success("User status updated.");
    }

    private ServerResponseDTO handleAssignUserRole(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer userId = (Integer) payload.get("userId");
        String roleName = (String) payload.get("roleName");
        if (userId == null || roleName == null) throw new ValidationException("userId and roleName are required.");

        userService.assignRoleToUser(userId, roleName);
        auditService.logUserAction(this.currentUser, "ASSIGN_USER_ROLE", "User ID " + userId + " role set to " + roleName, clientIpAddress, true, true);
        return ServerResponseDTO.success("User role updated.");
    }


    // --- Role Operations ---
    private ServerResponseDTO handleGetAllRoles(ClientRequestDTO request) throws ServiceException {
        List<Role> roles = roleService.getAllRoles();
        auditService.logUserAction(this.currentUser, "GET_ALL_ROLES", "Retrieved list of all roles", clientIpAddress, true, false);
        return ServerResponseDTO.success(roles); // Role это простой объект, можно передавать как есть
    }

    private ServerResponseDTO handleCreateRole(ClientRequestDTO request) throws ServiceException, ValidationException {
        Map<String, String> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, String>>() {});
        String roleName = payload.get("roleName");
        if (roleName == null) throw new ValidationException("roleName is required.");
        Role newRole = roleService.createRole(roleName);
        auditService.logUserAction(this.currentUser, "CREATE_ROLE", "Role created: " + newRole.getRoleName(), clientIpAddress, true, true);
        return ServerResponseDTO.success(newRole);
    }


    // --- Bank Operations ---
    private ServerResponseDTO handleCreateBank(ClientRequestDTO request) throws ServiceException, ValidationException {
        // Payload: {"name": "BankName", "registrationNumber": "123", "address": "Addr"}
        Map<String, String> bankData = parsePayload(request.getPayload(), new TypeReference<Map<String, String>>() {});
        Bank newBank = bankService.createBank(
                bankData.get("name"),
                bankData.get("registrationNumber"),
                bankData.get("address")
        );
        auditService.logUserAction(this.currentUser, "CREATE_BANK", "Bank created: " + newBank.getName(), clientIpAddress, true, true);
        return ServerResponseDTO.success(newBank); // Bank - простой объект
    }

    private ServerResponseDTO handleGetBankById(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        if (bankId == null) throw new ValidationException("bankId is required.");
        Bank bank = bankService.getBankById(bankId);
        auditService.logUserAction(this.currentUser, "GET_BANK_BY_ID", "Retrieved bank: " + bank.getName(), clientIpAddress, true, false);
        return ServerResponseDTO.success(bank);
    }

    private ServerResponseDTO handleGetAllBanks(ClientRequestDTO request) throws ServiceException {
        List<Bank> banks = bankService.getAllBanks();
        auditService.logUserAction(this.currentUser, "GET_ALL_BANKS", "Retrieved list of all banks", clientIpAddress, true, false);
        return ServerResponseDTO.success(banks);
    }

    private ServerResponseDTO handleUpdateBank(ClientRequestDTO request) throws ServiceException, ValidationException, EntityNotFoundException {
        Map<String, Object> bankData = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) bankData.get("id");
        if (bankId == null) throw new ValidationException("Bank ID is required for update.");

        bankService.updateBank(
                bankId,
                (String) bankData.get("name"),
                (String) bankData.get("registrationNumber"),
                (String) bankData.get("address")
        );
        auditService.logUserAction(this.currentUser, "UPDATE_BANK", "Bank updated, ID: " + bankId, clientIpAddress, true, true);
        return ServerResponseDTO.success("Bank updated successfully.");
    }

    private ServerResponseDTO handleDeleteBank(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        if (bankId == null) throw new ValidationException("bankId is required.");

        bankService.deleteBank(bankId);
        auditService.logUserAction(this.currentUser, "DELETE_BANK", "Bank deleted, ID: " + bankId, clientIpAddress, true, true);
        return ServerResponseDTO.success("Bank deleted successfully.");
    }


    // --- Financial Statement Operations ---
    private ServerResponseDTO handleCreateFinancialStatement(ClientRequestDTO request) throws ServiceException, ValidationException, EntityNotFoundException {
        FinancialStatementDTO statementDTO = parsePayload(request.getPayload(), FinancialStatementDTO.class);
        FinancialStatement newStatement = financialStatementService.createFinancialStatement(statementDTO, this.currentUser);
        auditService.logUserAction(this.currentUser, "CREATE_FIN_STATEMENT",
                "Financial statement created for bank ID " + newStatement.getBankId() + ", date " + newStatement.getReportDate(), clientIpAddress, true, true);
        // Возвращаем DTO, а не полный объект с элементами, если они большие
        return ServerResponseDTO.success(new FinancialStatementDTO(newStatement.getId(), newStatement.getBankId(), newStatement.getBank().getName(),
                newStatement.getReportDate(), newStatement.getStatementType(), newStatement.getCurrency(),
                newStatement.getCreatedAt(), newStatement.getCreatedByUserId(),
                newStatement.getCreatedByUser() != null ? newStatement.getCreatedByUser().getUsername() : null,
                null)); // null для items в ответе
    }

    private ServerResponseDTO handleGetFinancialStatement(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer statementId = (Integer) payload.get("statementId");
        if (statementId == null) throw new ValidationException("statementId is required.");

        FinancialStatement statement = financialStatementService.getFinancialStatementWithItemsById(statementId);
        FinancialStatementDTO dto = FinancialStatementDTO.fromFinancialStatement(statement, true); // true - include items
        auditService.logUserAction(this.currentUser, "GET_FIN_STATEMENT", "Retrieved financial statement ID " + statementId, clientIpAddress, true, false);
        return ServerResponseDTO.success(dto);
    }

    private ServerResponseDTO handleGetBankFinancialStatements(ClientRequestDTO request) throws ServiceException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        if (bankId == null) throw new ValidationException("bankId is required.");

        List<FinancialStatementDTO> overviews = financialStatementService.getFinancialStatementOverviewsByBank(bankId);
        auditService.logUserAction(this.currentUser, "GET_BANK_FIN_STATEMENTS", "Retrieved financial statement overviews for bank ID " + bankId, clientIpAddress, true, false);
        return ServerResponseDTO.success(overviews);
    }

    private ServerResponseDTO handleDeleteFinancialStatement(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer statementId = (Integer) payload.get("statementId");
        if (statementId == null) throw new ValidationException("statementId is required.");

        financialStatementService.deleteFinancialStatement(statementId);
        auditService.logUserAction(this.currentUser, "DELETE_FIN_STATEMENT", "Financial statement deleted, ID: " + statementId, clientIpAddress, true, true);
        return ServerResponseDTO.success("Financial statement deleted successfully.");
    }


    // --- Analysis Operations ---
    private ServerResponseDTO handleCalculateLiquidity(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        String dateStr = (String) payload.get("reportDate"); // yyyy-MM-dd
        if (bankId == null) throw new ValidationException("bankId is required.");
        LocalDate reportDate = dateStr != null ? LocalDate.parse(dateStr) : null;

        LiquidityAnalysisResultDTO result = analysisService.calculateLiquidity(bankId, reportDate, this.currentUser, clientIpAddress);
        // Аудит уже внутри сервиса analysisService
        return ServerResponseDTO.success(result);
    }

    private ServerResponseDTO handleCalculateSolvency(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        String dateStr = (String) payload.get("reportDate");
        if (bankId == null) throw new ValidationException("bankId is required.");
        LocalDate reportDate = dateStr != null ? LocalDate.parse(dateStr) : null;

        SolvencyAnalysisResultDTO result = analysisService.calculateSolvency(bankId, reportDate, this.currentUser, clientIpAddress);
        return ServerResponseDTO.success(result);
    }

    private ServerResponseDTO handleSaveLiquidityReport(ClientRequestDTO request) throws ServiceException, ValidationException {
        LiquidityAnalysisResultDTO resultDTO = parsePayload(request.getPayload(), LiquidityAnalysisResultDTO.class);
        AnalysisReport savedReport = analysisService.saveLiquidityAnalysisReport(resultDTO, this.currentUser);
        return ServerResponseDTO.success(AnalysisReportDTO.fromAnalysisReport(savedReport));
    }

    private ServerResponseDTO handleSaveSolvencyReport(ClientRequestDTO request) throws ServiceException, ValidationException {
        SolvencyAnalysisResultDTO resultDTO = parsePayload(request.getPayload(), SolvencyAnalysisResultDTO.class);
        AnalysisReport savedReport = analysisService.saveSolvencyAnalysisReport(resultDTO, this.currentUser);
        return ServerResponseDTO.success(AnalysisReportDTO.fromAnalysisReport(savedReport));
    }

    private ServerResponseDTO handleGetAnalysisReport(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer reportId = (Integer) payload.get("reportId");
        if (reportId == null) throw new ValidationException("reportId is required.");
        AnalysisReport report = analysisService.getAnalysisReportById(reportId);
        auditService.logUserAction(this.currentUser, "GET_ANALYSIS_REPORT", "Retrieved analysis report ID " + reportId, clientIpAddress, true, false);
        return ServerResponseDTO.success(AnalysisReportDTO.fromAnalysisReport(report));
    }

    private ServerResponseDTO handleGetBankAnalysisReports(ClientRequestDTO request) throws ServiceException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer bankId = (Integer) payload.get("bankId");
        if (bankId == null) throw new ValidationException("bankId is required.");
        List<AnalysisReport> reports = analysisService.getAnalysisReportsByBank(bankId);
        List<AnalysisReportDTO> dtos = reports.stream().map(AnalysisReportDTO::fromAnalysisReport).collect(Collectors.toList());
        auditService.logUserAction(this.currentUser, "GET_BANK_ANALYSIS_REPORTS", "Retrieved analysis reports for bank ID " + bankId, clientIpAddress, true, false);
        return ServerResponseDTO.success(dtos);
    }

    private ServerResponseDTO handleDeleteAnalysisReport(ClientRequestDTO request) throws ServiceException, EntityNotFoundException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer reportId = (Integer) payload.get("reportId");
        if (reportId == null) throw new ValidationException("reportId is required.");
        analysisService.deleteAnalysisReport(reportId, this.currentUser, clientIpAddress);
        return ServerResponseDTO.success("Analysis report deleted successfully.");
    }

    // --- Audit Operations ---
    private ServerResponseDTO handleGetAllAuditLogs(ClientRequestDTO request) throws ServiceException {
        List<AuditLogEntry> logs = auditService.getAllAuditLogs();
        // Конвертировать в DTO, если нужно (например, чтобы не слать полный User объект)
        List<AuditLogDTO> dtos = logs.stream().map(AuditLogDTO::fromAuditLogEntry).collect(Collectors.toList());
        auditService.logUserAction(this.currentUser, "GET_ALL_AUDIT_LOGS", "Retrieved all audit logs", clientIpAddress, true, false);
        return ServerResponseDTO.success(dtos);
    }

    private ServerResponseDTO handleGetUserAuditLogs(ClientRequestDTO request) throws ServiceException, ValidationException {
        Map<String, Object> payload = parsePayload(request.getPayload(), new TypeReference<Map<String, Object>>() {});
        Integer targetUserId = (Integer) payload.get("userId");
        if (targetUserId == null) throw new ValidationException("Target userId is required.");
        List<AuditLogEntry> logs = auditService.getAuditLogsByUser(targetUserId);
        List<AuditLogDTO> dtos = logs.stream().map(AuditLogDTO::fromAuditLogEntry).collect(Collectors.toList());
        auditService.logUserAction(this.currentUser, "GET_USER_AUDIT_LOGS", "Retrieved audit logs for user ID " + targetUserId, clientIpAddress, true, false);
        return ServerResponseDTO.success(dtos);
    }


    // --- Вспомогательные методы ---
    private <T> T parsePayload(Object payloadObject, Class<T> targetClass) throws ValidationException {
        if (payloadObject == null) {
            throw new ValidationException("Request payload is missing.");
        }
        try {
            // Если payload уже нужного типа (Jackson может это сделать, если настроен AllowPolymorphicDeserialization)
            // Но безопаснее переконвертировать из Map или строки JSON, если payload это строка.
            if (targetClass.isInstance(payloadObject)) {
                return targetClass.cast(payloadObject);
            }
            // Если payload это Map, ObjectMapper может сконвертировать его в объект
            return objectMapper.convertValue(payloadObject, targetClass);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse payload to {}: {}", targetClass.getSimpleName(), e.getMessage());
            throw new ValidationException("Invalid payload structure for the action. Expected " + targetClass.getSimpleName() + ".");
        }
    }

    private <T> T parsePayload(Object payloadObject, TypeReference<T> typeReference) throws ValidationException {
        if (payloadObject == null) {
            throw new ValidationException("Request payload is missing.");
        }
        try {
            return objectMapper.convertValue(payloadObject, typeReference);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to parse payload to {}: {}", typeReference.getType().getTypeName(), e.getMessage());
            throw new ValidationException("Invalid payload structure for the action. Expected " + typeReference.getType().getTypeName() + ".");
        }
    }

    private void logFailedRequest(ClientRequestDTO request, String errorType, String errorMessage) {
        if (auditService == null) return; // Если аудит сервис еще не инициализирован
        String action = (request != null && request.getAction() != null) ? request.getAction() : "UNKNOWN_ACTION";
        try {
            auditService.logAction(
                    this.currentUser != null ? this.currentUser.getId() : null,
                    action + "_" + errorType,
                    "Error: " + errorMessage + (request != null ? ", RequestPayloadType: " + (request.getPayload() != null ? request.getPayload().getClass().getSimpleName() : "null") : ""),
                    clientIpAddress,
                    false,
                    false // Не считаем это успехом основной операции
            );
        } catch (ServiceException e) {
            LOGGER.error("Failed to log failed request audit entry: {}", e.getMessage());
        }
    }


    private void closeClientSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing client socket for {}: {}", clientIpAddress, e.getMessage());
        }
    }
}