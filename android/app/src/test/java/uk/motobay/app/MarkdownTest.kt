package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.motobay.app.ui.settings.Block
import uk.motobay.app.ui.settings.inline
import uk.motobay.app.ui.settings.parseMarkdown

class MarkdownTest {
    @Test fun parsesTheShapesThePolicyUses() {
        val blocks = parseMarkdown(
            """
            ---
            title: x
            ---

            # Title

            Para one
            continues.

            - a
            - b
              wrapped

            | H1 | H2 |
            |---|---|
            | r1 | **v1** |
            | r2 | [link](http://x) |

            ## 2. Next
            """.trimIndent(),
        )
        assertEquals(Block.Heading(1, "Title"), blocks[0])
        assertEquals(Block.Paragraph("Para one continues."), blocks[1])
        assertEquals(Block.Bullets(listOf("a", "b wrapped")), blocks[2])
        assertEquals(Block.Table(listOf("H1", "H2"), listOf(listOf("r1", "**v1**"), listOf("r2", "[link](http://x)"))), blocks[3])
        assertEquals(Block.Heading(2, "2. Next"), blocks[4])
        assertEquals("see link here uk.motobays.app", inline("see [link](http://x) **here** `uk.motobays.app`").text)
    }
}
