package me.rerere.rikkahub.ui.components.richtext

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import me.rerere.rikkahub.ui.components.table.ColumnDefinition
import me.rerere.rikkahub.ui.components.table.ColumnWidth
import me.rerere.rikkahub.ui.components.table.DataTable
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

private val flavour by lazy {
    GFMFlavourDescriptor()
}

private val parser by lazy {
    MarkdownParser(flavour)
}

private val INLINE_LATEX_REGEX = Regex("\\\\\\((.+?)\\\\\\)")
private val BLOCK_LATEX_REGEX = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
private val CITATION_REGEX = Regex("\\[citation:(\\w+)\\]")
val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)
// 预处理markdown内容
private fun preProcess(content: String): String {
    // 替换行内公式 \( ... \) 到 $ ... $
    var result = content.replace(INLINE_LATEX_REGEX) { matchResult ->
        "$" + matchResult.groupValues[1] + "$"
    }

    // 替换块级公式 \[ ... \] 到 $$ ... $$
    result =
        result.replace(BLOCK_LATEX_REGEX) { matchResult ->
            "$$" + matchResult.groupValues[1] + "$$"
        }

    // 替换引用 [citation:xx] 为 <citation>xx</citation>
    result = result.replace(CITATION_REGEX) { matchResult ->
        "<citation>${matchResult.groupValues[1]}</citation>"
    }

    // 替换思考
    result = result.replace(THINKING_REGEX) { matchResult ->
        matchResult.groupValues[1].lines().filter { it.isNotBlank() }.joinToString("\n") { ">$it" }
    }

    return result
}

@Preview(showBackground = true)
@Composable
private fun MarkdownPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        MarkdownBlock(
            content = """
                # Hello World
                
                | A | B |
                | - | - |
                | 1 | 2 |
                
                | Name | Age | Address | Email | Job | Homepage |
                | ---- | --- | ------- | ----- | --- | -------- |
                | John | 25  | New York | john@example.com | Software Engineer | john.com |
                | Jane | 26  | London   | jane@example.com | Data Scientist | jane.com |
                
            """.trimIndent()
        )
    }
}

@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val preprocessed = remember(content) { preProcess(content) }
    val astTree = remember(preprocessed) {
        parser.buildMarkdownTreeFromString(preprocessed)
            .also {
                dumpAst(it, preprocessed) // for debugging ast tree
            }
    }

    ProvideTextStyle(style) {
        MarkdownAst(astTree, preprocessed, modifier)
    }
}

// for debug
private fun dumpAst(node: ASTNode, text: String, indent: String = "") {
    println("$indent${node.type} ${if (node.children.isEmpty()) node.getTextInNode(text) else ""}")
    node.children.fastForEach {
        dumpAst(it, text, "$indent  ")
    }
}

@Composable
private fun MarkdownAst(astNode: ASTNode, content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        astNode.children.fastForEach { child ->
            MarkdownNode(child, content)
        }
    }
}

