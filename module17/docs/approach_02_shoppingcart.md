# Approach 02 - Shopping Cart (`com.revolut.buildit.shoppingcart`)

Requirements: add/update/remove lines, merged quantities, subtotal, discounts (percentage coupon, buy-N-get-1, free shipping), totals never negative.

Design
- `Line` record with `total()`; `LinkedHashMap` keeps insertion order for display.
- `Discount` is a functional interface over an immutable `Snapshot` so rules are pure and unit-testable; static factories give the three built-ins.
- `totals()` applies all discounts, caps the total discount at subtotal + shipping, and rounds once (`HALF_EVEN`).
- All mutators `synchronized`: a cart belongs to one user but may be touched from several requests.

Trade-offs: stacking vs exclusive coupons, discount ordering (apply percentage before or after item promos), tax calculation, currency per cart,
persisting the cart (event-sourced vs snapshot).

Tests: merge/update/remove, invalid quantity/price, composed discounts with exact pennies, free shipping threshold, discount cap, empty cart has no shipping.
