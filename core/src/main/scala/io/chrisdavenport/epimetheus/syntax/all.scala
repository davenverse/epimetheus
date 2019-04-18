package io.chrisdavenport.epimetheus.syntax

trait all
  extends counter
  with gauge
  with histogram
  with summary

object all extends all
  with GaugeSyntaxBincompat1