object HeaderStyle {
    val H1 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )

    val H2 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )

    val H3 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    val H4 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )

    val H5 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    val H6 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
    when (node.type) {
        // 文件根节点
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.fastForEach { child ->
                MarkdownNode(child, content, modifier)
            }
        }

        // 段落
        MarkdownElementTypes.PARAGRAPH -> {
            Paragraph(
                node = node,
                content = content,
                modifier = modifier
            )
        }

        // 标题
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6 -> {
            val style = when (node.type) {
                MarkdownElementTypes.ATX_1 -> HeaderStyle.H1
                MarkdownElementTypes.ATX_2 -> HeaderStyle.H2
                MarkdownElementTypes.ATX_3 -> HeaderStyle.H3
                MarkdownElementTypes.ATX_4 -> HeaderStyle.H4
                MarkdownElementTypes.ATX_5 -> HeaderStyle.H5
                MarkdownElementTypes.ATX_6 -> HeaderStyle.H6
                else -> throw IllegalArgumentException("Unknown header type")
            }
            ProvideTextStyle(style) {
                FlowRow(modifier = modifier.padding(vertical = 8.dp)) {
                    node.children.forEach { child ->
                        MarkdownNode(child, content, Modifier.align(Alignment.CenterVertically))
                    }
                }
            }
        }

        // 列表
        MarkdownElementTypes.UNORDERED_LIST -> {
            Column(
                modifier = modifier.padding(start = 4.dp)
            ) {
                node.children.fastForEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text(
                                text = "• ",
                                modifier = Modifier.alignByBaseline()
                            )
                            FlowRow {
                                child.children.fastForEach { listItemChild ->
                                    MarkdownNode(listItemChild, content)
                                }
                            }
                        }
                    }
                }
            }
        }

        MarkdownElementTypes.ORDERED_LIST -> {
            Column(
                modifier = modifier.padding(start = 4.dp)
            ) {
                var index = 1
                node.children.fastForEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text(
                                text = child.findChildOfType(MarkdownTokenTypes.LIST_NUMBER)
                                    ?.getTextInNode(content) ?: "-",
                            )
                            FlowRow {
                                child.children.fastForEach { listItemChild ->
                                    MarkdownNode(
                                        listItemChild,
                                        content
                                    )
                                }
                            }
                        }
                        index++
                    }
                }
            }
        }

        // 引用块
        MarkdownElementTypes.BLOCK_QUOTE -> {
            ProvideTextStyle(LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
                val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                FlowRow(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = bgColor,
                                size = size
                            )
                            drawRect(
                                color = borderColor,
                                size = Size(10f, size.height)
                            )
                        }
                        .padding(8.dp)
                ) {
                    node.children.fastForEach { child ->
                        MarkdownNode(child, content)
                    }
                }
            }
        }

        // 链接
        MarkdownElementTypes.INLINE_LINK -> {
            val linkText =
                node.findChildOfType(MarkdownTokenTypes.TEXT)?.getTextInNode(content) ?: ""
            val linkDest =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            val context = LocalContext.current
            Text(
                text = linkText,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, linkDest.toUri())
                    context.startActivity(intent)
                }
            )
        }

        // 加粗和斜体
        MarkdownElementTypes.EMPH -> {
            ProvideTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
                node.children.fastForEach { child ->
                    MarkdownNode(child, content, modifier)
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
                node.children.fastForEach { child ->
                    MarkdownNode(child, content, modifier)
                }
            }
        }

        // GFM 特殊元素
        GFMElementTypes.STRIKETHROUGH -> {
            Text(
                text = node.getTextInNode(content),
                textDecoration = TextDecoration.LineThrough,
                modifier = modifier
            )
        }

        GFMElementTypes.TABLE -> {
            TableNode(node, content, modifier)
        }

        // 图片
        MarkdownElementTypes.IMAGE -> {
            val altText =
                node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: ""
            val imageUrl =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 这里可以使用Coil等图片加载库加载图片
                AsyncImage(model = imageUrl, contentDescription = altText)
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            val formula = node.getTextInNode(content)
            MathInline(
                formula,
                modifier = modifier
                    .padding(horizontal = 1.dp)
            )
        }

        GFMElementTypes.BLOCK_MATH -> {
            val formula = node.getTextInNode(content)
            MathBlock(
                formula, modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                modifier = modifier
            )
        }

        MarkdownElementTypes.CODE_BLOCK -> {
            val code = node.getTextInNode(content)
            HighlightCodeBlock(
                code = code,
                language = "plaintext",
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth(),
                completeCodeBlock = true
            )
        }

        // 代码块
        MarkdownElementTypes.CODE_FENCE -> {
            val code = node.getTextInNode(content, MarkdownTokenTypes.CODE_FENCE_CONTENT)
            val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
                ?.getTextInNode(content)
                ?: "plaintext"
            val hasEnd = node.findChildOfType(MarkdownTokenTypes.CODE_FENCE_END) != null

            HighlightCodeBlock(
                code = code,
                language = language,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth(),
                completeCodeBlock = hasEnd
            )
        }

        MarkdownTokenTypes.TEXT, MarkdownTokenTypes.WHITE_SPACE -> {
            val text = node.getTextInNode(content)
            Text(
                text = text,
                modifier = modifier
            )
        }

        MarkdownTokenTypes.EOL -> {
            Spacer(Modifier.fillMaxWidth())
        }

        // 其他类型的节点，递归处理子节点
        else -> {
            // 递归处理其他节点的子节点
            node.children.fastForEach { child ->
                MarkdownNode(child, content, modifier)
            }
        }
    }
}

