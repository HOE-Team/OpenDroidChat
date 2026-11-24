package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.data.ApiFunctionCallDelta
import com.hoeteam.opendroidchat.data.ApiToolCallDelta
import com.hoeteam.opendroidchat.network.ToolCallAccumulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallAccumulatorTest {

    @Test
    fun aggregatesStreamingFragments() {
        val accumulator = ToolCallAccumulator()
        accumulator.append(
            ApiToolCallDelta(
                index = 0,
                id = "call_1",
                type = "function",
                function = ApiFunctionCallDelta(
                    name = "AnySearch__batch_search",
                    arguments = "{\"query\":"
                )
            )
        )
        accumulator.append(
            ApiToolCallDelta(
                index = 0,
                function = ApiFunctionCallDelta(arguments = "\"CVE-2026\"}")
            )
        )

        val call = accumulator.build(0)

        assertEquals("call_1", call.id)
        assertEquals("function", call.type)
        assertEquals("AnySearch__batch_search", call.function.name)
        assertEquals("{\"query\":\"CVE-2026\"}", call.function.arguments)
    }

    @Test
    fun generatesIdWhenProviderOmitsIt() {
        val accumulator = ToolCallAccumulator()
        accumulator.append(
            ApiToolCallDelta(
                index = 2,
                function = ApiFunctionCallDelta(name = "extract", arguments = "{}")
            )
        )

        val call = accumulator.build(2)

        assertTrue(call.id.isNotBlank())
        assertEquals("extract", call.function.name)
    }
}
