@file:Suppress("EXPERIMENTAL_API_USAGE")

package xmodem

import platform.windows.*

val TRUEu: UInt = TRUE.toUInt()
val FALSEu: UInt = FALSE.toUInt()

enum class CBR {
    BPS_110,
    BPS_300,
    BPS_600,
    BPS_1200,
    BPS_2400,
    BPS_4800,
    BPS_9600,
    BPS_14400,
    BPS_19200,
    BPS_38400,
    BPS_57600,
    BPS_115200,
    BPS_128000,
    BPS_256000;


    val value: UInt  = name.substringAfterLast("_").toUInt()
}

enum class StopBits(winValue: Int) {
    ONE(ONESTOPBIT), ONE5(ONE5STOPBITS), TWO(TWOSTOPBITS);

    val winValue = winValue.toUByte()
}

enum class Parity(winValue: Int) {

    NO(NOPARITY), EVEN(EVENPARITY), MARK(MARKPARITY), ODD(ODDPARITY), SPACE(SPACEPARITY);

    val winValue = winValue.toUByte()
}