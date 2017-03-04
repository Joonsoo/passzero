package com.giyeok.passzero

object PasswordUtils {
    def checkMasterPassword(password: String): Option[String] = {
        if (password.length < 8) {
            Some("Password must be longer than 8")
        } else {
            val numbers = ('0' to '9').toSet
            val hasNumbers = password exists { numbers contains }
            val lowercases = ('a' to 'z').toSet
            val hasLowercase = password exists { lowercases contains }
            val uppercases = ('A' to 'Z').toSet
            val hasUppercase = password exists { uppercases contains }
            if (!(hasNumbers && hasLowercase && hasUppercase)) {
                Some("Password must have number, uppercase, and lowercase alphabets")
            } else {
                None
            }
        }
        // TODO
        None
    }
}
