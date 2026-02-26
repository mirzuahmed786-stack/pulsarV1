package com.elementa.wallet.rpc

import java.io.IOException

class RpcException(message: String, cause: Throwable? = null) : IOException(message, cause)
