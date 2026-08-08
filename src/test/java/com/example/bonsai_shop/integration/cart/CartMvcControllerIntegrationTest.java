package com.example.bonsai_shop.integration.cart;

import com.example.bonsai_shop.integration.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class CartMvcControllerIntegrationTest extends BaseControllerIntegrationTest {

    @DisplayName("TC-IT-CART-01: Parameterized Test rendering HTML views Cart, Checkout, Success & Lookup")
    @ParameterizedTest
    @ValueSource(strings = {"/cart", "/checkout", "/order/success?orderCode=ORD-TEST", "/lookup"})
    void testCartMvcViewsRendering(String url) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(url))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
