<div>
    <div>
        <img src="https://img.shields.io/maven-central/v/io.github.remmerw/nott" alt="Kotlin Maven Version" />
        <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android" alt="Badge Android" />
        <img src="https://img.shields.io/badge/Platform-JVM-8A2BE2.svg?logo=openjdk" alt="Badge JVM" />
    </div>
</div>

## Nott
Read Only DHT node (https://www.bittorrent.org/beps/bep_0043.html)

**Note:** A Nott DHT node enters directly the *Read-Only State* 

### Abstract
This extension introduces the concept of a 'read-only' DHT node for devices which are behind a
restrictive NAT where hole punching has failed and the node is essentially uncontactable 
or for devices where additional network traffic can affect device users monetarily, 
when devices have a fixed data plan, and in usability, where traffic adversely affects battery life.

### Rationale
The BitTorrent client and BitTorrent's new Bleep messaging service both use the 
"distributed hash table" (DHT)[1] for storing contact information. Each DHT node 
maintains a routing table to consult when it needs to find information from the DHT. 
When a device running a DHT node fails to hole punch, it is not reachable from outside and thus 
attempts to write to it will fail. In such cases, time can be saved if the node can declare 
that it should not be contacted. Such a state is also useful for devices which are sensitive to 
network traffic, particularly mobile devices where additional traffic has a both a monetary cost 
(when the additional network usage exceeds a fixed-bandwidth plan) and a cost in battery life 
(since the additional network traffic can prevent the radio from entering a power-saving state).

The concept of a 'read-only' DHT node saves time when nodes cannot be contacted from outside and 
reduces network traffic by not responding to queries and signaling other DHT nodes to not place 
it in their routing table and not to ping it to make see if it is still present.

### Read-Only State
When a DHT node enters the read-only state, it changes its behavior in the following ways:

It no longer responds to 'query' messages that it receives, that is messages containing a 'q' 
flag in the top-level dictionary. In each outgoing query message the read-only DHT node places a 
'ro' key in the top-level message dictionary and sets its value to 1. This will appear in the
request as '2:roi1e'. A node that receives DHT messages should inspect incoming queries for the 
'ro' flag set to 1. If it is found, the node should not add the message sender to its routing table. 
Instead it should merely service the query as usual. The reason for not adding the read-only 
node to the DHT routing table is that it will both waste time to ping the read-only node, 
since they will not respond to the ping, and it will cause the read-only node to incur 
further network traffic.

### References
[1]	BEP_0005. DHT Protocol (http://www.bittorrent.org/beps/bep_0005.html)


## Integration

```
    
kotlin {
    sourceSets {
        commonMain.dependencies {
            ...
            implementation("io.github.remmerw:nott:0.0.6")
        }
        ...
    }
}
    
```

## API

```
    @Test
    fun lookupTest(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH) // Note: it is a fake key

        withTimeoutOrNull(60 * 1000) {

            val nott = newNott(nodeId(), 6004, bootstrap())
            try {
                val channel = requestGetPeers(nott, key) {
                    5000
                }

                for (address in channel) {
                    println("Fake " + address.hostname)
                }
            } finally {
                nott.shutdown()
            }
        }

    }
```