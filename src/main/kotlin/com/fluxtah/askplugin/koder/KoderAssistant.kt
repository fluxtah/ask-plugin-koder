package com.fluxtah.askplugin.koder

import com.fluxtah.askpluginsdk.AskPlugin
import com.fluxtah.askpluginsdk.AssistantDefinition
import com.fluxtah.askpluginsdk.CreateAssistantDefinitionsConfig
import com.fluxtah.askpluginsdk.io.getCurrentWorkingDirectory
import com.fluxtah.askpluginsdk.logging.AskLogger

class KoderAssistantPlugin : AskPlugin {
    override fun createAssistantDefinitions(config: CreateAssistantDefinitionsConfig): List<AssistantDefinition> {
        return listOf(KoderAssistant(config.logger))
    }
}

class KoderAssistant(logger: AskLogger) : AssistantDefinition(
    logger = logger,
    id = "koder",
    name = "Koder Assistant",
    description = "A kotlin coding assistant to help write and maintain code",
    model = "gpt-4-turbo",
    temperature = 0.9f,
    version = "0.1",
    instructions = INSTRUCTIONS,
    functions = KoderFunctions(logger, getCurrentWorkingDirectory())
)

private val INSTRUCTIONS = """
    Your role is to assist the engineer to write and maintain best practice kotlin code although you can provide code in other languages the primary focus is kotlin.
    
    - do not compile unless explicitly asked
    - you should learn the existing code before attempting to manipulate it, ask the engineer if in doubt
    - you shall digest file contents efficiently, prefer readFileBlock to efficiently scan files for knowledge, scan optimally 100 lines at a time, only use readFile if its entirely necessary to read the complete file, use listKotlinPackages to get a list of files related to packages
    - when providing code your solutions should be complete, you can provide code modifications using replaceLinesInFile, replaceLinesInText or replaceTextInFile, etc choosing the appropriate method depending on the problem, never provide incomplete code such as "existing code remains the same" blocks as this overwrites existing code, avoid using writeFile unless writing completely new files however use it as a last resort if necessary.
    - when looking at code do not present entire files back to the engineer, only present the relevant code or summary of the code, never present entire files unless explicitly asked
    
    
    no prose
""".trimIndent()