#!/system/bin/sh
# Magisk copies this ZIP to MODPATH before invoking this script.
[ "$BOOTMODE" = true ] || abort 'Install through the Magisk app on a booted device.'
[ "$MAGISK_VER_CODE" -ge 20400 ] || abort 'Magisk 20.4 or newer is required.'
[ -s "$MODPATH/assets/silent.ogg" ] || abort 'Silent OGG asset is missing.'
. "$MODPATH/path-policy.sh"
ui_print '- Detecting supported shutter audio files'
matches=0
for base in /system/media/audio/ui /system/vendor/media/audio/ui /system/product/media/audio/ui /system/system_ext/media/audio/ui /vendor/media/audio/ui /product/media/audio/ui /system_ext/media/audio/ui; do
  [ -d "$base" ] || continue
  for source in "$base"/*.ogg; do
    [ -f "$source" ] || continue
    relative=$(overlay_path "$source") || continue
    destination="$MODPATH/$relative"
    mkdir -p "${destination%/*}" || abort 'Cannot create overlay directory.'
    cp "$MODPATH/assets/silent.ogg" "$destination" || abort 'Cannot write silent overlay.'
    ui_print "  $source"
    matches=$((matches + 1))
  done
done
[ "$matches" -gt 0 ] || abort 'No supported shutter files found. Camera-APK or hardware sounds need another mode.'
set_perm_recursive "$MODPATH/system" 0 0 0755 0644
ui_print '- Reboot, then check actual sound with your camera.'
ui_print '- Disable or uninstall this module and reboot to restore.'
