package dali.hamza.echangecurrencyapp.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.UiMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.compose.theme.DarkColors

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputAmount(
    modifier: Modifier = Modifier,
    openCurrencyDialog: () -> Unit,
    actionCalculate: (() -> Unit)?,
    onValueChanged: (String) -> Unit,
    clearText: () -> Unit,
    form: AmountInput,
    currency: String?,
    keyboardController: SoftwareKeyboardController? = null,
) {
    var showText by rememberSaveable {
        mutableStateOf(false)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.then(
            Modifier
                .verticalScroll(ScrollState(0))
                .padding(horizontal = 5.dp)

        )
    ) {

        Box {
            if (showText) {
                TextAmountWithCurrency(
                    amount = form.amount,
                    currency = currency,
                    modifier = Modifier.clickable {
                        if (showText) {
                            showText = false
                        }
                    }
                )
            } else {
                AmountTextField(
                    initValue = form.amount,
                    onValueChanged = onValueChanged,
                    currency = currency,
                    keyboardController = keyboardController,
                    actionCalculate = {
                        showText = true
                        if (actionCalculate != null) {
                            actionCalculate()
                        }
                    },
                    modifier = Modifier,
                )
            }

        }
        if (currency != null) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.height(64.dp)
            ) {
                Text(text = currency, modifier = Modifier.padding(bottom = 12.dp))
            }
        }


        AmountSideSetting(
            clearText = {
                showText = false
                clearText()
            },
            openCurrencyDialog = openCurrencyDialog
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AmountTextField(
    modifier: Modifier,
    initValue: String,
    currency: String?,
    keyboardController: SoftwareKeyboardController? = null,
    onValueChanged: (String) -> Unit,
    actionCalculate: (() -> Unit)?,

    ) {
    var size by rememberSaveable { mutableIntStateOf(64) }
    var action by rememberSaveable { mutableIntStateOf(0) }
    BasicTextField(
        initValue,
        onValueChange = { newAmount ->
            if (currency.isNullOrEmpty() || newAmount.isEmpty()) {
                action = 0
            } else {
                action = 7
            }
            onValueChanged(newAmount)
        },
        enabled = true,
        maxLines = 1,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 42.sp,
            textAlign = if (initValue.isEmpty()) TextAlign.End else TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = if (action == 0) ImeAction.None else ImeAction.Done
        ),
        //visualTransformation = AmountTransformation(currency),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
            if (actionCalculate != null) {
                actionCalculate()
            }
        }),
        onTextLayout = { textLayoutResult ->
            var baseSize = 20
            val textWithoutCurrency = textLayoutResult.layoutInput.text
            if (textWithoutCurrency.length in 4..6) {
                baseSize = 18
            } else if (textWithoutCurrency.length >= 7) {
                baseSize = 21
            }
            var len = textLayoutResult.layoutInput.text.length
            if (textWithoutCurrency.isEmpty()) {
                len = 1
            } else if (textWithoutCurrency.isNotEmpty()) {
                len += 2
            }
            size = len * baseSize
        },
        modifier = Modifier
            .requiredWidth(size.dp)

            .then(modifier),
    )
}

@Composable
fun AmountSideSetting(
    clearText: () -> Unit,
    openCurrencyDialog: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Icon(
            Icons.Default.Clear, "clickable icon to clear text",
            modifier = Modifier.clickable {
                clearText()
            },
        )
        SpacerHeight(height = 12.dp)
        Icon(
            Icons.Default.CurrencyExchange, "clickable icon to open dialog currency",
            modifier = Modifier.clickable {
                openCurrencyDialog()
            },
        )
    }
}

@Composable
fun TextAmountWithCurrency(
    modifier: Modifier,
    amount: String,
    currency: String?,
) {
    Text(
        text = amount,
        fontSize = 42.sp,
        modifier = modifier
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Preview(widthDp = 350, heightDp = 92)
@Composable
fun ShowTextAmount() {
    TextAmountWithCurrency(amount = "12", currency = "Tnd", modifier = Modifier)
}

@OptIn(ExperimentalComposeUiApi::class)
@UiMode
@Preview(
    widthDp = 350, heightDp = 92,
    showBackground = true
)
@Composable
fun ShowFieldAmount() {
    MaterialTheme(colorScheme = DarkColors) {
        InputAmount(
            form = AmountInput(amount = "12"),
            currency = "USD",
            keyboardController = null,
            onValueChanged = {},
            clearText = {},
            actionCalculate = {

            },
            openCurrencyDialog = {},
            // MaterialTheme.colorScheme.background)
        )
    }
}

class AmountTransformation(val currency: String?) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            buildAnnotatedString {
                this.append(text.text)
                withStyle(style = SpanStyle(fontSize = 14.sp)) {
                    append(" ${currency ?: ""}")
                }

            }, OffsetMapping.Identity
        )
    }

}