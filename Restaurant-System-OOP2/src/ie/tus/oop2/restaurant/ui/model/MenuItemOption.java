package ie.tus.oop2.restaurant.ui.model;

import java.math.BigDecimal;

public record MenuItemOption(long menuItemId, String name, BigDecimal price) {

    @Override
    public String toString() {
        return name + " (€" + price + ")";
    }
}