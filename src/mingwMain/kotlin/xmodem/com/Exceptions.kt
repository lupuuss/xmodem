package xmodem.com

import platform.windows.DWORD

class ComOpenFailedException(name: String, error: DWORD) : Exception("$name port could not be opened! Error code: $error")

class ComSettingsApplyFailedException(
    name: String, settingsName: String, error: DWORD
) : Exception("Settings ($settingsName) couldn't be applied to $name port! Error code: $error")