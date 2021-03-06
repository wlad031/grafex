package com.grafex.core

import cats.data.NonEmptyList
import org.scalatest.funsuite.AnyFunSuite

class ModeCallsParserTest extends AnyFunSuite {

  test("Mode calls parser should return error on empty string") {
    ModeCallsParser.parse("") match {
      case Left(error) =>
        val expectedError = ModeCallsParser.ParseError.EmptyCall()
        assert(error === expectedError)
      case Right(value) => fail(s"Unexpected result $value")
    }
  }

  test("Mode calls parser should return error on missing action") {
    ModeCallsParser.parse("mode") match {
      case Left(error) =>
        val expectedError = ModeCallsParser.ParseError.MissingAction("mode")
        assert(error === expectedError)
      case Right(value) => fail(s"Unexpected result $value")
    }
  }

  test("Mode calls parser should return error on missing action for versioned call") {
    ModeCallsParser.parse("mode.1") match {
      case Left(error) =>
        val expectedError = ModeCallsParser.ParseError.MissingAction("mode.1")
        assert(error === expectedError)
      case Right(value) => fail(s"Unexpected result $value")
    }
  }

  test("Mode calls parser should parse latest call") {
    ModeCallsParser.parse("mode/action") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call, Nil)) =>
        val expectedCall = Mode.Call("mode", definitions.action.Id("action"))
        assert(call === expectedCall)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse versioned call") {
    ModeCallsParser.parse("mode.1/action") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call, Nil)) =>
        val expectedCall =
          Mode.Call(definitions.mode.Id("mode", "1"), definitions.action.Id("action"))
        assert(call === expectedCall)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse call with complex version") {
    ModeCallsParser.parse("mode.1.2.3/action") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call, Nil)) =>
        val expectedCall =
          Mode.Call(definitions.mode.Id("mode", "1.2.3"), definitions.action.Id("action"))
        assert(call === expectedCall)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse call with empty last sub call") {
    ModeCallsParser.parse("mode.1/action>") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call, Nil)) =>
        val expectedCall =
          Mode.Call(definitions.mode.Id("mode", "1"), definitions.action.Id("action"))
        assert(call === expectedCall)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should return error on empty first sub call") {
    ModeCallsParser.parse(">mode1/action1") match {
      case Left(error) =>
        val expectedError = ModeCallsParser.ParseError.EmptySubCall(">mode1/action1")
        assert(error === expectedError)
      case Right(value) => fail(s"Unexpected result $value")
    }
  }

  test("Mode calls parser should return error on empty second sub call") {
    ModeCallsParser.parse("mode1/action1>>mode2/action2") match {
      case Left(error) =>
        val expectedError = ModeCallsParser.ParseError.EmptySubCall("mode1/action1>>mode2/action2")
        assert(error === expectedError)
      case Right(value) => fail(s"Unexpected result $value")
    }
  }

  test("Mode calls parser should parse multiple calls") {
    ModeCallsParser.parse("mode1/action1>mode2/action2") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call1, call2 :: Nil)) =>
        val expectedCall1 = Mode.Call("mode1", definitions.action.Id("action1"))
        val expectedCall2 = Mode.Call("mode2", definitions.action.Id("action2"))
        assert(call1 === expectedCall1)
        assert(call2 === expectedCall2)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse multiple calls where first is versioned") {
    ModeCallsParser.parse("mode1.1.0.0/action1>mode2/action2") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call1, call2 :: Nil)) =>
        val expectedCall1 =
          Mode.Call(definitions.mode.Id("mode1", "1.0.0"), definitions.action.Id("action1"))
        val expectedCall2 = Mode.Call("mode2", definitions.action.Id("action2"))
        assert(call1 === expectedCall1)
        assert(call2 === expectedCall2)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse multiple calls where last is versioned") {
    ModeCallsParser.parse("mode1/action1>mode2.1.0.0/action2") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call1, call2 :: Nil)) =>
        val expectedCall1 = Mode.Call("mode1", definitions.action.Id("action1"))
        val expectedCall2 =
          Mode.Call(definitions.mode.Id("mode2", "1.0.0"), definitions.action.Id("action2"))
        assert(call1 === expectedCall1)
        assert(call2 === expectedCall2)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }

  test("Mode calls parser should parse multiple calls where both are versioned") {
    ModeCallsParser.parse("mode1.1.0.0/action1>mode2.2.0.0/action2") match {
      case Left(error) => fail(s"Unexpected error $error")
      case Right(NonEmptyList(call1, call2 :: Nil)) =>
        val expectedCall1 =
          Mode.Call(definitions.mode.Id("mode1", "1.0.0"), definitions.action.Id("action1"))
        val expectedCall2 =
          Mode.Call(definitions.mode.Id("mode2", "2.0.0"), definitions.action.Id("action2"))
        assert(call1 === expectedCall1)
        assert(call2 === expectedCall2)
      case Right(r) => fail(s"Unexpected result $r")
    }
  }
}