@Composable
private fun Paragraph(node: ASTNode, content: String, modifier: Modifier) {
    // 如果段落中包含块级数学公式，则直接渲染所有子节点，不使用AnnotatedString
//    if (node.findChildOfType(GFMElementTypes.BLOCK_MATH) != null) {
//        node.children.forEach {
//            MarkdownNode(it, content, modifier)
//        }
//        return
//    }

    // dumpAst(node, content)

    val colorScheme = MaterialTheme.colorScheme
    val inlineContents = remember {
        mutableStateMapOf<String, InlineTextContent>()
    }

    BoxWithConstraints {
        val maxWidth = this.maxWidth
        val annotatedString = remember(content) {
            buildAnnotatedString {
                node.children.fastForEach { child ->
                    appendMarkdownNodeContent(child, content, inlineContents, colorScheme, maxWidth)
                }
            }
        }
        Text(
            text = annotatedString,
            modifier = modifier
                .padding(start = 4.dp),
            style = LocalTextStyle.current,
            inlineContent = inlineContents,
            softWrap = true,
            overflow = TextOverflow.Visible
        )
    }
}

@Composable
private fun TableNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
    // 提取表格的标题行和数据行
    val headerNode = node.children.find { it.type == GFMElementTypes.HEADER }
    val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }

    // 计算列数（从标题行获取）
    val columnCount = headerNode?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0

    // 检查是否有足够的列来显示表格
    if (columnCount == 0) return

    // 提取表头单元格文本
    val headerCells = headerNode?.children
        ?.filter { it.type == GFMTokenTypes.CELL }
        ?.map { it.getTextInNode(content).trim() }
        ?: emptyList()

    // 提取所有行的数据
    val rows = rowNodes.map { rowNode ->
        rowNode.children
            .filter { it.type == GFMTokenTypes.CELL }
            .map { it.getTextInNode(content).trim() }
    }

    // 创建列定义
    val columns = List(columnCount) { columnIndex ->
        ColumnDefinition<List<String>>(
            header = { 
                Text(
                    text = if (columnIndex < headerCells.size) headerCells[columnIndex] else "",
                    fontWeight = FontWeight.Bold
                )
            },
            cell = { rowData ->
                Text(
                    text = if (columnIndex < rowData.size) rowData[columnIndex] else "",
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            },
            width = ColumnWidth.Adaptive(min = 80.dp)
        )
    }

    // 渲染表格
    DataTable(
        columns = columns,
        data = rows,
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()

    )
}

