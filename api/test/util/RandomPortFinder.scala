package util

import java.io.IOException
import java.net.{InetSocketAddress, Socket}

object RandomPortFinder {

  def getRandomPort: Int = {
    Stream
      .continually(randomPort)
      .dropWhile(port => !checkIfPortIsFree("localhost", port))
      .head
  }

  private def randomPort: Int = {
    10000 + scala.util.Random.nextInt(55000)
  }

  private def checkIfPortIsFree(host: String, port: Int): Boolean = {
    val s = new Socket()
    try {
      s.connect(new InetSocketAddress(host, port), 1000)
      false
    } catch {
      case _: IOException => true
    } finally {
      s.close()
    }
  }

}
