object ReportGenerator {
    fun generate(thought: StructuredThought): String {
        return buildString {
            appendLine("# Thought Report")
            appendLine("Title: ${thought.title}")
            appendLine("Category: ${thought.category}")
            appendLine()
            appendLine("Summary:")
            appendLine(thought.summary)
            appendLine()
            appendLine("Details:")
            appendLine(thought.fullText)
            appendLine()
            appendLine("Next steps:")
            appendLine("- Define goals")
            appendLine("- Identify stakeholders")
            appendLine("- Estimate timeline and budget")
            appendLine("- Set reminder in calendar")
        }
    }
}