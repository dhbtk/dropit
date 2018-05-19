package dropit

import org.junit.runner.RunWith
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [AbstractIntegrationTest.TestConfiguration::class])
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {
    @Configuration
    @ComponentScan(basePackages = ["dropit"])
    class TestConfiguration
}