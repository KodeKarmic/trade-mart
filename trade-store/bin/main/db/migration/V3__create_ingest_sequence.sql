-- Create a DB sequence for ingest ordering
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = 'trade_ingest_seq'
  ) THEN
    CREATE SEQUENCE trade_ingest_seq START 1;
  END IF;
END $$;
