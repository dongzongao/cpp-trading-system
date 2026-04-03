#pragma once

#include <deque>
#include <functional>
#include <map>
#include <vector>

#include "trading/order.hpp"

namespace trading {

class OrderBook {
public:
    std::vector<Trade> submit(Order order);
    [[nodiscard]] TopOfBook top_of_book() const;

private:
    using BuyLevels = std::map<Price, std::deque<Order>, std::greater<>>;
    using SellLevels = std::map<Price, std::deque<Order>, std::less<>>;

    static Quantity aggregate_quantity(const std::deque<Order>& orders);

    std::vector<Trade> match_buy(Order& incoming);
    std::vector<Trade> match_sell(Order& incoming);

    BuyLevels bids_;
    SellLevels asks_;
};

}  // namespace trading
