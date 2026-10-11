#!/system/bin/sh
# Only exact shutter filenames under known audio directories may be overlaid.
overlay_path() {
  case "$1" in *'/../'*|*'/./'*|*'//'*) return 1 ;; esac
  case "${1%/*}" in
    /system/media/audio/ui|/system/vendor/media/audio/ui|/system/product/media/audio/ui|/system/system_ext/media/audio/ui|/vendor/media/audio/ui|/product/media/audio/ui|/system_ext/media/audio/ui) ;;
    *) return 1 ;;
  esac
  case "${1##*/}" in
    camera_click.ogg|camera_click_short.ogg|camera_shutter.ogg|camera_shutter_1.ogg|camera_shutter_2.ogg|camera_shutter_3.ogg|shutter.ogg|Shutter.ogg|Shutter_multiple.ogg) ;;
    *) return 1 ;;
  esac
  case "$1" in /system/*) printf '%s\n' "${1#/}" ;; *) printf 'system%s\n' "$1" ;; esac
}
