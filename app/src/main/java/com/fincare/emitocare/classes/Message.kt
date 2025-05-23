package com.fincare.emitocare

data class Message(
    var text: String? = null,
    var sender: String? = null,
    var timestamp: Long? = null,
    var isUser: Boolean = false
) {
    constructor() : this(null, null, null, false)
}
