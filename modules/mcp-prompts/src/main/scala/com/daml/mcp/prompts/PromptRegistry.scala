package com.daml.mcp.prompts

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{
  GetPromptResult,
  Prompt,
  PromptArgument,
  PromptMessage,
  Role
}

import java.util as ju

class PromptRegistry:

  val explainTemplate: SyncPromptSpecification = SyncPromptSpecification(
    Prompt(
      "explain_template",
      "Explain a DAML template — its purpose, fields, signatories, observers, and choices",
      ju.List.of(
        PromptArgument("template_source", "The full DAML source code of the template", true)
      )
    ),
    (_, request) =>
      val source = request.arguments().getOrDefault("template_source", "")
      GetPromptResult(
        "Explain this DAML template",
        ju.List.of(
          PromptMessage(
            Role.USER,
            McpSchema.TextContent(
              s"""Explain the following DAML template in detail. Cover:
             |1. What this template represents (business meaning)
             |2. What fields it has and what they mean
             |3. Who are the signatories and observers and why
             |4. What choices are available and what they do
             |5. Any constraints (ensure clauses)
             |
             |```daml
             |$source
             |```""".stripMargin
            )
          )
        )
      )
  )

  val reviewDaml: SyncPromptSpecification = SyncPromptSpecification(
    Prompt(
      "review_daml",
      "Review DAML code for correctness, authorization issues, and best practices",
      ju.List.of(PromptArgument("source", "The DAML source code to review", true))
    ),
    (_, request) =>
      val source = request.arguments().getOrDefault("source", "")
      GetPromptResult(
        "Review DAML code",
        ju.List.of(
          PromptMessage(
            Role.USER,
            McpSchema.TextContent(
              s"""Review the following DAML code. Check for:
             |1. Authorization correctness — are signatories and controllers set properly?
             |2. Privacy — do observers see only what they should?
             |3. Choice safety — are consuming vs non-consuming choices used correctly?
             |4. Data integrity — are ensure clauses sufficient?
             |5. Best practices — naming, modularity, readability
             |
             |```daml
             |$source
             |```""".stripMargin
            )
          )
        )
      )
  )

  val generateTest: SyncPromptSpecification = SyncPromptSpecification(
    Prompt(
      "generate_daml_test",
      "Generate a Daml Script test for a given template",
      ju.List.of(PromptArgument("template_source", "The DAML template source code to test", true))
    ),
    (_, request) =>
      val source = request.arguments().getOrDefault("template_source", "")
      GetPromptResult(
        "Generate Daml Script test",
        ju.List.of(
          PromptMessage(
            Role.USER,
            McpSchema.TextContent(
              s"""Generate a comprehensive Daml Script test for the following template.
             |The test should:
             |1. Allocate parties for all signatories and observers
             |2. Create a contract instance
             |3. Exercise each choice and verify the result
             |4. Test edge cases and error conditions
             |
             |```daml
             |$source
             |```""".stripMargin
            )
          )
        )
      )
  )

  val all: Seq[SyncPromptSpecification] =
    Seq(explainTemplate, reviewDaml, generateTest)
