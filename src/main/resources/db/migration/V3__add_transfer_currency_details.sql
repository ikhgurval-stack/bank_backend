
-- Add original transfer amount and currency information.
ALTER TABLE transfers
    ADD COLUMN requested_amount NUMERIC(19, 2),
    ADD COLUMN requested_currency VARCHAR(3),
    ADD COLUMN exchange_rate NUMERIC(19, 2);

-- Existing transfers were all settled in MNT.
UPDATE transfers
SET
    requested_amount = amount,
    requested_currency = 'MNT',
    exchange_rate = 1.00;

-- Validate non-null values when provided.
ALTER TABLE transfers
    ADD CONSTRAINT chk_requested_amount_positive
        CHECK (requested_amount > 0),

    ADD CONSTRAINT chk_requested_currency_supported
        CHECK (requested_currency IN ('MNT', 'USD')),

    ADD CONSTRAINT chk_exchange_rate_positive
        CHECK (exchange_rate > 0);
