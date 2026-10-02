package dali.hamza.shared.ui.theme

/**
 * Sovereign Ledger copy (Account cluster, design frames `qDCZE`, `MegbU`,
 * `9vaMD`, `0ZYbh`, `gbbrU`).
 *
 * Guardrail: screens reference copy through this file — the .pen file is the
 * source of truth; truncated design text was completed in its voice.
 */
object LedgerStrings {

    /** Pushed-screen top bar title (design: every sub-screen header). */
    const val APP_TITLE = "SOVEREIGN LEDGER"

    object Account {
        const val BADGE_PRIVATE = "PRIVATE CLIENT"
        const val GUEST_CLIENT = "GUEST CLIENT"
        const val BADGE_VERIFIED = "VERIFIED LEDGER"
        const val EDIT_PROFILE = "Edit Profile"
        const val EDIT_PROFILE_TITLE = "Edit Profile"
        const val EDIT_PROFILE_HINT = "Sovereign display name"

        const val PERSONAL_INFO = "Personal Information"
        const val PHONE_LABEL = "PHONE NUMBER"
        const val PHONE_VALUE = "+41 44 212 7700"
        const val RESIDENCE_LABEL = "IDENTITY RESIDENCE"
        const val RESIDENCE_VALUE = "Zurich, Switzerland"

        const val SECURITY_HEALTH = "Security Health"
        const val SECURITY_HEALTH_DESC =
            "Your vault is protected by military-grade encryption and multi-factor hardware keys."
        const val CLEARANCE = "Level 3 Clearance"
        const val CLEARANCE_SCORE = "75%"
        const val SECURITY_SECTION = "SECURITY & AUTHENTICATION"
        const val CHANGE_PASSWORD = "Change Password"
        const val BIOMETRIC_UNLOCK = "Biometric Unlock"

        /** Guest-mode local-data section (no session to log out of). */
        const val LOCAL_DATA_SECTION = "LOCAL DATA"
        const val CLEAR_LEDGER = "Clear Local Ledger"
        const val CLEAR_LEDGER_TITLE = "Clear local ledger?"
        const val CLEAR_LEDGER_MESSAGE =
            "All exchanges recorded on this device will be removed."
        const val CONFIRM_CLEAR = "Clear"

        const val PREFERENCES = "PREFERENCES"
        const val PUSH_NOTIFICATIONS = "Push Notifications"
        const val ENABLED = "Enabled"
        const val DISABLED = "Disabled"
        const val LANGUAGE = "Language"
        const val LANGUAGE_VALUE = "English (US)"

        const val VAULT_SECTION = "VAULT MANAGEMENT"
        const val LOG_OUT = "Log out from Ledger"
        const val DELETE_ACCOUNT = "Permanently Delete Account"

        const val SUPPORT_SECTION = "SUPPORT & INFORMATION"
        const val SUPPORT_CARD_TITLE = "Support & Knowledge Base"
        const val SUPPORT_CARD_DESC =
            "Access our FAQ, contact support, and explore helpful resources."
        const val SUPPORT_CARD_CTA = "Explore Support Hub"

        // ---- guest mode / login (coming soon) ----
        const val LOGIN = "Sovereign Login"
        const val COMING_SOON = "COMING SOON"
        const val DATA_PLAN = "Data Plan"
        const val DATA_PLAN_NOTE =
            "Guest plan refreshes rates hourly. Sovereign login unlocks realtime data."
        fun dataPlanGuest() = "Hourly rates · Guest"
        fun dataPlanSovereign() = "Realtime · Sovereign"

        const val VERSION = "V1.0.0"
        fun footer(lastSync: String) = "SOVEREIGN LEDGER NODE $VERSION • LAST SYNC: $lastSync"

        const val DIALOG_CANCEL = "Cancel"
        const val DIALOG_SAVE = "Save"
    }

    /** One FAQ entry (question + answer). */
    data class FaqEntry(val question: String, val answer: String)

    object Faq {
        const val HERO_TITLE = "How can we assist you?"
        const val HERO_DESC =
            "Search our knowledge base or browse categories below for guidance on rates, security and account management."
        const val SEARCH_HINT = "Search our knowledge base..."
        const val NO_RESULTS_TITLE = "No results"
        fun noResults(query: String) = "Nothing matches \"$query\". Try another term."

        const val SECURITY_TITLE = "Security"
        val security = listOf(
            FaqEntry(
                question = "How is my private key material secured?",
                answer = "Sovereign Ledger utilizes institutional-grade Multi-Party " +
                    "Computation (MPC) distributed across globally isolated secure " +
                    "enclaves. Your key is never constructed in whole, rendering " +
                    "single-point breaches mathematically impossible.",
            ),
            FaqEntry(
                question = "Setting up Hardware Key Authentication",
                answer = "Pair your hardware key from Security & Authentication → " +
                    "Biometric Unlock. Insert the device, hold its button for five " +
                    "seconds, and confirm the pairing code shown in your ledger.",
            ),
            FaqEntry(
                question = "What to do if my primary device is compromised?",
                answer = "Freeze the session instantly from any secondary device, then " +
                    "revoke the compromised key. Sovereign clients may also request a " +
                    "concierge-assisted device rotation within one hour.",
            ),
        )

