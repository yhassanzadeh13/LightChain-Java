package networking;

import model.exceptions.LightChainNetworkingException;
import network.Conduit;
import network.Network;
import org.junit.jupiter.api.Test;
import protocol.Engine;
import unittest.fixtures.EntityFixture;

import java.util.Iterator;

public class StubNetworkTest {


    // TODO: add a test for each of the following scenarios:
    // Use mock engines.
    // 1. Engine A (on one stub network) can send message to Engine B (on another stub network) through its StubNetwork, and the message is received by Engine B.
    // 2. Engine A can CONCURRENTLY send 100 messages to Engine B through its StubNetwork, and ALL messages received by Engine B.
    // 3. Extend case 2 with Engine B also sending a reply message to Engine A for each received messages and all replies
    // are received by Engine A.
    // 4. Engines A and B on one StubNetwork can CONCURRENTLY send 100 messages to Engines C and D on another StubNetwork (A -> C) and (B -> D), and each Engine only
    // receives messages destinated for it (C receives all messages from A) and (D receives all messages from B). Note that A and C must be on the same channel, and B
    // and B must be on another same channel.
    // 5. Stub network throws an exception if an engine is registering itself on an already taken channel.

    @Test
    void test() throws LightChainNetworkingException {
        Hub hub = new Hub();
        StubNetwork stubNetwork1 = new StubNetwork(hub);
        StubNetwork stubNetwork2 = new StubNetwork(hub);
        String channelA1="ChannelA1";
        String channelB1="ChannelB1";
        Engine A1 = new MockEngine();
        Engine B1 = new MockEngine();
        stubNetwork1.register(A1, channelA1);
        stubNetwork2.register(B1,channelB1);
        EntityFixture e1 = new EntityFixture();
        EntityFixture e2 = new EntityFixture();
        StubNetworkThread t1 = new StubNetworkThread(stubNetwork1,stubNetwork2,e1,channelA1);
        t1.start();
        //c1.unicast(e1, stubNetwork2.id());
        //System.out.println(e1.id());
        //System.out.println(B1);
    }
}
