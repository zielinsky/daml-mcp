package com.daml.mcp.ledger

import com.daml.ledger.javaapi.data.{
  CreatedEvent,
  CreateCommand,
  ExerciseCommand,
  Identifier,
  Value,
}

trait LedgerClient:
  def queryContracts(templateId: Identifier, parties: Set[String]): Seq[CreatedEvent]
  def createContract(actAs: String, command: CreateCommand): String
  def exerciseChoice(actAs: String, command: ExerciseCommand): Value
  def archiveContract(actAs: String, templateId: Identifier, contractId: String): Unit
  def listKnownParties(): Seq[String]
  def allocateParty(hint: Option[String], displayName: Option[String]): String
  def getLedgerEnd(): String
