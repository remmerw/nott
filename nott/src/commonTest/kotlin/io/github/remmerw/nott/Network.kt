
import java.net.InetAddress



fun internet() : Boolean{
        return try {
            // Ping Google's DNS server (timeout after 2000ms)
            InetAddress.getByName("8.8.8.8").isReachable(2000)
        } catch (e: Exception) {
            false
        }
    }