package dali.hamza.echangecurrencyapp.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.UiMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dali.hamza.echangecurrencyapp.common.DoubleFormatter
import dali.hamza.echangecurrencyapp.models.AmountInput
import dali.hamza.echangecurrencyapp.ui.compose.theme.DarkColors


@Composable
fun InputAmount(
    modifier: Modifier = Modifier,
    openCurrencyDialog: () -> Unit,
    actionCalculate: (() -> Unit)?,
    onValueChanged: (String) -> Unit,
    form: AmountInput,
    currency: String?,
    keyboardController: SoftwareKeyboardController? = null,
    suffixIcon: @Composable() (() -> Unit)? = null,
    prefixIcon: @Composable() (() -> Unit)? = null
) {
    var showText by rememberSaveable {
        mutableStateOf(false)
    }

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
        onCurrencyChange = {
            openCurrencyDialog()
        },
        suffixIcon = suffixIcon,
        prefixIcon = prefixIcon,
        modifier = modifier,
    )

}


@Composable
fun AmountTextField(
    modifier: Modifier,
    initValue: String,
    currency: String?,
    keyboardController: SoftwareKeyboardController? = null,
    onValueChanged: (String) -> Unit,
    actionCalculate: (() -> Unit)?,
    onCurrencyChange: (() -> Unit)?,
    suffixIcon: (@Composable () -> Unit)? = null,
    prefixIcon: (@Composable () -> Unit)? = null,
) {
    var action: ImeAction by remember { mutableStateOf(ImeAction.None) }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = initValue,
        onValueChange = { newAmount ->
            action =
                if (currency.isNullOrEmpty() || newAmount.isEmpty() || newAmount.toDouble() == 0.0) {
                    ImeAction.None
                } else {
                    ImeAction.Done
                }
            onValueChanged(newAmount)
        },
        placeholder = { Text("Amount") },
        leadingIcon = prefixIcon ?: {
            CurrencyFlagImage(
                currency = currency!!.lowercase(),
                size = 24.dp
            )
        },

        trailingIcon = suffixIcon ?: {
            Text(
                currency?.uppercase() ?: "",
                modifier = Modifier.clickable {
                    if (onCurrencyChange != null) {
                        onCurrencyChange()
                    }
                }
            )
        },
        modifier = Modifier
            //.requiredWidth(size.dp)
            .then(modifier),

        isError = false,
        singleLine = true,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            textAlign = TextAlign.Start
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = action
        ),
        visualTransformation = { text ->
            TransformedText(
                text = AnnotatedString(
                    DoubleFormatter.format(text.text)
                ),
                offsetMapping = OffsetMapping.Identity,
            )
        },
        //visualTransformation = AmountTransformation(currency),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (actionCalculate != null) {
                actionCalculate()
            }
        }),
        //cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),

    )
}


@Composable
fun TextAmountWithCurrency(
    modifier: Modifier,
    amount: String,
) {
    Text(
        text = amount,
        fontSize = 42.sp,
        modifier = modifier
    )
}


@Preview(widthDp = 350, heightDp = 92)
@Composable
fun ShowTextAmount() {
    TextAmountWithCurrency(amount = "12.0", modifier = Modifier)
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
            form = AmountInput(amount = "1200.0"),
            currency = "USD",
            keyboardController = null,
            onValueChanged = {},
            actionCalculate = {

            },
            openCurrencyDialog = {},
            // MaterialTheme.colorScheme.background)
        )
    }
}
