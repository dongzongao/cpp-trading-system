#pragma once

#include <unordered_map>
#include <vector>

#include "trading/order_book.hpp"
#include "trading/risk_manager.hpp"

namespace trading {

struct SubmissionResult {
    bool accepted {false};
    std::string rejection_reason;
    OrderId order_id {};
    Quantity open_quantity {};
    TopOfBook book;
    std::vector<Trade> trades;
};

class TradingSystem {
public:
    TradingSystem(std::unordered_set<std::string> allowed_symbols,
                  Quantity max_order_quantity,
                  double max_order_notional);

    SubmissionResult submit(const OrderRequest& request);
    [[nodiscard]] TopOfBook top_of_book(const std::string& symbol) const;

private:
    RiskManager risk_manager_;
    std::unordered_map<std::string, OrderBook> books_;
    OrderId next_order_id_ {1};
    Sequence next_sequence_ {1};
};

}  // namespace trading
