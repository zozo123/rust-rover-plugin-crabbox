package com.incredibuild.crabbox

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

object CrabboxSecrets {
    private const val ISLO_API_KEY = "ISLO_API_KEY"

    fun getIsloApiKey(): String {
        return PasswordSafe.instance.getPassword(isloCredentialAttributes()).orEmpty()
    }

    fun setIsloApiKey(value: String) {
        val normalized = value.trim()
        val credentials = if (normalized.isBlank()) {
            null
        } else {
            Credentials(ISLO_API_KEY, normalized)
        }
        PasswordSafe.instance.set(isloCredentialAttributes(), credentials)
    }

    fun hasIsloApiKey(): Boolean = getIsloApiKey().isNotBlank()

    fun withConfiguredSecrets(env: Map<String, String>): Map<String, String> {
        val isloApiKey = getIsloApiKey()
        if (isloApiKey.isBlank() || env.containsKey(ISLO_API_KEY)) {
            return env
        }

        return env + (ISLO_API_KEY to isloApiKey)
    }

    private fun isloCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(generateServiceName("Crabbox Runner", ISLO_API_KEY))
    }
}
