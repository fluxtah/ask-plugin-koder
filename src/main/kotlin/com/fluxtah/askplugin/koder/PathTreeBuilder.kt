package com.fluxtah.askplugin.koder

class PathTreeBuilder(paths: List<String>) {
    private val root = TreeNode("root")

    init {
        paths.forEach(this::addPath)
    }

    private fun addPath(path: String) {
        var current = root
        path.split('/').forEach { part ->
            current = current.children.getOrPut(part) { TreeNode(part) }
        }
    }

    fun toTextTree(): String = buildString {
        // Start with empty string for the initial indentation since root itself is not printed
        printNode(root, "", accumulatedPath = "")
    }

    private fun StringBuilder.printNode(node: TreeNode, indent: String, accumulatedPath: String) {
        val currentPath = if (accumulatedPath.isEmpty()) node.name else "$accumulatedPath/${node.name}"

        if (node !== root && node.children.size == 1 && node.children.values.first().children.isNotEmpty()) {
            // Single child directory, accumulate path and do not print yet, just recurse
            val singleChild = node.children.values.first()
            printNode(singleChild, indent, accumulatedPath = currentPath)
        } else {
            // Print current path if applicable and not root
            if (node !== root) {
                appendLine("$indent- $currentPath/")
            }
            val newIndent = if (node === root) indent else "$indent  "  // Only add space if not root
            node.children.values.forEach { child ->
                if (child.children.isEmpty()) {
                    appendLine("$newIndent- ${child.name}")  // it's a file
                } else {
                    printNode(child, newIndent, accumulatedPath = "")
                }
            }
        }
    }
}

