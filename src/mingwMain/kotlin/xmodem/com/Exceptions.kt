package xmodem.com

class ComOpenFailedException(name: String) : Exception("$name port could not be opened!")

class ComSettingsApplyFailedException(name: String) : Exception("Setting couldn't be applied to $name port")