        const val TRADING_TITLE = "Trading & Markets"
        val trading = listOf(
            "Execution Slippage Explained",
            "Dark Pool Access Tiers",
            "Settlement Timeframes (T+1)",
        )

        const val ACCOUNT_TITLE = "Account Management"
        const val ACCOUNT_DESC =
            "Everything related to tier upgrades, compliance documentation, and multi-signature approvals."
        val account = listOf(
            FaqEntry(
                question = "Upgrading to Sovereign Tier",
                answer = "Requirements include verified assets over $1M and an initial " +
                    "compliance review. Upgrades are processed within 48 hours.",
            ),
            FaqEntry(
                question = "Adding a Co-Signatory",
                answer = "Initiate multi-sig setup via the Security Center. The secondary " +
                    "party must also complete identity verification.",
            ),
            FaqEntry(
                question = "Generating Tax Statements",
                answer = "End-of-year reports are automatically generated in the Audit " +
                    "Logs section by January 15th.",
            ),
            FaqEntry(
                question = "Wire Transfer Whitelists",
                answer = "All outbound destinations must be pre-approved. Changes " +
                    "require a 48-hour cooling period.",
            ),
        )

        const val CTA_TITLE = "Still require assistance?"
        const val CTA_DESC =
            "Our dedicated concierge team is available 24/7 for Sovereign Tier members."
        const val CTA_PRIMARY = "Contact Concierge"
        const val CTA_SECONDARY = "Secure Message"
    }

    object Support {
        const val HERO_TITLE = "How can we help?"
        const val SEARCH_HINT = "Search articles, guides, and policies..."

        const val FAQ_TITLE = "FAQ"
        const val FAQ_DESC = "Quick answers for common questions."
        const val CONTACT_TITLE = "Contact Us"
        const val CONTACT_DESC = "Chat with our premium support team."
        const val FEEDBACK_TITLE = "Feedback"
        const val FEEDBACK_DESC = "Help us refine your ledger experience."

        const val LEGAL_TITLE = "Resources & Legal"
        val legal = listOf(
            "Terms & Conditions",
            "Privacy Policy",
            "About Sovereign Ledger",
            "Rate App",
            "Share with Colleagues",
        )

        const val VERSION = "SOVEREIGN LEDGER V2.4.0-STABLE"
        const val COPYRIGHT = "© 2024 Sovereign Wealth Systems AG."
    }

    object Contact {
        const val HERO_TITLE = "Concierge Support"
        const val HERO_DESC =
            "Discreet, priority assistance for Sovereign clients. Select your preferred channel below or dispatch a secure message directly to your designated liaison."

        const val LIVE_CHAT = "Live Chat"
        const val PREMIUM = "PREMIUM"
        const val LIVE_CHAT_DESC =
            "Immediate, encrypted text dialogue with a senior relationship manager."
        const val INITIATE = "Initiate Session"

        const val PHONE = "Phone Concierge"
        const val PHONE_DESC = "24/7 global access via secure encrypted lines."
        const val PHONE_VALUE = "+41 44 555 12 34"

        const val DISPATCH = "Secure Dispatch"
        const val DISPATCH_DESC = "Guaranteed acknowledgement within 1 hour."
        const val DISPATCH_VALUE = "liaison@sovereign.vault"

        const val FORM_TITLE = "Secure Message Form"
        const val SUBJECT_LABEL = "Subject Category"
        const val SUBJECT_PLACEHOLDER = "Select topic..."
        val topics = listOf(
            "Rates & Market Data",
            "Account & Verification",
            "Security Concern",
            "Feature Request",
        )
        const val DESCRIPTION_LABEL = "Detailed Description"
        const val DESCRIPTION_PLACEHOLDER = "Please provide specific details regarding your request..."
        const val SUBMIT = "Dispatch Message"

        const val SUCCESS_TITLE = "Message Dispatched"
        const val SUCCESS_DESC =
            "Your secure message is on its way to your liaison. Expect an acknowledgement within 1 hour."
    }

    object Feedback {
        const val HERO_TITLE = "Refine the Instrument."
        const val HERO_DESC =
            "Your insights shape the architecture of the Sovereign Ledger. Tell us how we can elevate your private banking experience."

        const val RATING_LABEL = "Overall Experience"
        const val CATEGORY_LABEL = "Primary Category"
        val categories = listOf("Performance", "New Feature", "Support Request", "UI Design")
        const val SUGGESTIONS_LABEL = "Detailed Suggestions"
        const val OPTIONAL = "Optional"
        const val SUGGESTIONS_PLACEHOLDER = "Describe your experience or feature request..."
        const val SUBMIT = "Submit Intelligence"

        const val SUCCESS_TITLE = "Intelligence Received"
        const val SUCCESS_DESC =
            "Thank you — your insights refine the instrument for every Sovereign client."
    }
}
