package com.example.pdf

data class PageTextChunk(
    val pageNumber: Int,
    val text: String
)

object TextChunker {
    /**
     * Splits extracted text into manageable chunks with page references.
     * Keeps chunk sizes around 1000-2500 characters so Gemini can generate high quality MCQs
     * without exceeding token constraints.
     */
    fun createChunks(pageTexts: List<PageTextChunk>, maxChunkLength: Int = 2000): List<PageTextChunk> {
        val result = mutableListOf<PageTextChunk>()

        for (page in pageTexts) {
            val trimmed = page.text.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.length <= maxChunkLength) {
                result.add(page)
            } else {
                // Split long page by paragraphs or sentences
                val paragraphs = trimmed.split("\n\n")
                var currentBlock = StringBuilder()

                for (p in paragraphs) {
                    if (currentBlock.length + p.length > maxChunkLength && currentBlock.isNotEmpty()) {
                        result.add(PageTextChunk(page.pageNumber, currentBlock.toString().trim()))
                        currentBlock = StringBuilder()
                    }
                    if (currentBlock.isNotEmpty()) currentBlock.append("\n\n")
                    currentBlock.append(p)
                }

                if (currentBlock.isNotEmpty()) {
                    result.add(PageTextChunk(page.pageNumber, currentBlock.toString().trim()))
                }
            }
        }

        return result
    }
}
