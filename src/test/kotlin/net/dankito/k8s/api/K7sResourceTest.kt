package net.dankito.k8s.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
class K7sResourceTest {

    @Test
    fun testHelloEndpoint() {
        given()
            .`when`().get("/k7s/resources")
            .then()
            .statusCode(200)
            .body(containsString("pod"))
    }

}