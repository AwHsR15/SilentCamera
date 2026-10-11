# 2.1 interface

Library: Google Jetpack Compose Material 3, Material 3 1.3.1, stable Compose BOM 2024.12.01.
Reference: https://developer.android.com/develop/ui/compose/designsystems/material3

The dashboard uses neutral surfaces, a restrained green accent, a camera entry card and a two-column mode grid. Tapping a mode opens a Material 3 bottom sheet containing its requirements, current values and apply/restore actions. Capability and manual verification results remain separate.

The camera uses a dark viewfinder with rounded corners and a circular shutter, with gallery and lens controls on either side. Diagnostics groups device information and selectable command output. Shared typography and shape tokens apply to all three languages and both themes. Scrollable content supports smaller screens and larger text.

Miuix was also considered, but its upstream documentation describes the library as experimental. Keeping the official stable Material 3 library avoids an unnecessary toolchain migration and preserves Android accessibility and localization behavior.

Visual device testing remains required for OEM font scaling, camera rendering and vendor shutter behavior; a successful build alone does not establish these.
