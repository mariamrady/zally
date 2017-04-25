package de.zalando.zally.rules

import com.typesafe.config.Config
import de.zalando.zally.Violation
import de.zalando.zally.ViolationType
import de.zalando.zally.utils.getAllJsonObjects
import io.swagger.models.Swagger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AvoidSynonymsRule(@Autowired rulesConfig: Config) : AbstractRule() {

    override val title = "Use common property names"
    // TODO: Provide URL
    override val url = ""
    override val violationType = ViolationType.SHOULD
    override val code = "S010"

    private val descPattern = "Property names should utilize common dictionary"

    @Suppress("UNCHECKED_CAST")
    private val commonDictionary = rulesConfig.getConfig("$name.dictionary").entrySet()
            .map { (key, config) -> key to config.unwrapped() as List<String> }

    override fun validate(swagger: Swagger): Violation? {
        return validate(swagger, false)
    }

    fun validate(swagger: Swagger, returnViolation: Boolean): Violation? {
        val reversedDictionary = commonDictionary.flatMap { (canonical, synonyms) -> synonyms.map { it to canonical } }.toMap()
        val result = swagger.getAllJsonObjects().flatMap { (def, path) ->
            def.keys.filter { it in reversedDictionary }.map { it to path }
        }

        // TODO: Start returning violations once we'll define a proper dictionary (#301)
        return if (result.isNotEmpty() && returnViolation) {
            val (names, paths) = result.unzip()
            val details = names.toSet().groupBy(reversedDictionary::get)
                    .map { (canonical, synonyms) -> canonical + " instead of " + synonyms.joinToString(", ") }
                    .joinToString("\n")
            Violation(this, title, "$descPattern:\n$details", violationType, url, paths.toSet().toList())
        } else null
    }
}