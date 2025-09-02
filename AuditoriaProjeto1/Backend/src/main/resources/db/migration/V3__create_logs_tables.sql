-- Tabela principal para todos os logs da aplicação
CREATE TABLE
    application_logs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        timestamp DATETIME NOT NULL,
        level VARCHAR(10) NOT NULL,
        logger_name VARCHAR(255) NOT NULL,
        thread_name VARCHAR(100),
        message TEXT NOT NULL,
        exception_message TEXT,
        exception_class VARCHAR(255),
        stack_trace LONGTEXT,
        mdc_data JSON,
        request_id VARCHAR(100),
        user_id VARCHAR(100),
        session_id VARCHAR(100),
        ip_address VARCHAR(45),
        user_agent TEXT,
        request_uri VARCHAR(500),
        http_method VARCHAR(10),
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_timestamp (timestamp),
        INDEX idx_level (level),
        INDEX idx_logger (logger_name),
        INDEX idx_request_id (request_id),
        INDEX idx_user_id (user_id),
        INDEX idx_ip_address (ip_address)
    );

-- Tabela para logs de requisições HTTP
CREATE TABLE
    http_request_logs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        request_id VARCHAR(100) NOT NULL UNIQUE,
        method VARCHAR(10) NOT NULL,
        uri VARCHAR(500) NOT NULL,
        query_string VARCHAR(1000),
        headers JSON,
        request_body LONGTEXT,
        response_status INTEGER,
        response_body LONGTEXT,
        response_headers JSON,
        processing_time_ms BIGINT,
        user_id VARCHAR(100),
        session_id VARCHAR(100),
        ip_address VARCHAR(45),
        user_agent TEXT,
        timestamp DATETIME NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_timestamp (timestamp),
        INDEX idx_method (method),
        INDEX idx_uri (uri),
        INDEX idx_user_id (user_id),
        INDEX idx_ip_address (ip_address),
        INDEX idx_status (response_status)
    );

-- Tabela para logs de transações de banco de dados
CREATE TABLE
    database_transaction_logs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        transaction_id VARCHAR(100) NOT NULL,
        operation_type VARCHAR(50) NOT NULL, -- INSERT, UPDATE, DELETE, SELECT
        table_name VARCHAR(100) NOT NULL,
        entity_id VARCHAR(100),
        sql_query TEXT,
        parameters JSON,
        execution_time_ms BIGINT,
        rows_affected INTEGER,
        user_id VARCHAR(100),
        timestamp DATETIME NOT NULL,
        success BOOLEAN DEFAULT TRUE,
        error_message TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_timestamp (timestamp),
        INDEX idx_operation (operation_type),
        INDEX idx_table (table_name),
        INDEX idx_user_id (user_id),
        INDEX idx_transaction_id (transaction_id)
    );

-- Tabela para logs de segurança específicos
CREATE TABLE
    security_logs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        event_type VARCHAR(100) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAILED, PERMISSION_DENIED, etc.
        user_identifier VARCHAR(255),
        ip_address VARCHAR(45),
        user_agent TEXT,
        resource_accessed VARCHAR(500),
        permission_required VARCHAR(100),
        additional_data JSON,
        risk_level VARCHAR(20) DEFAULT 'LOW', -- LOW, MEDIUM, HIGH, CRITICAL
        timestamp DATETIME NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_timestamp (timestamp),
        INDEX idx_event_type (event_type),
        INDEX idx_user_identifier (user_identifier),
        INDEX idx_ip_address (ip_address),
        INDEX idx_risk_level (risk_level)
    );

-- Tabela para logs de performance
CREATE TABLE
    performance_logs (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        component_name VARCHAR(255) NOT NULL,
        operation_name VARCHAR(255) NOT NULL,
        execution_time_ms BIGINT NOT NULL,
        memory_used_mb DECIMAL(10, 2),
        cpu_usage_percent DECIMAL(5, 2),
        additional_metrics JSON,
        user_id VARCHAR(100),
        request_id VARCHAR(100),
        timestamp DATETIME NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_timestamp (timestamp),
        INDEX idx_component (component_name),
        INDEX idx_operation (operation_name),
        INDEX idx_execution_time (execution_time_ms)
    );