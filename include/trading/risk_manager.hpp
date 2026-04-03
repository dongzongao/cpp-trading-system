#pragma once

#include <string>
#include <unordered_set>

#include "trading/order.hpp"

namespace trading {

struct RiskResult {
    bool accepted {false};
    std::string reason;
};

class RiskManager {
public:
    RiskManager(std::unordered_set<std::string> allowed_symbols,
                Quantity max_order_quantity,
                double max_order_notional);

    [[nodiscard]] RiskResult validate(const OrderRequest& request) const;

private:
    std::unordered_set<std::string> allowed_symbols_;
    Quantity max_order_quantity_ {};
    double max_order_notional_ {};
};

}  // namespace trading
