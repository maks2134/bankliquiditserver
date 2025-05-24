package com.bsuir.bankliquiditserver.dao;

import com.bsuir.bankliquiditserver.db.DatabaseConnector;
import com.bsuir.bankliquiditserver.model.AnalysisReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Для работы с JSONB в PostgreSQL, если захотим мапить его не просто в строку
// import com.fasterxml.jackson.databind.ObjectMapper; // Понадобится если будем сериализовать/десериализовать JSONB в Map или объект

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AnalysisReportDao implements GenericDao<AnalysisReport, Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisReportDao.class);
    // private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules(); // Для JSONB

    private static final String SELECT_BY_ID = "SELECT id, bank_id, report_type, analysis_date, analyzed_by_user_id, current_ratio, quick_ratio, cash_ratio, debt_to_equity_ratio, total_debt_to_total_assets_ratio, report_data FROM analysis_reports WHERE id = ?;";
    private static final String SELECT_ALL = "SELECT id, bank_id, report_type, analysis_date, analyzed_by_user_id, current_ratio, quick_ratio, cash_ratio, debt_to_equity_ratio, total_debt_to_total_assets_ratio, report_data FROM analysis_reports ORDER BY analysis_date DESC;";
    private static final String INSERT = "INSERT INTO analysis_reports (bank_id, report_type, analysis_date, analyzed_by_user_id, current_ratio, quick_ratio, cash_ratio, debt_to_equity_ratio, total_debt_to_total_assets_ratio, report_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB));";
    private static final String UPDATE = "UPDATE analysis_reports SET bank_id = ?, report_type = ?, analysis_date = ?, analyzed_by_user_id = ?, current_ratio = ?, quick_ratio = ?, cash_ratio = ?, debt_to_equity_ratio = ?, total_debt_to_total_assets_ratio = ?, report_data = CAST(? AS JSONB) WHERE id = ?;";
    private static final String DELETE_BY_ID = "DELETE FROM analysis_reports WHERE id = ?;";
    private static final String SELECT_BY_BANK_ID = "SELECT id, bank_id, report_type, analysis_date, analyzed_by_user_id, current_ratio, quick_ratio, cash_ratio, debt_to_equity_ratio, total_debt_to_total_assets_ratio, report_data FROM analysis_reports WHERE bank_id = ? ORDER BY analysis_date DESC;";
    private static final String SELECT_BY_USER_ID = "SELECT id, bank_id, report_type, analysis_date, analyzed_by_user_id, current_ratio, quick_ratio, cash_ratio, debt_to_equity_ratio, total_debt_to_total_assets_ratio, report_data FROM analysis_reports WHERE analyzed_by_user_id = ? ORDER BY analysis_date DESC;";


    @Override
    public Optional<AnalysisReport> findById(Integer id) throws SQLException {
        AnalysisReport report = null;
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                report = mapResultSetToAnalysisReport(rs);
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AnalysisReport by id {}: {}", id, e.getMessage());
            throw e;
        }
        return Optional.ofNullable(report);
    }

    @Override
    public List<AnalysisReport> findAll() throws SQLException {
        List<AnalysisReport> reports = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reports.add(mapResultSetToAnalysisReport(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding all AnalysisReports: {}", e.getMessage());
            throw e;
        }
        return reports;
    }

    public List<AnalysisReport> findByBankId(Integer bankId) throws SQLException {
        List<AnalysisReport> reports = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_BANK_ID)) {
            ps.setInt(1, bankId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reports.add(mapResultSetToAnalysisReport(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AnalysisReports by bank_id {}: {}", bankId, e.getMessage());
            throw e;
        }
        return reports;
    }

    public List<AnalysisReport> findByUserId(Integer userId) throws SQLException {
        List<AnalysisReport> reports = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_BY_USER_ID)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reports.add(mapResultSetToAnalysisReport(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding AnalysisReports by user_id {}: {}", userId, e.getMessage());
            throw e;
        }
        return reports;
    }

    @Override
    public AnalysisReport save(AnalysisReport report) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, report.getBankId());
            ps.setString(2, report.getReportType());
            ps.setTimestamp(3, report.getAnalysisDate() != null ? Timestamp.valueOf(report.getAnalysisDate()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setInt(4, report.getAnalyzedByUserId());
            ps.setBigDecimal(5, report.getCurrentRatio());
            ps.setBigDecimal(6, report.getQuickRatio());
            ps.setBigDecimal(7, report.getCashRatio());
            ps.setBigDecimal(8, report.getDebtToEquityRatio());
            ps.setBigDecimal(9, report.getTotalDebtToTotalAssetsRatio());
            // Для JSONB, передаем строку. PostgreSQL сам ее распарсит.
            // Если report.getReportData() это уже JSON строка, то все ок.
            // Если это объект, его нужно сначала сериализовать в JSON строку с помощью Jackson.
            ps.setString(10, report.getReportData()); // Предполагается, что это уже JSON строка

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating AnalysisReport failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    report.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating AnalysisReport failed, no ID obtained.");
                }
            }
            LOGGER.info("AnalysisReport saved: {}", report);
            return report;
        } catch (SQLException e) {
            LOGGER.error("Error saving AnalysisReport {}: {}", report, e.getMessage());
            throw e;
        }
    }

    @Override
    public void update(AnalysisReport report) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(UPDATE)) {
            ps.setInt(1, report.getBankId());
            ps.setString(2, report.getReportType());
            ps.setTimestamp(3, Timestamp.valueOf(report.getAnalysisDate()));
            ps.setInt(4, report.getAnalyzedByUserId());
            ps.setBigDecimal(5, report.getCurrentRatio());
            ps.setBigDecimal(6, report.getQuickRatio());
            ps.setBigDecimal(7, report.getCashRatio());
            ps.setBigDecimal(8, report.getDebtToEquityRatio());
            ps.setBigDecimal(9, report.getTotalDebtToTotalAssetsRatio());
            ps.setString(10, report.getReportData());
            ps.setInt(11, report.getId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating AnalysisReport failed, no rows affected for ID: " + report.getId());
            }
            LOGGER.info("AnalysisReport updated: {}", report);
        } catch (SQLException e) {
            LOGGER.error("Error updating AnalysisReport {}: {}", report, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteById(Integer id) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_BY_ID)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                LOGGER.warn("No AnalysisReport found with ID {} to delete.", id);
            } else {
                LOGGER.info("AnalysisReport deleted with ID: {}", id);
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting AnalysisReport by id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private AnalysisReport mapResultSetToAnalysisReport(ResultSet rs) throws SQLException {
        AnalysisReport report = new AnalysisReport();
        report.setId(rs.getInt("id"));
        report.setBankId(rs.getInt("bank_id"));
        report.setReportType(rs.getString("report_type"));
        Timestamp analysisDateTs = rs.getTimestamp("analysis_date");
        if (analysisDateTs != null) {
            report.setAnalysisDate(analysisDateTs.toLocalDateTime());
        }
        report.setAnalyzedByUserId(rs.getInt("analyzed_by_user_id"));
        report.setCurrentRatio(rs.getBigDecimal("current_ratio"));
        report.setQuickRatio(rs.getBigDecimal("quick_ratio"));
        report.setCashRatio(rs.getBigDecimal("cash_ratio"));
        report.setDebtToEquityRatio(rs.getBigDecimal("debt_to_equity_ratio"));
        report.setTotalDebtToTotalAssetsRatio(rs.getBigDecimal("total_debt_to_total_assets_ratio"));
        report.setReportData(rs.getString("report_data")); // Читаем JSONB как строку

        // Загрузка связанных объектов Bank и User - задача сервисного слоя
        return report;
    }
}