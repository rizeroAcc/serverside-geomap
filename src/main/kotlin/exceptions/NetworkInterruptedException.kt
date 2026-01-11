package com.mapprjct.exceptions

class NetworkInterruptedException(val details : String) : BaseAppException() {
    override val shortMessage: String
        get() = "Network Interrupted"
    override val detailedMessage: String
        get() = details
}