private fun AnnotatedString.Builder.appendMarkdownNodeContent(
    node: ASTNode,
    content: String,
    inlineContents: MutableMap<String, InlineTextContent>,
    colorScheme: ColorScheme,
    maxWidth: Dp
) {
    when (node.type) {
        MarkdownTokenTypes.TEXT,
        MarkdownTokenTypes.LPAREN,
        MarkdownTokenTypes.RPAREN,
        MarkdownTokenTypes.WHITE_SPACE,
        MarkdownTokenTypes.COLON -> {
            append(node.getTextInNode(content))
        }

        MarkdownTokenTypes.EMPH -> {
            val text = node.getTextInNode(content)
            if (text != "*") append(text)
        }

        MarkdownTokenTypes.HTML_TAG -> {
            val text = node.getTextInNode(content)
            if (text == "<citation>") {
                val id = node.nextSibling()?.getTextInNode(content)
                if (id != null) {
                    pushStyle(
                        SpanStyle(
                            background = colorScheme.secondaryContainer,
                            fontSize = 0.85.em
                        )
                    )
                    append(" ")
                }
            } else if (text == "</citation>") {
                append(" ")
                pop()
                append(" ")
            } else {
                append(text)
            }
        }

        MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.fastForEach {
                    appendMarkdownNodeContent(
                        it,
                        content,
                        inlineContents,
                        colorScheme,
                        maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.fastForEach {
                    appendMarkdownNodeContent(
                        it,
                        content,
                        inlineContents,
                        colorScheme,
                        maxWidth
                    )
                }
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkText =
                node.findChildOfType(MarkdownTokenTypes.TEXT)?.getTextInNode(content) ?: ""
            val linkDest =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            withLink(LinkAnnotation.Url(linkDest)) {
                withStyle(
                    SpanStyle(
                        color = colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(linkText)
                }
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 0.95.em,
                )
            ) {
                append(code)
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            // formula as id
            val formula = node.getTextInNode(content)
            appendInlineContent(formula, "[Latex]")
            inlineContents.putIfAbsent(
                formula, InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        val density = LocalDensity.current
                        MathInline(
                            formula,
                            modifier = Modifier
                                .onGloballyPositioned { coord ->
                                    val width = coord.size.width
                                    val height = coord.size.height
                                    with(density) {
                                        val widthInSp = width.toDp().toSp()
                                        val heightInSp = (height.toDp() + 4.dp).toSp()
                                        val inlineContent = inlineContents[formula]
                                        if (inlineContent != null && inlineContent.placeholder.width != widthInSp) {
                                            inlineContents[formula] = InlineTextContent(
                                                placeholder = Placeholder(
                                                    width = widthInSp,
                                                    height = heightInSp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                ),
                                                children = inlineContent.children
                                            )
                                        }
                                    }
                                }
                        )
                    }
                ))
        }

        GFMElementTypes.BLOCK_MATH -> {
            // formula as id
            val formula = node.getTextInNode(content)
            appendInlineContent(formula, "[Latex]")
            inlineContents.putIfAbsent(
                formula, InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        val density = LocalDensity.current
                        MathBlock(
                            formula,
                            modifier = Modifier
                                .width(maxWidth)
                                .onGloballyPositioned { coord ->
                                    val height = coord.size.height
                                    with(density) {
                                        val widthInSp = maxWidth.toSp()
                                        val heightInSp = (height.toDp() + 24.dp).toSp()
                                        val inlineContent = inlineContents[formula]
                                        if (inlineContent != null && inlineContent.placeholder.width != widthInSp) {
                                            inlineContents[formula] = InlineTextContent(
                                                placeholder = Placeholder(
                                                    width = widthInSp,
                                                    height = heightInSp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                ),
                                                children = inlineContent.children
                                            )
                                        }
                                    }
                                }
                        )
                    }
                ))
        }

        // 其他类型继续递归处理
        else -> {
            node.children.fastForEach {
                appendMarkdownNodeContent(
                    it,
                    content,
                    inlineContents,
                    colorScheme,
                    maxWidth
                )
            }
        }
    }
}

private fun ASTNode.getTextInNode(text: String): String {
    return text.substring(startOffset, endOffset)
}

private fun ASTNode.getTextInNode(text: String, type: IElementType): String {
    var startOffset = -1
    var endOffset = -1
    children.fastForEach {
        if (it.type == type) {
            if (startOffset == -1) {
                startOffset = it.startOffset
            }
            endOffset = it.endOffset
        }
    }
    if (startOffset == -1 || endOffset == -1) {
        return ""
    }
    return text.substring(startOffset, endOffset)
}

private fun ASTNode.nextSibling(): ASTNode? {
    val brother = this.parent?.children ?: return null
    for (i in brother.indices) {
        if (brother[i] == this) {
            if (i + 1 < brother.size) {
                return brother[i + 1]
            }
        }
    }
    return null
}

private fun ASTNode.findChildOfType(vararg types: IElementType): ASTNode? {
    if (this.type in types) return this
    for (child in children) {
        val result = child.findChildOfType(*types)
        if (result != null) return result
    }
    return null
}

private fun ASTNode.traverseChildren(
    action: (ASTNode) -> Unit
) {
    children.fastForEach { child ->
        action(child)
        child.traverseChildren(action)
    }
}