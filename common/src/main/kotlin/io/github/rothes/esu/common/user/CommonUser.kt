package io.github.rothes.esu.common.user

import io.github.rothes.esu.core.user.User

abstract class CommonUser : User {

    override var language: String?
        get() = languageUnsafe ?: clientLocale
        set(value) {
            languageUnsafe = value
        }
    override var colorScheme: String?
        get() = colorSchemeUnsafe
        set(value) {
            colorSchemeUnsafe = value
        }

    override var languageUnsafe: String? = null
        set(value) {
            field = value
            userDataDirty = true
        }
    override var colorSchemeUnsafe: String? = null
        set(value) {
            field = value
            userDataDirty = true
        }

    override var userDataDirty: Boolean = false

}
