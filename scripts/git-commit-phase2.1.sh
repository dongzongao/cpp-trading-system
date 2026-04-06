#!/bin/bash

# Git commit script for Phase 2.1 User Context implementation

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
git commit -m "feat(user-service): implement Phase 2.1 User Context with complete DDD architecture

## Features
- Complete DDD four-layer architecture implementation
- User registration and authentication with JWT
- KYC submission and approval workflow
- User status management (active, suspended, closed)
- Role-based access control

## Domain Layer
- 8 Value Objects: UserId, Username, Email, Password, Phone, FullName, IdNumber, Address
- 4 Enums: UserStatus, KycStatus, Role, IdType
- 1 Entity: KycInfo
- 1 Aggregate Root: User with complete business logic
- 7 Domain Events for event-driven architecture
- 3 Domain Services: JWT, Authentication, Duplication Check
- Repository interface

## Application Layer
- 3 Command objects for CQRS pattern
- 2 DTOs for data transfer
- UserApplicationService with core business operations
- Assembler for domain-to-DTO conversion

## Interface Layer
- REST API Controller with 7 endpoints
- Request objects with validation annotations
- Global exception handler

## Infrastructure Layer
- JPA entity (UserPO) with proper indexing
- JPA repository implementation
- Domain-to-PO mapper
- Flyway database migration script

## Technical Highlights
- BCrypt password encryption
- JWT token authentication
- Parameter validation with Bean Validation
- Transaction management
- Database indexing optimization
- Event-driven design

## API Endpoints
- POST /api/v1/users/register - User registration
- POST /api/v1/users/login - User login
- POST /api/v1/users/{id}/kyc - Submit KYC
- POST /api/v1/users/{id}/kyc/approve - Approve KYC
- POST /api/v1/users/{id}/kyc/reject - Reject KYC
- POST /api/v1/users/{id}/verify-email - Verify email
- GET /api/v1/users/{id} - Get user info

## Database
- users table with complete schema
- Indexes on username, email, status, created_at
- Flyway migration V1__create_users_table.sql

## Documentation
- Complete implementation summary in docs/user-context-implementation.md
- DDD design patterns followed
- Production-ready code quality

Closes #phase2.1"

echo ""
echo "✅ Commit completed successfully!"
echo ""
echo "📤 To push to remote, run:"
echo "   git push origin main"
