



internal fun ByteArray.toLongKey(length: Int): Long {
    require(length in 1..8) { "Length must be between 1 and 8 bytes for a Long value." }
    require(this.size >= length) { "Array is too small for the requested length." }

    var result = 0L
    for (i in 0 until length) {
        val byteValue = this[i].toLong() and 0xFFL
        result = (result shl 8) or byteValue
    }
    return result
}
