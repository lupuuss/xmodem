package xmodem.com

import platform.windows.DWORD

open class ComException(msg: String, error: DWORD) : Exception("$msg Error code: $error")

class ComOpenFailedException(name: String, error: DWORD) : ComException("$name port could not be opened!", error)

class ComSettingsApplyFailedException(
    name: String, settingsName: String, error: DWORD
) : ComException("Settings ($settingsName) couldn't be applied to $name port!", error)

class ComPurgeFailedException(name: String, error: DWORD) : ComException("$name port purge failed!", error)