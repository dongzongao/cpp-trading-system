#pragma once

#include <cstdint>
#include <optional>
#include <string>

namespace trading {

using Price = double;
using Quantity = std::int64_t;
using OrderId = std::uint64_t;
using Sequence = std::uint64_t;

enum class Side {
    Buy,
    Sell
};

inline std::string to_string(Side side) {
    return side == Side::Buy ? "BUY" : "SELL";
}

struct Trade {
    OrderId buy_order_id {};
    OrderId sell_order_id {};
    std::string symbol;
    Price price {};
    Quantity quantity {};
};

struct TopOfBook {
    std::optional<Price> best_bid;
    Quantity best_bid_quantity {};
    std::optional<Price> best_ask;
    Quantity best_ask_quantity {};
};

}  // namespace trading
