#include <iomanip>
#include <iostream>
#include <vector>

#include "trading/trading_system.hpp"

using trading::OrderRequest;
using trading::Side;
using trading::Trade;
using trading::TradingSystem;

namespace {

void print_trades(const std::vector<Trade>& trades) {
    for (const auto& trade : trades) {
        std::cout << "TRADE symbol=" << trade.symbol
                  << " qty=" << trade.quantity
                  << " price=" << std::fixed << std::setprecision(2) << trade.price
                  << " buy_id=" << trade.buy_order_id
                  << " sell_id=" << trade.sell_order_id << '\n';
    }
}

void print_book(const trading::TopOfBook& book) {
    std::cout << "BOOK ";
    if (book.best_bid) {
        std::cout << "bid=" << *book.best_bid << " x " << book.best_bid_quantity << ' ';
    } else {
        std::cout << "bid=NA ";
    }

    if (book.best_ask) {
        std::cout << "ask=" << *book.best_ask << " x " << book.best_ask_quantity;
    } else {
        std::cout << "ask=NA";
    }

    std::cout << '\n';
}

}  // namespace

int main() {
    TradingSystem system({"BTC-USD"}, 100, 2'000'000.0);

    const std::vector<OrderRequest> flow = {
        {"BTC-USD", Side::Buy, 99500.0, 10},
        {"BTC-USD", Side::Buy, 99450.0, 15},
        {"BTC-USD", Side::Sell, 99500.0, 6},
        {"BTC-USD", Side::Sell, 99480.0, 8},
    };

    for (const auto& request : flow) {
        const auto result = system.submit(request);
        std::cout << (result.accepted ? "ACCEPTED " : "REJECTED ")
                  << trading::to_string(request.side)
                  << " order for " << request.symbol
                  << " qty=" << request.quantity
                  << " price=" << request.price;

        if (!result.accepted) {
            std::cout << " reason=" << result.rejection_reason << '\n';
            continue;
        }

        std::cout << " order_id=" << result.order_id
                  << " open_qty=" << result.open_quantity << '\n';

        print_trades(result.trades);
        print_book(result.book);
    }

    return 0;
}
