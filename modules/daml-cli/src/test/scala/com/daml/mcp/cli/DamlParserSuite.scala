package com.daml.mcp.cli

import munit.FunSuite
import com.daml.mcp.cli.models.DamlField

class DamlParserSuite extends FunSuite:

  val simpleTemplate: String =
    """module Main where
      |
      |template Asset
      |  with
      |    issuer : Party
      |    owner  : Party
      |    name   : Text
      |  where
      |    signatory issuer
      |    observer owner
      |    choice Give : ContractId Asset
      |      with
      |        newOwner : Party
      |      controller owner
      |      do create this with owner = newOwner
      |""".stripMargin

  test("extracts module name"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    assertEquals(templates.head.module, "Main")

  test("extracts template name"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    assertEquals(templates.head.name, "Asset")

  test("extracts template fields"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    val fields = templates.head.fields
    assertEquals(fields.length, 3)
    assertEquals(fields(0), DamlField("issuer", "Party"))
    assertEquals(fields(1), DamlField("owner", "Party"))
    assertEquals(fields(2), DamlField("name", "Text"))

  test("extracts signatories"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    assertEquals(templates.head.signatories, "issuer")

  test("extracts observers"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    assertEquals(templates.head.observers, "owner")

  test("extracts choice name and return type"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    val choices = templates.head.choices
    assertEquals(choices.length, 1)
    assertEquals(choices.head.name, "Give")
    assertEquals(choices.head.returnType, "ContractId Asset")

  test("extracts choice params"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    val params = templates.head.choices.head.params
    assertEquals(params.length, 1)
    assertEquals(params.head, DamlField("newOwner", "Party"))

  test("extracts choice controller"):
    val templates = DamlParser.parseFile(simpleTemplate, "Main.daml")
    assertEquals(templates.head.choices.head.controller, "owner")

  test("stores source file path"):
    val templates = DamlParser.parseFile(simpleTemplate, "/project/daml/Main.daml")
    assertEquals(templates.head.sourceFile, "/project/daml/Main.daml")

  val multiTemplate: String =
    """module Iou where
      |
      |template Iou
      |  with
      |    issuer : Party
      |    owner : Party
      |    amount : Decimal
      |  where
      |    signatory issuer, owner
      |    observer owner
      |
      |    choice Transfer : ContractId IouTransfer
      |      with
      |        newOwner : Party
      |      controller owner
      |      do create IouTransfer with iou = this; newOwner
      |
      |    choice Split : (ContractId Iou, ContractId Iou)
      |      with
      |        splitAmount : Decimal
      |      controller owner
      |      do return (undefined, undefined)
      |
      |template IouTransfer
      |  with
      |    iou : Iou
      |    newOwner : Party
      |  where
      |    signatory iou.issuer, iou.owner
      |    observer newOwner
      |
      |    choice Accept : ContractId Iou
      |      controller newOwner
      |      do create iou with owner = newOwner
      |""".stripMargin

  test("parses multiple templates from one file"):
    val templates = DamlParser.parseFile(multiTemplate, "Iou.daml")
    assertEquals(templates.length, 2)
    assertEquals(templates(0).name, "Iou")
    assertEquals(templates(1).name, "IouTransfer")

  test("parses multiple choices on a single template"):
    val templates = DamlParser.parseFile(multiTemplate, "Iou.daml")
    val iou = templates.find(_.name == "Iou").get
    assertEquals(iou.choices.length, 2)
    assertEquals(iou.choices(0).name, "Transfer")
    assertEquals(iou.choices(1).name, "Split")

  test("parses multi-party signatories"):
    val templates = DamlParser.parseFile(multiTemplate, "Iou.daml")
    assertEquals(templates(0).signatories, "issuer, owner")

  test("parses choice without params"):
    val templates = DamlParser.parseFile(multiTemplate, "Iou.daml")
    val accept = templates(1).choices.find(_.name == "Accept").get
    assertEquals(accept.params, Seq.empty)

  test("returns empty for source with no templates"):
    val source = """module Empty where
                   |
                   |type Foo = Text
                   |""".stripMargin
    val templates = DamlParser.parseFile(source, "Empty.daml")
    assertEquals(templates.length, 0)

  test("handles unknown module gracefully"):
    val source = """template Orphan
                   |  with
                   |    owner : Party
                   |  where
                   |    signatory owner
                   |""".stripMargin
    val templates = DamlParser.parseFile(source, "Orphan.daml")
    assertEquals(templates.head.module, "Unknown")
