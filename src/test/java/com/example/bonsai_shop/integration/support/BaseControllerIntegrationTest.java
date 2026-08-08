package com.example.bonsai_shop.integration.support;

import com.example.bonsai_shop.customer.service.EmailService;
import com.example.bonsai_shop.product.service.MailService;
import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional
public abstract class BaseControllerIntegrationTest extends AbstractDatabaseSafeIntegrationTest {

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @MockitoBean
    protected MailService mailService;

    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected OrderExpirationService orderExpirationService;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
}
