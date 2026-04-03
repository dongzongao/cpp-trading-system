#include "trading/trading_system.hpp"

#include <utility>

namespace trading {

RiskManager::RiskManager(std::unordered_set<std::string> allowed_symbols,
                         Quantity max_order_quantity,
                         double max_order_notional)
    : allowed_symbols_(std::move(allowed_symbols)),
      max_order_quantity_(max_order_quantity),
      max_order_notional_(max_order_notional) {}

RiskResult RiskManager::validate(const OrderRequest& request) const {
    if (!allowed_symbols_.contains(request.symbol)) {
        return {.accepted = false, .reason = "symbol not allowed"};
    }

    if (request.quantity <= 0) {
        return {.accepted = false, .reason = "quantity must be positive"};
    }

    if (request.price <= 0) {
        return {.accepted = false, .reason = "price must be positive"};
    }

    if (request.quantity > max_order_quantity_) {
        return {.accepted = false, .reason = "quantity exceeds risk limit"};
    }

    if ((request.price * static_cast<double>(request.quantity)) > max_order_notional_) {
        return {.accepted = false, .reason = "notional exceeds risk limit"};
    }

    return {.accepted = true, .reason = {}};
}

TradingSystem::TradingSystem(std::unordered_set<std::string> allowed_symbols,
                             Quantity max_order_quantity,
                             double max_order_notional)
    : risk_manager_(std::move(allowed_symbols), max_order_quantity, max_order_notional) {}

SubmissionResult TradingSystem::submit(const OrderRequest& request) {
    const auto risk = risk_manager_.validate(request);
    if (!risk.accepted) {
        return {
            .accepted = false,
            .rejection_reason = risk.reason,
        };
    }

    auto& book = books_[request.symbol];

    Order order {
        .id = next_order_id_++,
        .symbol = request.symbol,
        .side = request.side,
        .price = request.price,
        .quantity = request.quantity,
        .sequence = next_sequence_++,
    };

    const auto order_id = order.id;
    auto trades = book.submit(order);
    auto top = book.top_of_book();

    return {
        .accepted = true,
        .order_id = order_id,
        .open_quantity = order.quantity,
        .book = top,
        .trades = std::move(trades),
    };
}

TopOfBook TradingSystem::top_of_book(const std::string& symbol) const {
    const auto it = books_.find(symbol);
    if (it == books_.end()) {
        return {};
    }
    return it->second.top_of_book();
}

}  // namespace trading
