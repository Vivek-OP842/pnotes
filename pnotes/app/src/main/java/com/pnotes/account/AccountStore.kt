package com.pnotes.account

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Who is signed in, and whether this install holds a verified lifetime purchase for them. */
data class AccountState(
    val account: Account? = null,
    val premium: Boolean = false,
    /**
     * The entitlement came from the local test harness rather than a verified purchase. The UI says
     * so; nothing may present it as a sale.
     */
    val premiumIsLocal: Boolean = false,
) {
    val signedIn: Boolean get() = account != null

    /** The letter shown on the avatar control; "V" is the workspace initial used when signed out. */
    val avatarLabel: String
        get() = account?.displayName?.trim()?.firstOrNull()?.uppercase()
            ?: account?.email?.firstOrNull()?.uppercase()
            ?: "V"
}

/**
 * The signed-in identity and the premium entitlement, persisted across launches.
 *
 * Deliberately separate from note storage: notes are local and offline, and stay fully usable while
 * signed out. Nothing here touches the `.pnote` files or the browse root.
 *
 * Entitlement is recorded per account id rather than as one global flag, so signing out and back in
 * restores the purchase to the account that made it, and a second account on the same device does
 * not inherit it.
 */
class AccountStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    /**
     * Records a successful sign-in. Premium is granted only if this account already owns it, so
     * signing in never by itself unlocks the paid tier.
     */
    fun signIn(account: Account) {
        prefs.edit().putString(KEY_ACCOUNT, accountToJson(account).toString()).apply()
        _state.value = snapshot(account)
    }

    /**
     * Clears the session. Entitlement is kept against the account id for a later sign-in, so signing
     * back in restores what was bought. Note that this deliberately does not clear a local test
     * entitlement: that one belongs to the device, and only the test harness may remove it.
     */
    fun signOut() {
        prefs.edit().remove(KEY_ACCOUNT).apply()
        _state.value = snapshot(account = null)
    }

    /**
     * Grants lifetime premium. Call only after a purchase has been verified; while signed out this
     * is a no-op, because a Play purchase always belongs to an account.
     */
    fun grantPremium() {
        val account = _state.value.account ?: return
        prefs.edit().putStringSet(KEY_ENTITLED, entitledIds() + account.id).apply()
        _state.value = snapshot(account)
    }

    /** Drops the entitlement for the signed-in account, e.g. after a refund or a failed restore. */
    fun revokePremium() {
        val account = _state.value.account ?: return
        prefs.edit().putStringSet(KEY_ENTITLED, entitledIds() - account.id).apply()
        _state.value = snapshot(account)
    }

    /**
     * Records a purchase made through the local test harness, which has no Play account behind it.
     * Device-scoped on purpose, and always reported as [AccountState.premiumIsLocal].
     */
    fun grantLocalEntitlement() {
        prefs.edit().putBoolean(KEY_LOCAL_PREMIUM, true).apply()
        _state.value = snapshot(_state.value.account)
    }

    /** Withdraws a local test entitlement, e.g. to re-run the purchase flow from scratch. */
    fun revokeLocalEntitlement() {
        prefs.edit().remove(KEY_LOCAL_PREMIUM).apply()
        _state.value = snapshot(_state.value.account)
    }

    private fun entitledIds(): Set<String> = prefs.getStringSet(KEY_ENTITLED, emptySet()).orEmpty()

    private fun locallyEntitled(): Boolean = prefs.getBoolean(KEY_LOCAL_PREMIUM, false)

    /** Rebuilds the whole state from storage, so every field always agrees with what is persisted. */
    private fun snapshot(account: Account?): AccountState {
        val paid = account != null && entitledIds().contains(account.id)
        val local = locallyEntitled()
        return AccountState(
            account = account,
            premium = paid || local,
            premiumIsLocal = local && !paid,
        )
    }

    private fun restore(): AccountState {
        val stored = prefs.getString(KEY_ACCOUNT, null)
        val account = stored?.let(::accountFromJson)
        return snapshot(account)
    }

    private fun accountToJson(account: Account): JSONObject = JSONObject().apply {
        put("id", account.id)
        put("email", account.email)
        put("name", account.displayName)
        put("photo", account.photoUrl ?: JSONObject.NULL)
    }

    private fun accountFromJson(raw: String): Account? = runCatching {
        val json = JSONObject(raw)
        Account(
            id = json.optString("id"),
            email = json.optString("email"),
            displayName = json.optString("name"),
            photoUrl = json.optString("photo").takeIf { it.isNotEmpty() && it != "null" },
        )
    }.getOrNull()?.takeIf { it.id.isNotEmpty() }

    companion object {
        private const val PREFS = "pnotes.account"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_ENTITLED = "entitledAccountIds"
        private const val KEY_LOCAL_PREMIUM = "localTestPremium"

        @Volatile
        private var instance: AccountStore? = null

        /** The process-wide store. Cheap to call from composition. */
        fun get(context: Context): AccountStore =
            instance ?: synchronized(this) {
                instance ?: AccountStore(context).also { instance = it }
            }
    }
}

/**
 * The app's [AccountStore]. Null in previews and tests, so read it defensively; every consumer has
 * a sensible unsigned-in fallback.
 */
val LocalAccountStore = staticCompositionLocalOf<AccountStore?> { null }

/** Opens the sign-in surface. A no-op by default, so any composable may call it unconditionally. */
val LocalOpenSignIn = staticCompositionLocalOf<() -> Unit> { {} }

/** Opens the premium (upgrade) surface. A no-op by default. */
val LocalOpenPremium = staticCompositionLocalOf<() -> Unit> { {} }
