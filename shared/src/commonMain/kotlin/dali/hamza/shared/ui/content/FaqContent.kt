package dali.hamza.shared.ui.content

import androidx.compose.runtime.Composable
import echangecurrencyapp.shared.generated.resources.Res
import echangecurrencyapp.shared.generated.resources.faq_acc_a1
import echangecurrencyapp.shared.generated.resources.faq_acc_a2
import echangecurrencyapp.shared.generated.resources.faq_acc_a3
import echangecurrencyapp.shared.generated.resources.faq_acc_a4
import echangecurrencyapp.shared.generated.resources.faq_acc_q1
import echangecurrencyapp.shared.generated.resources.faq_acc_q2
import echangecurrencyapp.shared.generated.resources.faq_acc_q3
import echangecurrencyapp.shared.generated.resources.faq_acc_q4
import echangecurrencyapp.shared.generated.resources.faq_sec_a1
import echangecurrencyapp.shared.generated.resources.faq_sec_a2
import echangecurrencyapp.shared.generated.resources.faq_sec_a3
import echangecurrencyapp.shared.generated.resources.faq_sec_q1
import echangecurrencyapp.shared.generated.resources.faq_sec_q2
import echangecurrencyapp.shared.generated.resources.faq_sec_q3
import echangecurrencyapp.shared.generated.resources.faq_trading_t1
import echangecurrencyapp.shared.generated.resources.faq_trading_t2
import echangecurrencyapp.shared.generated.resources.faq_trading_t3
import org.jetbrains.compose.resources.stringResource

/**
 * Localized FAQ content (was the static `LedgerStrings.Faq` catalog) —
 * resolved at composition so it follows the active locale. Shared by the
 * FAQ screen and the Support hub's knowledge-base search.
 */
data class FaqEntry(val question: String, val answer: String)

/** Security accordion entries (design frame `MegbU`). */
@Composable
fun faqSecurityEntries(): List<FaqEntry> = listOf(
    FaqEntry(
        question = stringResource(Res.string.faq_sec_q1),
        answer = stringResource(Res.string.faq_sec_a1),
    ),
    FaqEntry(
        question = stringResource(Res.string.faq_sec_q2),
        answer = stringResource(Res.string.faq_sec_a2),
    ),
    FaqEntry(
        question = stringResource(Res.string.faq_sec_q3),
        answer = stringResource(Res.string.faq_sec_a3),
    ),
)

/** Account Management compact QA entries. */
@Composable
fun faqAccountEntries(): List<FaqEntry> = listOf(
    FaqEntry(
        question = stringResource(Res.string.faq_acc_q1),
        answer = stringResource(Res.string.faq_acc_a1),
    ),
    FaqEntry(
        question = stringResource(Res.string.faq_acc_q2),
        answer = stringResource(Res.string.faq_acc_a2),
    ),
    FaqEntry(
        question = stringResource(Res.string.faq_acc_q3),
        answer = stringResource(Res.string.faq_acc_a3),
    ),
    FaqEntry(
        question = stringResource(Res.string.faq_acc_q4),
        answer = stringResource(Res.string.faq_acc_a4),
    ),
)

/** Trading & Markets placeholder links. */
@Composable
fun faqTradingLinks(): List<String> = listOf(
    stringResource(Res.string.faq_trading_t1),
    stringResource(Res.string.faq_trading_t2),
    stringResource(Res.string.faq_trading_t3),
)
