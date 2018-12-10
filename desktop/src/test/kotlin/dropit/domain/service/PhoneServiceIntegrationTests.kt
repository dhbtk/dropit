//package dropit.domain.service
//
//import dropit.AbstractIntegrationTest
//import dropit.application.dto.TokenRequest
//import dropit.application.dto.TokenStatus
//import org.junit.Assert
//import org.junit.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.test.context.jdbc.Sql
//import java.util.*
//
//class PhoneServiceIntegrationTests : AbstractIntegrationTest() {
//    @Autowired
//    lateinit var phoneService: PhoneService
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it creates a token for a given request and it is in "pending" status`() {
//        val request = TokenRequest(UUID.randomUUID().toString(), "test phone")
//        val token = phoneService.requestToken(request)
//        Assert.assertNotNull(token)
//        val status = phoneService.getTokenStatus(token)
//        Assert.assertEquals(TokenStatus.PENDING, status)
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it throws an exception on an unknown token`() {
//        try {
//            val status = phoneService.getTokenStatus("asdf")
//            Assert.fail()
//        } catch (e: UnauthorizedException) {
//            // we're good
//        }
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it should create a phone and approve it`() {
//        val request = TokenRequest(UUID.randomUUID().toString(), "test phone")
//        val token = phoneService.requestToken(request)
//        Assert.assertNotNull(token)
//        val status = phoneService.getTokenStatus(token)
//        Assert.assertEquals(TokenStatus.PENDING, status)
//        val phone = phoneService.authorizePhone(UUID.fromString(request.id))
//        Assert.assertEquals(TokenStatus.AUTHORIZED, phone.status)
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it should not authorize a phone that does not exist`() {
//        try {
//            phoneService.authorizePhone(UUID.randomUUID())
//            Assert.fail()
//        } catch(e: RuntimeException) {
//            // ok
//        }
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it should create a phone and deny it`() {
//        val request = TokenRequest(UUID.randomUUID().toString(), "test phone")
//        val token = phoneService.requestToken(request)
//        Assert.assertNotNull(token)
//        val status = phoneService.getTokenStatus(token)
//        Assert.assertEquals(TokenStatus.PENDING, status)
//        val phone = phoneService.denyPhone(UUID.fromString(request.id))
//        Assert.assertEquals(TokenStatus.DENIED, phone.status)
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql")
//    fun `it should not deauthorize a phone that does not exist`() {
//        try {
//            phoneService.denyPhone(UUID.randomUUID())
//            Assert.fail()
//        } catch(e: RuntimeException) {
//            // ok
//        }
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql", "/dataset/phone.sql")
//    fun `it should list phones, correctly filtering by authorization status`() {
//        val nonDenied = phoneService.listPhones(false)
//        Assert.assertEquals(0, nonDenied.filter { it.status == TokenStatus.DENIED }.size)
//        val denied = phoneService.listPhones(true)
//        Assert.assertNotEquals(0, denied.filter { it.status == TokenStatus.DENIED }.size)
//    }
//
//    @Test
//    @Sql("/dataset/clear.sql", "/dataset/phone.sql")
//    fun `it should delete a phone successfully`() {
//        val phones = phoneService.listPhones(false)
//        phoneService.deletePhone(phones[0].id!!)
//        Assert.assertEquals(phones.size - 1, phoneService.listPhones(false).size)
//    }
//}