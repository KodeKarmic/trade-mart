-- Add ingest_sequence column to trades for storing ingest ordering
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'trades' AND column_name = 'ingest_sequence'
  ) THEN
    ALTER TABLE trades ADD COLUMN ingest_sequence BIGINT;
  END IF;
END $$;
