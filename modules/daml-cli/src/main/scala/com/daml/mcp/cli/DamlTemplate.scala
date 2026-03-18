package com.daml.mcp.cli

final case class DamlField(name: String, fieldType: String)

final case class DamlChoice(
    name: String,
    returnType: String,
    params: Seq[DamlField],
    controller: String
)

final case class DamlTemplate(
    name: String,
    module: String,
    fields: Seq[DamlField],
    signatories: String,
    observers: String,
    choices: Seq[DamlChoice],
    sourceFile: String
)
