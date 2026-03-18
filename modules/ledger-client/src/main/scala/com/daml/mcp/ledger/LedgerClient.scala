package com.daml.mcp.ledger

import cats.effect.IO
import com.daml.ledger.javaapi.data.{
  CreatedEvent,
  CreateCommand,
  ExerciseCommand,
  Identifier,
  Value
}

trait LedgerClient:
  def queryContracts(templateId: Identifier, parties: Set[String]): IO[Seq[CreatedEvent]]
  def createContract(actAs: String, command: CreateCommand): IO[String]
  def exerciseChoice(actAs: String, command: ExerciseCommand): IO[Value]
  def archiveContract(actAs: String, templateId: Identifier, contractId: String): IO[Unit]
  def listKnownParties(): IO[Seq[String]]
  def allocateParty(hint: Option[String], displayName: Option[String]): IO[String]
  def getLedgerEnd(): IO[String]
