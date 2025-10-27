-- Create trades table for trade-store feature
CREATE TABLE IF NOT EXISTS trades (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    price NUMERIC(19,4),
    quantity INTEGER,
    maturity_date DATE,
    status VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    opt_lock INTEGER DEFAULT 0
);

-- Add a unique constraint on trade_id if not present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'trades'
          AND tc.constraint_type = 'UNIQUE'
          AND kcu.column_name = 'trade_id'
    ) THEN
        ALTER TABLE trades
        ADD CONSTRAINT uc_trade_tradeid UNIQUE (trade_id);
    END IF;
END $$;
