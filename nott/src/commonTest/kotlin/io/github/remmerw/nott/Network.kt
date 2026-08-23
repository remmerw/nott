package io.github.remmerw.nott


import java.net.InetAddress

fun internet() : Boolean{
        return try {
            
            InetAddress.getByName("8.8.8.8").isReachable(2000)
        } catch (e: Exception) {
            false
        }
    }