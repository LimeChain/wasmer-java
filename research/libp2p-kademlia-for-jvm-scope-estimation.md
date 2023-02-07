# Libp2p Kademlia for JVM Scope & Estimation

## Libp2p’s Kademlia specification

[specs/kad-dht at master · libp2p/specs](https://github.com/libp2p/specs/tree/master/kad-dht)

## Estimation

**12-14 weeks for 1 FTE**

**8 weeks for 2 FTEs**

## Implementation scope

Libp2p’s Kademlia implementation should be integrated in https://github.com/libp2p/jvm-libp2p and consequently - implemented in **Kotlin** in order to conform to existing code in the repository. On a high-level Kademlia can be broken into several parts:

- Node initialisation
    - Bootstrap nodes
    - Network configuration
        - Replication parameter (k)
        - Alpha concurrency parameter (α)
        - Query timeout parameter
        - Content advertisement expiration parameter
- Routing table
    - Can be implemented with any data structure.
        - We should either k-buckets or XOR trie data structures in order to keep implementation differences to a minimum between each language implementation
    - Calculate XOR distance between nodes
    - Keep the routing table filled and healthy throughout time
        - We can follow the algorithm [proposed in the spec](https://github.com/libp2p/specs/tree/kad-dht-spec/kad-dht#bootstrap-process)
    - Fallback to bootstrap nodes algorithm in case peer count falls below a threshold
    - Manage stale/disconnected peers
    - Find the closest node to a key (`FIND_NODE` operation)
        - We can follow the algorithm [proposed in the spec](https://github.com/libp2p/specs/tree/kad-dht-spec/kad-dht#peer-routing)
- Value storage and retrieval
    - Storing and retrieving entries (`GET_VALUE`, `PUT_VALUE` operations)
    - Entry validation
    - Entry correction
- Content provider advertisement and discovery
    - `ADD_PROVIDER`, `GET_PROVIDERS` operations
- RPC message handling
    - Sending and receiving messages using **protobuf**
        - Implement [multiformats unsigned-varint spec](https://github.com/multiformats/unsigned-varint) that’s used to encode/decode messages
    - Executing different DHT operations based on the message type
        - `FIND_NODE`, `GET_VALUE`, `PUT_VALUE`, `ADD_PROVIDER`, `GET_PROVIDERS`
        - `PING` is not required as it’s deprecated in favour of Ping Protocol which is already implemented in jvm-libp2p
- Test cases to demonstrate correctness and functionality