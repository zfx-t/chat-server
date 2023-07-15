
import kotlinx.coroutines.*


fun main(): Unit = runBlocking {
    println("服务器已经开启")
    launch {
        while (true) {
            server.apply {
                println("等待连接")
                val client = withContext(Dispatchers.IO) { server.serversocket.accept() }
                //使用了 Dispatchers.IO 调度器，将 accept() 操作放在了 IO 线程池中进行，从而避免了阻塞当前的协程。
                println("有新用户连接")
                launch {
                    withContext(Dispatchers.IO) { handleClient(client) }
                }
             }
        }
    }


}


object Type {
    val TYPE_MESSAGE = 0
    val TYPE_IMAGE = 1
}

/**
 * 全局变量
 */
val port = 12340
