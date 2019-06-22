package ru.spbstu

import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URL
import java.nio.file.Path


data class SubmitParams(val block_num: Int, val sol_path: String, val desc_path: String)

object Server {

    val LAMBDA_PROVIDER = URL("http://localhost:8332/")

    private fun submitResults(params: SubmitParams) {
        val client = JsonRpcHttpClient(LAMBDA_PROVIDER)
        client("submit", params)
    }

    fun processBlock(path: String) {
        val blockDir = File(path)
        val blockNumber = Integer.valueOf(blockDir.name)

        println(path)
        println(blockNumber)

        Main.main(listOf(
                "--sol-folder", "forLambdaChain/$blockNumber",
                "--map", Path.of(blockDir.path, "task.desc").toFile().absolutePath,
                "--threads", "7",
                "--use-absolute-map-path"
        ))

        val block_num = blockNumber
        val sol_path = "forLambdaChain/$blockNumber/task.sol"
        val desc_path = ""

        submitResults(SubmitParams(block_num, sol_path, desc_path))
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
