package com.github.msink.tools

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.DefaultRenderer
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.renderers.isImage
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer

class GfmPlugin : DokkaPlugin() {

    val gfmPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val renderer by extending {
        (CoreExtensions.renderer
                providing { MarkdownRenderer(it) }
                override dokkaBase.htmlRenderer)
    }

    val locationProvider by extending {
        (dokkaBase.locationProviderFactory
                providing { MarkdownLocationProviderFactory(it) }
                override dokkaBase.locationProvider)
    }

    val rootCreator by extending {
        gfmPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        (gfmPreprocessors
                providing { PackageListCreator(it, RecognizedLinkFormat.DokkaGFM) }
                order { after(rootCreator) })
    }
}

open class MarkdownBuilder {
    val stringBuilder = StringBuilder()

    open fun append(content: String): Unit = with(stringBuilder) {
        append(content)
    }

    open fun build(): String = stringBuilder.toString()
}

inline fun buildMarkdown(block: MarkdownBuilder.() -> Unit): String {
    return MarkdownBuilder().apply(block).build()
}

open class MarkdownRenderer(
    context: DokkaContext
) : DefaultRenderer<MarkdownBuilder>(context) {

    override val preprocessors = context.plugin<GfmPlugin>().query { gfmPreprocessors }

    override fun MarkdownBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: MarkdownBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) -> {
                childrenCallback()
                buildNewLine()
            }
            node.hasStyle(TextStyle.Paragraph) -> {
                buildParagraph()
                childrenCallback()
                buildParagraph()
            }
            else -> childrenCallback()
        }
    }

    override fun MarkdownBuilder.buildHeader(level: Int, node: ContentHeader, content: MarkdownBuilder.() -> Unit) {
        buildParagraph()
        append("#".repeat(level) + " ")
        content()
        buildNewLine()
    }

    override fun MarkdownBuilder.buildLink(address: String, content: MarkdownBuilder.() -> Unit) {
        append("[")
        content()
        append("]($address)")
    }

    override fun MarkdownBuilder.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildListLevel(node, pageContext)
    }

    private fun MarkdownBuilder.buildListItem(items: List<ContentNode>, pageContext: ContentPage) {
        items.forEach {
            if (it is ContentList) {
                buildList(it, pageContext)
            } else {
                append("<li>")
                append(buildMarkdown { it.build(this, pageContext, it.sourceSets) }.trim())
                append("</li>")
            }
        }
    }

    private fun MarkdownBuilder.buildListLevel(node: ContentList, pageContext: ContentPage) {
        if (node.ordered) {
            append("<ol>")
            buildListItem(node.children, pageContext)
            append("</ol>")
        } else {
            append("<ul>")
            buildListItem(node.children, pageContext)
            append("</ul>")
        }
    }

    override fun MarkdownBuilder.buildNewLine() {
        append("  \n")
    }

    private fun MarkdownBuilder.buildParagraph() {
        append("\n\n")
    }

    override fun MarkdownBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun MarkdownBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DisplaySourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext, sourceSets)
        } else {
            val distinct = sourceSets.map {
                it to buildMarkdown { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DisplaySourceSet, String>::second, Pair<DisplaySourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                append(" ")
                buildSourceSetTags(platforms.toSet())
                append(" $text ")
                buildNewLine()
            }
        }
    }

    override fun MarkdownBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        if(node.isImage()){
            append("!")
        }
        append("[${node.altText}](${node.address})")
    }

    override fun MarkdownBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildNewLine()
        if (node.dci.kind == ContentKind.Sample || node.dci.kind == ContentKind.Parameters) {
            node.sourceSets.forEach { sourcesetData ->
                append(sourcesetData.name)
                buildNewLine()
                buildTable(
                    node.copy(
                        children = node.children.filter { it.sourceSets.contains(sourcesetData) },
                        dci = node.dci.copy(kind = ContentKind.Main)
                    ), pageContext, sourceSetRestriction
                )
                buildNewLine()
            }
        } else {
            val size = node.header.size

            if (node.header.isNotEmpty()) {
                append("| ")
                node.header.forEach {
                    it.children.forEach {
                        append(" ")
                        it.build(this, pageContext, it.sourceSets)
                    }
                    append("| ")
                }
                append("\n")
            } else {
                append("| ".repeat(size))
                if (size > 0) append("|\n")
            }

            append("|---".repeat(size))
            if (size > 0) append("|\n")

            node.children.filterNot {
                val dri = it.dci.dri.first()
                dri.packageName == "kotlin" && dri.classNames == "Any"
            }.forEach {
                val builder = MarkdownBuilder()
                it.children.forEach {
                    builder.append("| ")
                    builder.append("<a name=\"${it.dci.dri.first()}\"></a>")
                    builder.append(
                        buildMarkdown { it.build(this, pageContext) }.replace(
                            Regex("#+ "),
                            ""
                        )
                    )  // Workaround for headers inside tables
                }
                append(builder.build().withEntersAsHtml())
                append(" | ".repeat(size - it.children.size))
                append("\n")
            }
        }
    }

    override fun MarkdownBuilder.buildText(textNode: ContentText) {
        if (textNode.text.isNotBlank()) {
            val decorators = decorators(textNode.style)
            append(textNode.text.takeWhile { it == ' ' })
            append(decorators)
            append(textNode.text.trim())
            append(decorators.reversed())
            append(textNode.text.takeLastWhile { it == ' ' })
        }
    }

    override fun MarkdownBuilder.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).asReversed().forEach { node ->
            append("/")
            if (node.isNavigable) buildLink(node, page)
            else append(node.name)
        }
        buildParagraph()
    }

    override fun buildPage(page: ContentPage, content: (MarkdownBuilder, ContentPage) -> Unit): String =
        buildMarkdown {
            content(this, page)
        }

    override fun buildError(node: ContentNode) {
        context.logger.warn("Markdown renderer has encountered problem. The unmatched node is $node")
    }

    override fun MarkdownBuilder.buildDivergent(node: ContentDivergentGroup, pageContext: ContentPage) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, contentPage, sourceSet ->
                instance.before?.let { before ->
                    buildMarkdown { buildContentNode(before, pageContext, sourceSet) }
                } ?: ""
            }, { instance, contentPage, sourceSet ->
                instance.after?.let { after ->
                    buildMarkdown { buildContentNode(after, pageContext, sourceSet) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

            buildSourceSetTags(sourceSets)
            buildNewLine()
            instance.before?.let {
                append("Brief description")
                buildNewLine()
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

            append("Content")
            buildNewLine()
            entry.groupBy { buildMarkdown { buildContentNode(it.first.divergent, pageContext, setOf(it.second)) } }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if (sourceSets.size > 1) {
                        buildSourceSetTags(innerSourceSets)
                        buildNewLine()
                    }
                    innerInstance.divergent.build(
                        this@buildDivergent,
                        pageContext,
                        setOf(innerSourceSets.first())
                    ) // It's workaround to render content only once
                    buildNewLine()
                }

            instance.after?.let {
                append("More info")
                buildNewLine()
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

            buildParagraph()
        }
    }

    private fun decorators(styles: Set<Style>) = buildString {
        styles.forEach {
            when (it) {
                TextStyle.Bold -> append("**")
                TextStyle.Italic -> append("*")
                TextStyle.Strong -> append("**")
                TextStyle.Strikethrough -> append("~~")
                else -> Unit
            }
        }
    }

    private val PageNode.isNavigable: Boolean
        get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

    private fun MarkdownBuilder.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)!!) {
            append(to.name)
        }

    override suspend fun renderPage(page: PageNode) {
        val path by lazy {
            locationProvider.resolve(page, skipExtension = true)
                ?: throw DokkaException("Cannot resolve path for ${page.name}")
        }

        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".md")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".md")
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }

    private fun String.withEntersAsHtml(): String = replace("\n", "<br>")

    private fun List<Pair<ContentDivergentInstance, DisplaySourceSet>>.getInstanceAndSourceSets() =
        this.let { Pair(it.first().first, it.map { it.second }.toSet()) }

    private fun StringBuilder.buildSourceSetTags(sourceSets: Set<DisplaySourceSet>) =
        append(sourceSets.joinToString(prefix = "[", postfix = "]") { it.name })
}

class MarkdownLocationProviderFactory(val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: RootPageNode) = MarkdownLocationProvider(pageNode, context)
}

class MarkdownLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DokkaLocationProvider(pageGraphRoot, dokkaContext, ".md") {

    override val PAGE_WITH_CHILDREN_SUFFIX = "README"

    override fun pathTo(node: PageNode, context: PageNode?): String {
        return super.pathTo(node, context).removePrefix("libui/")
    }

    override fun ancestors(node: PageNode): List<PageNode> {
        return generateSequence(node) { it.parent() }
            .filterNot { it is RendererSpecificPage }
            .filterNot { it is ModulePageNode }
            .toList()
    }

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]
}
