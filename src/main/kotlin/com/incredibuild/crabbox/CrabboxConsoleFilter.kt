package com.incredibuild.crabbox

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project

class CrabboxConsoleFilter(
    private val project: Project,
) : Filter {
    private val urlPattern = Regex("""https?://[^\s)]+""")
    private val runPattern = Regex("""\brun_[A-Za-z0-9_-]+\b""")
    private val leasePattern = Regex("""\bcbx_[0-9a-fA-F]{12}\b""")

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val lineStartOffset = entireLength - line.length
        val items = mutableListOf<Filter.ResultItem>()

        urlPattern.findAll(line).forEach { match ->
            items += Filter.ResultItem(
                lineStartOffset + match.range.first,
                lineStartOffset + match.range.last + 1,
                BrowserHyperlinkInfo(match.value),
            )
        }

        runPattern.findAll(line).forEach { match ->
            items += Filter.ResultItem(
                lineStartOffset + match.range.first,
                lineStartOffset + match.range.last + 1,
                CrabboxCommandHyperlinkInfo(
                    project = project,
                    title = "Crabbox logs ${match.value}",
                    args = listOf("logs", match.value),
                ),
            )
        }

        leasePattern.findAll(line).forEach { match ->
            items += Filter.ResultItem(
                lineStartOffset + match.range.first,
                lineStartOffset + match.range.last + 1,
                CrabboxCommandHyperlinkInfo(
                    project = project,
                    title = "Crabbox status ${match.value}",
                    args = listOf("status", match.value),
                ),
            )
        }

        return if (items.isEmpty()) null else Filter.Result(items)
    }
}

private class BrowserHyperlinkInfo(
    private val url: String,
) : HyperlinkInfo {
    override fun navigate(project: Project) {
        BrowserUtil.browse(url)
    }
}

private class CrabboxCommandHyperlinkInfo(
    private val project: Project,
    private val title: String,
    private val args: List<String>,
) : HyperlinkInfo {
    override fun navigate(project: Project) {
        CrabboxTaskRunner.runSimple(project, title, args)
    }
}
