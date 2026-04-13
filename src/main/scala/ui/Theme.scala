package ui

object Theme:
  // Core palette
  val BgDeep      = "#080a12"
  val BgBase      = "#0d0f1c"
  val BgRaised    = "#13162a"
  val BgBorder    = "#1e2240"
  val BgHover     = "#1a1e38"

  val AccentPrimary   = "#00d4ff"
  val AccentSecondary = "#ff8c00"
  val AccentSuccess   = "#00ff9d"
  val AccentDanger    = "#ff2d6b"
  val AccentMuted     = "#3d4a7a"

  val TextBright  = "#e8eaff"
  val TextNormal  = "#8892b8"
  val TextDim     = "#6D719A"
  val TextAccent  = "#00d4ff"

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
    s"-fx-control-inner-background: $BgRaised; -fx-accent: $AccentPrimary;"

  def scrollPaneStyle =
    s"-fx-background: $BgBase; -fx-background-color: $BgBase; -fx-border-color: transparent; -fx-padding: 0;"

  def tabPaneStyle =
    s"-fx-background-color: $BgDeep; -fx-tab-min-height: 30px; -fx-tab-max-height: 30px;"

  def separatorStyle =
    s"-fx-background-color: $BgBorder; -fx-pref-height: 1px;"

  // ── ComboBox stylesheet ───────────────────────────────────────
  // Inject this onto every ComboBox via getStylesheets.add(...)
  val comboBoxStylesheet: String =
    "data:text/css," +
      java.net.URLEncoder.encode(
        s"""
           |/* Main button area */
           |.combo-box {
           |  -fx-background-color: $BgRaised;
           |  -fx-border-color: $BgBorder;
           |  -fx-border-radius: 3;
           |  -fx-background-radius: 3;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 11px;
           |}
           |
           |/* The text shown in the closed box */
           |.combo-box .list-cell {
           |  -fx-background-color: transparent;
           |  -fx-text-fill: $TextNormal;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 11px;
           |  -fx-padding: 3 6 3 6;
           |}
           |
           |/* Arrow button */
           |.combo-box .arrow-button {
           |  -fx-background-color: transparent;
           |  -fx-border-color: transparent;
           |}
           |
           |.combo-box .arrow {
           |  -fx-background-color: $TextDim;
           |}
           |
           |.combo-box:hover .arrow {
           |  -fx-background-color: $AccentPrimary;
           |}
           |
           |.combo-box:focused {
           |  -fx-border-color: $AccentPrimary;
           |}
           |
           |/* Dropdown popup */
           |.combo-box-popup .list-view {
           |  -fx-background-color: $BgBase;
           |  -fx-border-color: $BgBorder;
           |  -fx-border-width: 1;
           |  -fx-padding: 2;
           |  -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 4);
           |}
           |
           |/* Each row in the dropdown */
           |.combo-box-popup .list-view .list-cell {
           |  -fx-background-color: transparent;
           |  -fx-text-fill: $TextNormal;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 11px;
           |  -fx-padding: 6 10 6 10;
           |}
           |
           |/* Hovered row */
           |.combo-box-popup .list-view .list-cell:hover {
           |  -fx-background-color: $BgRaised;
           |  -fx-text-fill: $TextBright;
           |}
           |
           |/* Selected row */
           |.combo-box-popup .list-view .list-cell:selected {
           |  -fx-background-color: $BgBorder;
           |  -fx-text-fill: $AccentPrimary;
           |}
           |
           |/* Selected + hovered */
           |.combo-box-popup .list-view .list-cell:selected:hover {
           |  -fx-background-color: $BgHover;
           |  -fx-text-fill: $AccentPrimary;
           |}
           |
           |/* Scrollbar inside dropdown */
           |.combo-box-popup .list-view .scroll-bar .track,
           |.combo-box-popup .list-view .scroll-bar .track-background {
           |  -fx-background-color: $BgBase;
           |}
           |
           |.combo-box-popup .list-view .scroll-bar .thumb {
           |  -fx-background-color: $BgBorder;
           |  -fx-background-radius: 2;
           |}
           |""".stripMargin,
        "UTF-8"
      ).replace("+", "%20")

  // ── TabPane stylesheet ────────────────────────────────────────
  val tabPaneStylesheet: String =
    "data:text/css," +
      java.net.URLEncoder.encode(
        s""".tab-pane > .tab-header-area > .headers-region > .tab {
           |  -fx-background-color: transparent;
           |  -fx-background-insets: 0;
           |  -fx-padding: 0 16 0 16;
           |  -fx-border-width: 0;
           |}
           |.tab-pane > .tab-header-area > .headers-region > .tab .tab-label {
           |  -fx-text-fill: $TextDim;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 11px;
           |}
           |.tab-pane > .tab-header-area > .headers-region > .tab:selected {
           |  -fx-background-color: $BgBase;
           |  -fx-border-color: $AccentPrimary;
           |  -fx-border-width: 0 0 2 0;
           |}
           |.tab-pane > .tab-header-area > .headers-region > .tab:selected .tab-label {
           |  -fx-text-fill: $AccentPrimary;
           |}
           |.tab-pane > .tab-header-area {
           |  -fx-background-color: $BgDeep;
           |  -fx-border-color: $BgBorder;
           |  -fx-border-width: 0 0 1 0;
           |}
           |.tab-pane > .tab-header-area > .tab-header-background {
           |  -fx-background-color: $BgDeep;
           |}
           |.tab-pane > .tab-content-area {
           |  -fx-background-color: $BgDeep;
           |  -fx-border-color: transparent;
           |}""".stripMargin,
        "UTF-8"
      ).replace("+", "%20")

  // ── TableView stylesheet ──────────────────────────────────────
  val tableViewStylesheet: String =
    "data:text/css," +
      java.net.URLEncoder.encode(
        s"""
           |.table-view {
           |  -fx-background-color: $BgBase;
           |  -fx-table-cell-border-color: transparent;
           |}
           |.table-view .column-header-background {
           |  -fx-background-color: $BgDeep;
           |}
           |.table-view .column-header {
           |  -fx-background-color: $BgDeep;
           |  -fx-border-color: $BgBorder;
           |  -fx-border-width: 0 1 1 0;
           |}
           |.table-view .column-header .label {
           |  -fx-text-fill: $TextDim;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 10px;
           |  -fx-font-weight: bold;
           |}
           |.table-row-cell {
           |  -fx-background-color: $BgBase;
           |  -fx-border-color: transparent transparent $BgBorder transparent;
           |}
           |.table-row-cell:odd {
           |  -fx-background-color: $BgRaised;
           |}
           |.table-row-cell:selected {
           |  -fx-background-color: $BgHover;
           |}
           |.table-row-cell:hover {
           |  -fx-background-color: $BgHover;
           |}
           |.table-cell {
           |  -fx-text-fill: $TextNormal;
           |  -fx-font-family: 'Consolas', monospace;
           |  -fx-font-size: 11px;
           |  -fx-border-color: transparent;
           |}""".stripMargin,
        "UTF-8"
      ).replace("+", "%20")