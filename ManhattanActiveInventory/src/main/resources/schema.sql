-- Fix reconciliation_result table schema to ensure non-nullable columns have proper defaults

-- Drop existing table to recreate with proper constraints
DROP TABLE IF EXISTS reconciliation_result CASCADE;

-- Drop the existing sequence if exists
DROP SEQUENCE IF EXISTS reconciliation_result_seq CASCADE;

-- Create sequence with proper parameters
CREATE SEQUENCE IF NOT EXISTS reconciliation_result_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START WITH 1
    CACHE 1;

-- Create table with proper constraints and defaults
CREATE TABLE IF NOT EXISTS reconciliation_result (
    id BIGINT PRIMARY KEY DEFAULT nextval('reconciliation_result_seq'),
    item_id VARCHAR(255),
    location_id VARCHAR(255),
    mao_quantity INT NOT NULL DEFAULT 0,
    mawm_quantity INT NOT NULL DEFAULT 0,
    discrepancy INT NOT NULL DEFAULT 0,
    reconciled_quantity INT NOT NULL DEFAULT 0,
    authority_system VARCHAR(255),
    auto_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    reconciliation_message VARCHAR(1000),
    reconciliation_time TIMESTAMP,
    resolved_time TIMESTAMP,
    
    -- New columns for approval workflow
    needs_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    rejected BOOLEAN NOT NULL DEFAULT FALSE,
    approver_notes VARCHAR(1000),
    rejection_reason VARCHAR(1000),
    approval_time TIMESTAMP,
    approved_by VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_item_id ON reconciliation_result(item_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_location_id ON reconciliation_result(location_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_reconciliation_time ON reconciliation_result(reconciliation_time);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_resolved_time ON reconciliation_result(resolved_time);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_status ON reconciliation_result(status);