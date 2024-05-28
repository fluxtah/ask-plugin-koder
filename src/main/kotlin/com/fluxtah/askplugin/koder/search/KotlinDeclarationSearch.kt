package com.fluxtah.askplugin.koder.search

import com.fluxtah.askplugin.koder.kotlin.KotlinFileRepository
import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import kotlinx.serialization.Serializable
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.store.Directory
import org.apache.lucene.store.RAMDirectory
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File

class KotlinDeclarationSearch {
    private val analyzer = StandardAnalyzer()
    private val index: Directory = RAMDirectory()
    private val config = IndexWriterConfig(analyzer)
    private val includeExtensions = setOf("kt", "kts")
    fun search(searchText: String): List<KotlinDeclaration> {
        buildIndex()

        val term = Term("name", searchText.lowercase())
        val query: Query = FuzzyQuery(term, 2)
        val reader = DirectoryReader.open(index)
        val searcher = IndexSearcher(reader)
        val docs = searcher.search(query, 10)
        val hits: Array<ScoreDoc> = docs.scoreDocs

        val results = mutableListOf<KotlinDeclaration>()
        // Display results
        for (i in hits.indices) {
            val docId = hits[i].doc
            val d: Document = searcher.doc(docId)
            results.add(
                KotlinDeclaration(
                    fqPath = d.get("id"),
                    name = d.get("name"),
                    type = d.get("type"),
                    startOffset = d.get("startOffset").toInt(),
                    endOffset = d.get("endOffset").toInt()
                )
            )
           // println("Found: ${d.get("id")} ${d.get("name")} [${d.get("type")}] at ${d.get("startOffset")}-${d.get("endOffset")}")
        }
        reader.close()

        return results
    }

    private fun buildIndex() {
        IndexWriter(index, config).use { writer ->
            val kotlinFileRepository = KotlinFileRepository()
            val currentWorkingDir = File(getCurrentWorkingDirectory())
            currentWorkingDir.walk()
                .filter {
                    !it.path.contains("build/")
                }
                .filter { it.isFile && !it.absolutePath.contains("/.") && includeExtensions.contains(it.extension) }
                .forEach { file ->
                    val relativePath = currentWorkingDir.toPath().relativize(file.toPath()).toString()
                    val ktFile = kotlinFileRepository.parseFileOnce(file.path)
                    val declarations = ktFile!!.declarations
                    addDeclarations(relativePath, writer, declarations)
                }
        }
    }

    private fun addDeclarations(fqnPath: String, writer: IndexWriter, declarations: List<KtDeclaration>) {
        for (decl in declarations) {
            val name = decl.name ?: ""
            if (decl.name == null) {
                continue
            }
            val fqn = "$fqnPath#${name}"
            val kotlinDeclaration = KotlinDeclaration(
                fqPath = fqn,
                name = decl.name ?: "",
                type = decl::class.simpleName!!,
                startOffset = decl.startOffset,
                endOffset = decl.endOffset
            )

            addDoc(writer, kotlinDeclaration)

            if (decl is KtClassOrObject) {
                val classDeclarations = decl.declarations
                addDeclarations(fqn, writer, classDeclarations)
            }

        }
    }

    private fun addDoc(writer: IndexWriter, kotlinDeclaration: KotlinDeclaration) {
        val doc = Document()
        doc.add(TextField("id", kotlinDeclaration.fqPath, Field.Store.YES))
        doc.add(TextField("name", kotlinDeclaration.name, Field.Store.YES))
        doc.add(StringField("type", kotlinDeclaration.type, Field.Store.YES))
        doc.add(StringField("startOffset", kotlinDeclaration.startOffset.toString(), Field.Store.YES))
        doc.add(StringField("endOffset", kotlinDeclaration.endOffset.toString(), Field.Store.YES))
        writer.updateDocument(Term("id", kotlinDeclaration.fqPath), doc) // Replace with a suitable unique term
    }
}

@Serializable
data class KotlinDeclaration(
    val fqPath: String,
    val name: String,
    val type: String,
    val startOffset: Int,
    val endOffset: Int
)