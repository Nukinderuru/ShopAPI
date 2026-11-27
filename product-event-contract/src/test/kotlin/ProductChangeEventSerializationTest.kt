import com.nukinderuru.events.ProductChangeEvent
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductChangeEventSerializationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `event id is serialized and restored`() {
        val event = ProductChangeEvent(
            eventId = EVENT_ID,
            productId = PRODUCT_ID,
            oldPrice = BigDecimal("10.00"),
            newPrice = BigDecimal("11.00"),
            oldAvailableStock = 5,
            newAvailableStock = 4,
            occurredAt = Instant.parse("2026-11-20T12:00:00Z")
        )

        val encoded = json.encodeToString(event)
        val decoded = json.decodeFromString<ProductChangeEvent>(encoded)

        assertTrue(encoded.contains("\"eventId\":\"$EVENT_ID\""))
        assertEquals(event, decoded)
    }

    private companion object {
        val EVENT_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val PRODUCT_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    }
}