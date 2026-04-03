#include "trading/order_book.hpp"

namespace trading {

Quantity OrderBook::aggregate_quantity(const std::deque<Order>& orders) {
    Quantity total = 0;
    for (const auto& order : orders) {
        total += order.quantity;
    }
    return total;
}

std::vector<Trade> OrderBook::submit(Order order) {
    if (order.side == Side::Buy) {
        return match_buy(order);
    }
    return match_sell(order);
}

std::vector<Trade> OrderBook::match_buy(Order& incoming) {
    std::vector<Trade> trades;

    while (incoming.quantity > 0 && !asks_.empty()) {
        auto best_ask_it = asks_.begin();
        if (incoming.price < best_ask_it->first) {
            break;
        }

        auto& queue = best_ask_it->second;
        while (incoming.quantity > 0 && !queue.empty()) {
            auto& resting = queue.front();
            const Quantity traded = std::min(incoming.quantity, resting.quantity);

            trades.push_back(Trade{
                .buy_order_id = incoming.id,
                .sell_order_id = resting.id,
                .symbol = incoming.symbol,
                .price = resting.price,
                .quantity = traded,
            });

            incoming.quantity -= traded;
            resting.quantity -= traded;

            if (resting.quantity == 0) {
                queue.pop_front();
            }
        }

        if (queue.empty()) {
            asks_.erase(best_ask_it);
        }
    }

    if (incoming.quantity > 0) {
        bids_[incoming.price].push_back(incoming);
    }

    return trades;
}

std::vector<Trade> OrderBook::match_sell(Order& incoming) {
    std::vector<Trade> trades;

    while (incoming.quantity > 0 && !bids_.empty()) {
        auto best_bid_it = bids_.begin();
        if (incoming.price > best_bid_it->first) {
            break;
        }

        auto& queue = best_bid_it->second;
        while (incoming.quantity > 0 && !queue.empty()) {
            auto& resting = queue.front();
            const Quantity traded = std::min(incoming.quantity, resting.quantity);

            trades.push_back(Trade{
                .buy_order_id = resting.id,
                .sell_order_id = incoming.id,
                .symbol = incoming.symbol,
                .price = resting.price,
                .quantity = traded,
            });

            incoming.quantity -= traded;
            resting.quantity -= traded;

            if (resting.quantity == 0) {
                queue.pop_front();
            }
        }

        if (queue.empty()) {
            bids_.erase(best_bid_it);
        }
    }

    if (incoming.quantity > 0) {
        asks_[incoming.price].push_back(incoming);
    }

    return trades;
}

TopOfBook OrderBook::top_of_book() const {
    TopOfBook top {};

    if (!bids_.empty()) {
        top.best_bid = bids_.begin()->first;
        top.best_bid_quantity = aggregate_quantity(bids_.begin()->second);
    }

    if (!asks_.empty()) {
        top.best_ask = asks_.begin()->first;
        top.best_ask_quantity = aggregate_quantity(asks_.begin()->second);
    }

    return top;
}

}  // namespace trading
