//package dropit
//
//import org.junit.BeforeClass
//import org.junit.runner.RunWith
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.ComponentScan
//import org.springframework.context.annotation.Configuration
//import org.springframework.jdbc.datasource.DataSourceTransactionManager
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.context.ContextConfiguration
//import org.springframework.test.context.TestExecutionListeners
//import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
//import org.springframework.test.context.junit4.SpringRunner
//import javax.sql.DataSource
//
//@RunWith(SpringRunner::class)
//@ContextConfiguration(classes = [AbstractIntegrationTest.TestConfiguration::class])
//@ActiveProfiles("test")
//@TestExecutionListeners(
//        listeners = [SqlScriptsTestExecutionListener::class],
//        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
//)
//abstract class AbstractIntegrationTest {
//    companion object {
//        @JvmStatic
//        @BeforeClass
//        fun init() {
//            System.setProperty("java.awt.headless", "true")
//        }
//    }
//    @Configuration
//    @ComponentScan(basePackages = ["dropit"])
//    class TestConfiguration {
//        @Bean
//        fun platformTransactionManager(dataSource: DataSource) = DataSourceTransactionManager(dataSource)
//    }
//}