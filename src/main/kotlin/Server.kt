import cn.hutool.json.JSONUtil
import kotlinx.coroutines.delay
import java.net.ServerSocket
import java.net.Socket
import java.util.*

object server {
    val serversocket: ServerSocket = ServerSocket(port)
    val socketMap = mutableMapOf<String, Socket>()
    val userMap = mutableMapOf<String, String>()
}

suspend fun handleClient(socket: Socket) {
    var isAlive = true
    println("添加新用户成功")
    //初始化定义名字
    val name = socket.getInputStream().bufferedReader().readLine()
    var userId = UUID.randomUUID().toString()
    server.socketMap.put(userId, socket)
    server.userMap.put(userId, name)
    println(server.userMap)
    while (isAlive) {
        val order = socket.getInputStream().bufferedReader().readLine()
        when (order) {
            "1" -> {
                println("有用户修改昵称")
                var user = JSONUtil.parseObj(socket.getInputStream().bufferedReader().readLine())
                server.userMap.keys.forEach {
                    if (server.userMap[it] == user.get("oldName")) {
                        server.userMap[it] = user.get("newName").toString()
                    }
                }
                println("修改昵称成功")
            }

            "2" -> {
                println("有用户请求用户列表")

                val writer = socket.getOutputStream().bufferedWriter()

                writer.write(server.userMap.values.toString() + "\n")
                writer.flush()
                println("返回用户列表成功")
            }

            "3" -> {
                println("有用户进行聊天")
                var chat = JSONUtil.parseObj(socket.getInputStream().bufferedReader().readLine())
                val sender = chat.get("sender")
                val name = chat.get("receiver")
                val msg = chat.get("msg")
                for (id in server.socketMap.keys) {
                    if (server.userMap[id] == name) {
                        val writer =
                            server.socketMap[id]!!.getOutputStream().bufferedWriter()
                        writer.write("${Type.TYPE_MESSAGE}\n")
                        writer.flush()
                        delay(200)
                        writer.write("\"" + sender.toString() + "\"" + ":" + msg.toString() + "\n")
                        writer.flush()
                        break
                    }
                }
                println("发送结束")
            }

            "4" -> {

                fun sendImage(receiver: Socket, fileSize: Int) {
                    val bytes = ByteArray(1024)
                    println(fileSize)
                    var i = 0
                    do {
                        var len = socket.getInputStream().read(bytes)
                        if (len != -1) {
                            receiver.getOutputStream().write(bytes, 0, len)
                            i += len
                            println("已完成${i * 1.0 / fileSize}")
                        }
                    } while (len != -1 && i < fileSize)
                    println("已经传输完成")
                }
                println("有用户发送图片")
                val msg = JSONUtil.parseObj(socket.getInputStream().bufferedReader().readLine())
                val sender = msg.get("sender")
                val name = msg.get("receiver")
                val fileName = msg.get("fileName")
                val fileSize = msg.get("fileSize").toString().toInt()
                val receiver: Socket
                for (id in server.socketMap.keys) {
                    if (server.userMap[id] == name) {
                        receiver = server.socketMap[id]!!
                        receiver.getOutputStream().write("${Type.TYPE_IMAGE}\n".toByteArray())
                        receiver.getOutputStream().flush()
                        delay(200)
                        receiver.getOutputStream().write((JSONUtil.toJsonStr(msg) + "\n").toByteArray())
                        receiver.getOutputStream().flush()
                        delay(2000)
                        sendImage(receiver, fileSize)
                        break
                    }
                }

                println("发送结束")
            }

            "5" -> {
                val name = socket.getInputStream().bufferedReader().readLine()
                println(name)
                for (id in server.socketMap.keys) {
                    if (server.userMap[id] == name) {
                        server.socketMap.remove(id)
                        server.userMap.remove(id)
                        break
                    }
                }
                socket.getOutputStream().close()
                socket.close()
                delay(100)
                isAlive=false
                println("用户$name 断开连接")
            }
        }
    }
}