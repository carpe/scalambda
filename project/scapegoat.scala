import sbt._

object scapegoat {
  import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport._
  lazy val scapegoatSettings: Seq[Def.Setting[_]] = {
    Seq(
      ThisBuild / scapegoatVersion := "1.4.15",
      scapegoatDisabledInspections := Seq("FinalModifierOnCaseClass", "UnsafeTraversableMethods")
    )
  }
}
