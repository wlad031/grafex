package dev.vgerasimov.grafex
package webui

import scalacss.ScalaCssReact._
import japgolly.scalajs.react.{CtorType, _}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.document
import scalacss.DevDefaults._

import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.document
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.Component
import scalacss.ScalaCssReact._

object Main {

  def main(args: Array[String]): Unit = {
    println("hello world")

    MyStyles.addToDocument()

    val HelloMessage = ScalaComponent.builder[String]
      .render($ => <.div("Hello ", $.props))
      .build

    val MyComponent =
      ScalaComponent.builder[Unit]("blah")
        .render(_ =>
          <.button(
//            ^.onClick --> alert("Hey! I didn't say you could click me!"),
            MyStyles.bootstrapButton,
            "I am a button!"))
        .build

    val root = document.getElementById("root")
//    HelloMessage("John  ").renderIntoDOM(root)
    MyComponent().renderIntoDOM(root)

//    val NoArgs =
//      ScalaComponent.static(<.div("Hello!"))
//
//    NoArgs().renderIntoDOM(document.body)
  }

  object MyStyles extends StyleSheet.Inline {
    import dsl._

    val bootstrapButton = style(
      addClassName("btn btn-default"),
      fontSize(200 %%)
    )
  }

}
