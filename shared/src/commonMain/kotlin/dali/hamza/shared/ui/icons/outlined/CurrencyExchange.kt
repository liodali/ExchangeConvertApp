/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dali.hamza.shared.ui.icons.outlined

import dali.hamza.shared.ui.icons.Icons
import dali.hamza.shared.ui.icons.materialIcon
import dali.hamza.shared.ui.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.CurrencyExchange: ImageVector
    get() {
        if (_currencyExchange != null) {
            return _currencyExchange!!
        }
        _currencyExchange = materialIcon(name = "Outlined.CurrencyExchange") {
            materialPath {
                moveTo(12.89f, 11.1f)
                curveToRelative(-1.78f, -0.59f, -2.64f, -0.96f, -2.64f, -1.9f)
                curveToRelative(0.0f, -1.02f, 1.11f, -1.39f, 1.81f, -1.39f)
                curveToRelative(1.31f, 0.0f, 1.79f, 0.99f, 1.9f, 1.34f)
                lineToRelative(1.58f, -0.67f)
                curveTo(15.39f, 8.03f, 14.72f, 6.56f, 13.0f, 6.24f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(1.26f)
                curveTo(8.52f, 6.82f, 8.51f, 9.12f, 8.51f, 9.22f)
                curveToRelative(0.0f, 2.27f, 2.25f, 2.91f, 3.35f, 3.31f)
                curveToRelative(1.58f, 0.56f, 2.28f, 1.07f, 2.28f, 2.03f)
                curveToRelative(0.0f, 1.13f, -1.05f, 1.61f, -1.98f, 1.61f)
                curveToRelative(-1.82f, 0.0f, -2.34f, -1.87f, -2.4f, -2.09f)
                lineTo(8.1f, 14.75f)
                curveToRelative(0.63f, 2.19f, 2.28f, 2.78f, 2.9f, 2.96f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-1.24f)
                curveToRelative(0.4f, -0.09f, 2.9f, -0.59f, 2.9f, -3.22f)
                curveTo(15.9f, 13.15f, 15.29f, 11.93f, 12.89f, 11.1f)
                close()
                moveTo(3.0f, 21.0f)
                horizontalLineTo(1.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(2.0f)
                lineToRelative(-2.48f, 0.0f)
                curveToRelative(1.61f, 2.41f, 4.36f, 4.0f, 7.48f, 4.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.0f, 6.08f, -4.92f, 11.0f, -11.0f, 11.0f)
                curveToRelative(-3.72f, 0.0f, -7.01f, -1.85f, -9.0f, -4.67f)
                lineTo(3.0f, 21.0f)
                close()
                moveTo(1.0f, 12.0f)
                curveTo(1.0f, 5.92f, 5.92f, 1.0f, 12.0f, 1.0f)
                curveToRelative(3.72f, 0.0f, 7.01f, 1.85f, 9.0f, 4.67f)
                lineTo(21.0f, 3.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(-6.0f)
                verticalLineTo(7.0f)
                lineToRelative(2.48f, 0.0f)
                curveTo(17.87f, 4.59f, 15.12f, 3.0f, 12.0f, 3.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                horizontalLineTo(1.0f)
                close()
            }
        }
        return _currencyExchange!!
    }

private var _currencyExchange: ImageVector? = null
