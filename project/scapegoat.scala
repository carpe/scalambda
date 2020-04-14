import sbt._

object scapegoat {
  import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport._
  lazy val scapegoatSettings: Seq[Def.Setting[_]] = {
    Seq(
      scapegoatVersion in ThisBuild := "1.4.1",
      scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass", "UnsafeTraversableMethods")
    )
  }
}
