package xmodem.files

class FileOpenException(path: String) : Exception("File $path couldn't be opened!")