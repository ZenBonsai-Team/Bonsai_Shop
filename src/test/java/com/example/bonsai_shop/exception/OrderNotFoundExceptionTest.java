package com.example.bonsai_shop.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderNotFoundExceptionTest {

    @Test
    void testOrderNotFoundException() {
        String msg = "Test Order Not Found Exception Message";
        OrderNotFoundException exception = new OrderNotFoundException(msg);
        
        assertNotNull(exception);
        assertEquals(msg, exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }
}
