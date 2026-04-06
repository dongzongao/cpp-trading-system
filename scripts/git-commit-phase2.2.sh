#!/bin/bash

# Git commit script for Phase 2.2 Account Context implementation

set -e

echo "🔍 Checking git status..."
git status

echo ""
echo "📝 Adding all changes..."
git add .

echo ""
echo "📊 Showing what will be committed..."
git status --short

echo ""
echo "💾 Committing changes..."
git commit -m "feat(account-service): implement Phase 2.2 Account Context with complete DDD architecture

## Features
- Complete DDD four-layer architecture for account management
- Account creation and management
- Deposit and withdrawal operations
- Fund freezing/unfreezing mechanism
- Position management
- Transaction history tracking

## Domain Layer
- 10 Value Objects: AccountId, Money, Quantity, Symbol, Currency, AccountType, AccountStatus, TransactionType, FreezeReason, PositionId, TransactionId
- Money value object with arithmetic operations and currency validation
- Quantity value object for position management
- 2 Entities: Position, AccountTransaction
- Position entity with increase/decrease/freeze/unfreeze operations
- 1 Aggregate Root: Account with complete business logic
- Account operations: deposit, withdraw, freeze, unfreeze, deductFrozen
- Account status management: suspend, activate, close
- 3 Repository interfaces: AccountRepository, PositionRepository, AccountTransactionRepository

## Application Layer
- 3 Command objects: CreateAccountCommand, DepositCommand, WithdrawCommand
- AccountApplicationService with core business operations
- AccountDTO for data transfer
- Transaction management with @Transactional

## Interface Layer
- REST API Controller with 5 endpoints
- Request objects with validation annotations
- CreateAccountRequest, DepositRequest with Bean Validation

## Infrastructure Layer
- AccountPO entity with JPA annotations
- AccountJpaRepository with custom query methods
- AccountMapper for domain-to-PO conversion
- AccountRepositoryImpl implementing domain repository
- AccountTransactionRepositoryImpl (simplified)
- Database migration script V3__create_accounts_table.sql

## Technical Highlights
- BigDecimal for precise money calculations
- Currency-aware money operations
- Balance consistency constraints (total = available + frozen)
- Optimistic locking for concurrent operations
- Database indexes for performance
- Transaction isolation for data consistency

## API Endpoints
- POST /api/v1/accounts - Create account
- GET /api/v1/accounts/{id} - Get account info
- POST /api/v1/accounts/{id}/deposit - Deposit funds
- POST /api/v1/accounts/{id}/withdraw - Withdraw funds
- GET /api/v1/accounts/{id}/balance - Get account balance

## Database
- accounts table with complete schema
- Indexes on user_id, (user_id, currency), status
- Balance consistency constraints
- Non-negative balance constraints
- Flyway migration V3__create_accounts_table.sql

## Business Rules
- One account per user per currency
- Account must be ACTIVE for operations
- Available balance = Total balance - Frozen balance
- Cannot close account with non-zero balance
- All money operations validate currency consistency

Closes #phase2.2"

echo ""
echo "✅ Commit completed successfully!"
echo ""
echo "📤 To push to remote, run:"
echo "   git push origin main"
