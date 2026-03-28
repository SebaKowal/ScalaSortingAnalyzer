package ui

object Theme:
  // Core palette
  val BgDeep      = "#080a12"   // deepest background
  val BgBase      = "#0d0f1c"   // panel background
  val BgRaised    = "#13162a"   // raised surfaces
  val BgBorder    = "#1e2240"   // borders / dividers
  val BgHover     = "#1a1e38"   // hover states

  // Accent — electric cyan as primary, amber as secondary
  val AccentPrimary   = "#00d4ff"   // cyan — comparisons, active
  val AccentSecondary = "#ff8c00"   // amber — swaps
  val AccentSuccess   = "#00ff9d"   // green — sorted
  val AccentDanger    = "#ff2d6b"   // red — pivot / danger
  val AccentMuted     = "#3d4a7a"   // muted blue — default bars

  // Text
  val TextBright  = "#e8eaff"
  val TextNormal  = "#8892b8"
  val TextDim     = "#444d6e"
  val TextAccent  = "#00d4ff"

  // Functional styles
  def panelStyle(extra: String = "") =
    s"-fx-background-color: $BgBase; $extra"

  def cardStyle =
    s"-fx-background-color: $BgRaised; -fx-border-color: $BgBorder; " +
      s"-fx-border-radius: 4; -fx-background-radius: 4;"

  def labelStyle(size: Int = 11, color: String = TextNormal) =
    s"-fx-text-fill: $color; -fx-font-size: ${size}px; -fx-font-family: 'Consolas', monospace;"

  def titleStyle(size: Int = 12, color: String = TextAccent) =
    s"-fx-text-fill: $color; -fx-font-size: ${size}px; -fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"

  def buttonPrimary =
    s"-fx-background-color: $AccentPrimary; -fx-text-fill: $BgDeep; " +
      s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; " +
      s"-fx-font-size: 11px; -fx-padding: 7 18; -fx-background-radius: 3; -fx-cursor: hand;"

  def buttonSecondary =
    s"-fx-background-color: $BgRaised; -fx-text-fill: $TextNormal; " +
      s"-fx-border-color: $BgBorder; -fx-border-radius: 3; -fx-background-radius: 3; " +
      s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-padding: 7 18; -fx-cursor: hand;"

  def buttonDanger =
    s"-fx-background-color: transparent; -fx-text-fill: $AccentDanger; " +
      s"-fx-border-color: $AccentDanger; -fx-border-radius: 3; -fx-background-radius: 3; " +
      s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-padding: 7 18; -fx-cursor: hand;"

  def comboStyle =
    s"-fx-background-color: $BgRaised; -fx-text-fill: $TextBright; " +
      s"-fx-border-color: $BgBorder; -fx-border-radius: 3; -fx-background-radius: 3; " +
      s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;"

  def sliderStyle =
    s"-fx-control-inner-background: $BgRaised; " +
      s"-fx-accent: $AccentPrimary;"

  def scrollPaneStyle =
    s"-fx-background: $BgBase; -fx-background-color: $BgBase; -fx-border-color: transparent; " +
      s"-fx-padding: 0;"

  def tabPaneStyle =
    s"-fx-background-color: $BgDeep; -fx-tab-min-height: 30px; -fx-tab-max-height: 30px;"

  def separatorStyle =
    s"-fx-background-color: $BgBorder; -fx-pref-height: 1px;"