#pragma once

#include <string>

#include "trading/types.hpp"

namespace trading {

struct OrderRequest {
    std::string symbol;
    Side side {Side::Buy};
    Price price {};
    Quantity quantity {};
};

struct Order {
    OrderId id {};
    std::string symbol;
    Side side {Side::Buy};
    Price price {};
    Quantity quantity {};
    Sequence sequence {};
};

}  // namespace trading
