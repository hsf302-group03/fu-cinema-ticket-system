package com.fu.cinema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")          // dùng H2 (src/test/resources/application-test.properties)
class FuCinemaTicketSystemApplicationTests {

    @Test
    void contextLoads() {
    }
}
