package com.daml.mcp.cli.models

/** Parsed DAML template metadata. */
final case class DamlField(name: String, fieldType: String)

/** Parsed DAML choice metadata. */
final case class DamlChoice(
    name: String,
    returnType: String,
    params: Seq[DamlField],
    controller: String
)

/** Parsed DAML template metadata. */
final case class DamlTemplate(
    name: String,
    module: String,
    fields: Seq[DamlField],
    signatories: String,
    observers: String,
    choices: Seq[DamlChoice],
    sourceFile: String
)
