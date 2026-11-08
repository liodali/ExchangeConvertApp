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

package dali.hamza.shared.ui.icons.automirrored.outlined

import dali.hamza.shared.ui.icons.Icons
import dali.hamza.shared.ui.icons.materialIcon
import dali.hamza.shared.ui.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.AutoMirrored.Outlined.ArrowRightAlt: ImageVector
    get() {
        if (_arrowRightAlt != null) {
            return _arrowRightAlt!!
        }
        _arrowRightAlt = materialIcon(name = "AutoMirrored.Outlined.ArrowRightAlt", autoMirror =
                true) {
            materialPath {
                moveTo(16.01f, 11.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(12.01f)
                verticalLineToRelative(3.0f)
                lineTo(20.0f, 12.0f)
                lineToRelative(-3.99f, -4.0f)
                verticalLineToRelative(3.0f)
                close()
            }
        }
        return _arrowRightAlt!!
    }

private var _arrowRightAlt: ImageVector? = null
