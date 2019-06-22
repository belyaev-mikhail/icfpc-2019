package ru.spbstu

import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URL


data class SubmitParams(val block_num: Int, val sol_path: String, val desc_path: String)

object Server {

    val LAMBDA_PROVIDER = URL("http://localhost:8332/")

    private fun submitResults(params: SubmitParams) {
        val client = JsonRpcHttpClient(LAMBDA_PROVIDER)
        client("submit", params)
    }

    fun processBlock(path: String){
        val blockDir = File(path)
        val blockNumber = Integer.valueOf(blockDir.name)

        println(path)
        println(blockNumber)


    }


}


fun main(args: Array<String>) {
    HttpServer.create(InetSocketAddress(32345), 0).apply {
        createContext("/newBlock") { http ->
            val newBlockPath = http.requestBody.bufferedReader().readText().trim()
            Server.processBlock(newBlockPath)
        }

        start()
    }
}
