-- Initialize authority configuration if it doesn't exist
INSERT INTO authority_config (id, default_authority_system) 
VALUES (1, 'MAO') 
ON CONFLICT (id) DO NOTHING;

-- Initialize test data for reconciliation (only for development)
INSERT INTO reconciliation_result (
    id, 
    item_id, 
    location_id, 
    mao_quantity, 
    mawm_quantity, 
    discrepancy, 
    reconciled_quantity, 
    authority_system, 
    auto_resolved, 
    reconciled, 
    reconciliation_message, 
    reconciliation_time, 
    resolved_time
) VALUES (
    nextval('reconciliation_result_seq'),
    'TEST_ITEM_001',
    'TEST_LOC_001',
    100,
    100,
    0,
    100,
    'MAO',
    true,
    true,
    'No discrepancy detected',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;