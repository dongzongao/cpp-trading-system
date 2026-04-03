#include <cstdlib>
#include <iostream>
#include <string_view>

#include "trading/trading_system.hpp"

namespace {

void assert_true(bool condition, std::string_view message) {
    if (!condition) {
        std::cerr << "Assertion failed: " << message << '\n';
        std::exit(1);
    }
}

void should_reject_unknown_symbol() {
    trading::TradingSystem system({"BTC-USD"}, 100, 2'000'000.0);
    const auto result = system.submit({"ETH-USD", trading::Side::Buy, 3000.0, 1});

    assert_true(!result.accepted, "unknown symbol should be rejected");
    assert_true(result.rejection_reason == "symbol not allowed", "rejection reason should explain risk failure");
}

void should_match_at_resting_order_price() {
    trading::TradingSystem system({"BTC-USD"}, 100, 2'000'000.0);

    const auto passive = system.submit({"BTC-USD", trading::Side::Sell, 100.0, 10});
    const auto aggressive = system.submit({"BTC-USD", trading::Side::Buy, 101.0, 4});

    assert_true(passive.accepted, "passive order should be accepted");
    assert_true(aggressive.accepted, "aggressive order should be accepted");
    assert_true(aggressive.trades.size() == 1, "buy order should generate one trade");
    assert_true(aggressive.trades.front().price == 100.0, "trade should execute at resting ask price");
    assert_true(aggressive.trades.front().quantity == 4, "trade should execute partial quantity");

    const auto top = system.top_of_book("BTC-USD");
    assert_true(top.best_ask.has_value(), "best ask should remain after partial fill");
    assert_true(*top.best_ask == 100.0, "remaining ask should stay at same price");
    assert_true(top.best_ask_quantity == 6, "remaining ask quantity should be updated");
}

void should_preserve_price_time_priority() {
    trading::TradingSystem system({"BTC-USD"}, 100, 2'000'000.0);

    const auto first_buy = system.submit({"BTC-USD", trading::Side::Buy, 100.0, 5});
    const auto second_buy = system.submit({"BTC-USD", trading::Side::Buy, 100.0, 7});
    const auto sell = system.submit({"BTC-USD", trading::Side::Sell, 99.0, 8});

    assert_true(first_buy.accepted && second_buy.accepted && sell.accepted, "orders should be accepted");
    assert_true(sell.trades.size() == 2, "sell order should match both resting bids");
    assert_true(sell.trades[0].buy_order_id == first_buy.order_id, "older bid should match first");
    assert_true(sell.trades[0].quantity == 5, "older order should be fully matched first");
    assert_true(sell.trades[1].buy_order_id == second_buy.order_id, "newer bid should match second");
    assert_true(sell.trades[1].quantity == 3, "remaining quantity should match second order");
}

}  // namespace

int main() {
    should_reject_unknown_symbol();
    should_match_at_resting_order_price();
    should_preserve_price_time_priority();
    std::cout << "All trading system tests passed.\n";
    return 0;
}
