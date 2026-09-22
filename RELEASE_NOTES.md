> app-version: 2.4.14

* Fixed the v2.4.13 bottom bar: the glass layer was recorded in the wrong
  order, so the bar blurred a backgroundless copy of the screen — that is
  why it looked strange. The recorder now captures the full backdrop
  (background + color wash + content), exactly like the v2.0-v2.1 engine
* The bar is now a pixel-faithful restoration of the original Telegram
  glass: the exact 28dp live backdrop blur, the exact tint, the exact
  top-weighted scrim and the bright three-stop specular rim of that era
* The classic bar no longer loses its blur in battery saver / low-RAM
  (FROST) mode — it always frosts, on Android 12+ (hardware) and on
  Android 8-11 (soft CPU snapshot), just like back then
* No iOS lens, no refraction, no zoom-bleed on the bar; cards and
  everything else are untouched
