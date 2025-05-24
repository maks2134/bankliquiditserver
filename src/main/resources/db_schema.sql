DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS analysis_reports;
DROP TABLE IF EXISTS statement_items;
DROP TABLE IF EXISTS financial_statements;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS banks;

CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       role_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE banks (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) UNIQUE NOT NULL,
                       registration_number VARCHAR(100) UNIQUE,
                       address TEXT
);

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255),
                       email VARCHAR(255) UNIQUE,
                       role_id INTEGER NOT NULL,
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE financial_statements (
                                      id SERIAL PRIMARY KEY,
                                      bank_id INTEGER NOT NULL,
                                      report_date DATE NOT NULL,
                                      statement_type VARCHAR(50) NOT NULL,
                                      currency VARCHAR(3) DEFAULT 'BYN',
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      created_by_user_id INTEGER,
                                      UNIQUE (bank_id, report_date, statement_type),
                                      FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE,
                                      FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE statement_items (
                                 id SERIAL PRIMARY KEY,
                                 statement_id INTEGER NOT NULL,
                                 item_code VARCHAR(50),
                                 item_name VARCHAR(255) NOT NULL,
                                 item_value DECIMAL(18, 2) NOT NULL,
                                 parent_item_id INTEGER NULL,
                                 FOREIGN KEY (statement_id) REFERENCES financial_statements(id) ON DELETE CASCADE,
                                 FOREIGN KEY (parent_item_id) REFERENCES statement_items(id) ON DELETE SET NULL
);
CREATE INDEX idx_statement_items_statement_id ON statement_items(statement_id);
CREATE INDEX idx_statement_items_item_name ON statement_items(item_name);

CREATE TABLE analysis_reports (
                                  id SERIAL PRIMARY KEY,
                                  bank_id INTEGER NOT NULL,
                                  report_type VARCHAR(100) NOT NULL,
                                  analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  analyzed_by_user_id INTEGER NOT NULL,
                                  current_ratio DECIMAL(10, 4),
                                  quick_ratio DECIMAL(10, 4),
                                  cash_ratio DECIMAL(10, 4),
                                  debt_to_equity_ratio DECIMAL(10, 4),
                                  total_debt_to_total_assets_ratio DECIMAL(10, 4),
                                  report_data JSONB,
                                  FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE,
                                  FOREIGN KEY (analyzed_by_user_id) REFERENCES users(id)
);

CREATE TABLE audit_log (
                           id SERIAL PRIMARY KEY,
                           user_id INTEGER,
                           action_type VARCHAR(100) NOT NULL,
                           details TEXT,
                           ip_address VARCHAR(45),
                           timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           success BOOLEAN,
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO roles (role_name) VALUES ('ADMIN'), ('ANALYST'), ('GUEST');

INSERT INTO users (username, password_hash, full_name, email, role_id)
VALUES ('admin', 'hashed_admin_password', 'Administrator', 'admin@bank.com', (SELECT id FROM roles WHERE role_name = 'ADMIN'));

INSERT INTO banks (name, registration_number, address)
VALUES ('My First Commercial Bank', 'MFB-12345', 'Minsk, Nezavisimosti Ave. 1');