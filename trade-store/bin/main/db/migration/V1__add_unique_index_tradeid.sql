DO $$
BEGIN
	-- Only create the index if the table exists and the index does not.
	IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trades') THEN
		IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_tradeid_unique') THEN
			CREATE UNIQUE INDEX idx_tradeid_unique ON trades (tradeid);
		END IF;
	END IF;
END $$;
