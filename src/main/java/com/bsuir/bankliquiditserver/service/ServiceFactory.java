package com.bsuir.bankliquiditserver.service; // или com.bsuir.bankliquiditserver.factory

import com.bsuir.bankliquiditserver.dao.*;
import com.bsuir.bankliquiditserver.service.impl.*;

// Паттерн Factory Method (или простой статический доступ к синглтонам сервисов)
public class ServiceFactory {

    // --- DAO Instances ---
    // DAO не обязательно должны быть синглтонами, если они stateless.
    // Но для простоты и консистентности можно сделать их доступными через фабрику.
    private static final RoleDao ROLE_DAO = new RoleDao();
    private static final UserDao USER_DAO = new UserDao();
    private static final BankDao BANK_DAO = new BankDao();
    private static final FinancialStatementDao FINANCIAL_STATEMENT_DAO = new FinancialStatementDao();
    private static final StatementItemDao STATEMENT_ITEM_DAO = new StatementItemDao();
    private static final AnalysisReportDao ANALYSIS_REPORT_DAO = new AnalysisReportDao();
    private static final AuditLogDao AUDIT_LOG_DAO = new AuditLogDao();

    // --- Service Instances (обычно синглтоны) ---
    private static final UserService USER_SERVICE = new UserServiceImpl(USER_DAO, ROLE_DAO);
    private static final RoleService ROLE_SERVICE = new RoleServiceImpl(ROLE_DAO, USER_DAO);
    private static final BankService BANK_SERVICE = new BankServiceImpl(BANK_DAO, FINANCIAL_STATEMENT_DAO);
    private static final AuditService AUDIT_SERVICE = new AuditServiceImpl(AUDIT_LOG_DAO, USER_DAO);
    private static final FinancialStatementService FINANCIAL_STATEMENT_SERVICE =
            new FinancialStatementServiceImpl(FINANCIAL_STATEMENT_DAO, STATEMENT_ITEM_DAO, BANK_DAO, USER_DAO);
    private static final AnalysisService ANALYSIS_SERVICE =
            new AnalysisServiceImpl(FINANCIAL_STATEMENT_SERVICE, ANALYSIS_REPORT_DAO, BANK_DAO, AUDIT_SERVICE);


    // --- Getters for Services ---
    public static UserService getUserService() {
        return USER_SERVICE;
    }

    public static RoleService getRoleService() {
        return ROLE_SERVICE;
    }

    public static BankService getBankService() {
        return BANK_SERVICE;
    }

    public static FinancialStatementService getFinancialStatementService() {
        return FINANCIAL_STATEMENT_SERVICE;
    }

    public static AnalysisService getAnalysisService() {
        return ANALYSIS_SERVICE;
    }

    public static AuditService getAuditService() {
        return AUDIT_SERVICE;
    }

    // Приватный конструктор, чтобы запретить создание экземпляров фабрики, если все методы статические
    private ServiceFactory() {}
}