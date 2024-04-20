package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dali.hamza.echangecurrencyapp.models.DataUIState
import dali.hamza.echangecurrencyapp.models.ErrorUIState
import dali.hamza.echangecurrencyapp.models.LoadingUIState
import dali.hamza.echangecurrencyapp.models.NoDataUIState
import dali.hamza.echangecurrencyapp.models.UIState


@Composable
fun <T> UIState.StateBuilder(
    loadingUI: (@Composable () -> Unit)?,
    emptyUI: (@Composable () -> Unit)?,
    errorUI: (@Composable () -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (this) {
        is LoadingUIState -> when {
            loadingUI != null -> loadingUI()
            else -> Loading()
        }

        is NoDataUIState -> when {
            emptyUI != null -> emptyUI()
            else -> EmptyBox()
        }

        is DataUIState<*> ->
            content(data!! as T)


        is ErrorUIState -> when {
            errorUI != null -> errorUI()
            else -> ShowErrorList()
        }

        else -> EmptyBox()
    }
}


@Composable
fun SpacerHeight(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun SpacerWidth(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}

@Composable
fun EmptyBox() {
    Box {}
}

@Composable
fun EmptyData(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector = Icons.Default.Payments,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.then(modifier)
    ) {
        Icon(icon, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(32.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun Center(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        content()
    }
}

@Preview
@Composable
fun ShowEmptyDataPreview() {
    EmptyData(text = "No currencies available")
}