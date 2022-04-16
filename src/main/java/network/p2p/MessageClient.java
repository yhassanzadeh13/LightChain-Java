/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.p2p;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import model.Entity;
import model.lightchain.Identifier;
import modules.codec.JsonEncoder;
import network.p2p.proto.Message;
import network.p2p.proto.MessengerGrpc;

/**
 * Includes the implementation of client side functionality of gRPC requests.
 */
public class MessageClient {
  private static final Logger logger = Logger.getLogger(MessageClient.class.getName());
  private final MessengerGrpc.MessengerStub asyncStub;

  /**
   * Construct client for accessing MessageServer using the existing channel.
   */
  public MessageClient(Channel channel) {
    asyncStub = MessengerGrpc.newStub(channel);
  }

  /**
   * Async client-streaming.
   */
  public void deliver(Entity entity, Identifier target, String channel) throws InterruptedException {
    final CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {

      @Override
      public void onNext(Empty value) {

      }

      @Override
      public void onError(Throwable t) {
        System.err.println("deliver Failed: " + Status.fromThrowable(t));
        finishLatch.countDown();
      }

      @Override
      public void onCompleted() {
        finishLatch.countDown();
      }
    };

    StreamObserver<Message> requestObserver = asyncStub.deliver(responseObserver);
    try {

      JsonEncoder encoder = new JsonEncoder();

      Message message = Message.newBuilder()
          .setOriginId(ByteString.copyFromUtf8("" + channel))
          .setPayload(ByteString.copyFrom(encoder.encode(entity).getBytes()))
          .setType(encoder.encode(entity).getType())
          .addTargetIds(ByteString.copyFromUtf8(
              channel))
          .build();
      requestObserver.onNext(message);

      if (finishLatch.getCount() == 0) {
        // RPC completed or errored before we finished sending.
        // Sending further requests won't error, but they will just be thrown away.
        return;
      }

    } catch (RuntimeException e) {
      // Cancel RPC
      requestObserver.onError(e);
      throw e;
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      System.err.println("deliver can not finish within 1 minutes");
    }
  }
}
