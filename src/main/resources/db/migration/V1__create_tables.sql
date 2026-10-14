
-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Account number generator
CREATE SEQUENCE account_number_seq START WITH 1;

-- 3. Accounts (One user -> Many accounts)
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    account_number VARCHAR(15) NOT NULL UNIQUE,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0
        CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'MNT'
        CHECK (currency = 'MNT'),
    status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Transfers
CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    initiated_by UUID NOT NULL REFERENCES users(id),
    from_account_id UUID NOT NULL REFERENCES accounts(id),
    to_account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19,2) NOT NULL
        CHECK (amount > 0 AND amount = TRUNC(amount)),
    currency VARCHAR(3) NOT NULL
        CHECK (currency = 'MNT'),
    description VARCHAR(255) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'COMPLETED',
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (initiated_by, idempotency_key),
    CHECK (from_account_id <> to_account_id)
);

-- 5. Indexes
CREATE INDEX idx_accounts_user
ON accounts(user_id);

CREATE INDEX idx_transfers_sender
ON transfers(from_account_id, created_at DESC);

CREATE INDEX idx_transfers_receiver
ON transfers(to_account_id, created_at DESC);
