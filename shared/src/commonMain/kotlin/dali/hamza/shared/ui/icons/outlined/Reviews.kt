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

public val Icons.Outlined.Reviews: ImageVector
    get() {
        if (_reviews != null) {
            return _reviews!!
        }
        _reviews = materialIcon(name = "Outlined.Reviews") {
            materialPath {
                moveTo(20.0f, 2.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 2.0f, 2.0f, 2.9f, 2.0f, 4.0f)
                verticalLineToRelative(18.0f)
                lineToRelative(4.0f, -4.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(4.0f)
                curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f)
                close()
                moveTo(20.0f, 16.0f)
                horizontalLineTo(5.17f)
                lineTo(4.0f, 17.17f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(16.0f)
                verticalLineTo(16.0f)
                close()
            }
            materialPath {
                moveTo(12.0f, 15.0f)
                lineToRelative(1.57f, -3.43f)
                lineToRelative(3.43f, -1.57f)
                lineToRelative(-3.43f, -1.57f)
                lineToRelative(-1.57f, -3.43f)
                lineToRelative(-1.57f, 3.43f)
                lineToRelative(-3.43f, 1.57f)
                lineToRelative(3.43f, 1.57f)
                close()
            }
        }
        return _reviews!!
    }

private var _reviews: ImageVector? = null
