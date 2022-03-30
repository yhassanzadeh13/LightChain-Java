package protocol.engines;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Encapsulates tests for ingest engine implementation.
 */
public class IngestEngineTest {
  // TODO: a single individual test function for each of these scenarios:
  // 1. Happy path of receiving a validated single block.
  // 2. Happy path of receiving two validated blocks sequentially.
  // 3. Happy path of receiving two validated blocks concurrently.
  // 4. Happy path of receiving two duplicate validated blocks sequentially
  //    (the second duplicate block should be discarded).
  // 5. Happy path of receiving two duplicate validated blocks concurrently
  //    (the second duplicate block should be discarded).
  // 6. Happy path of receiving a validated block with shared transactions in pendingTx.
  // 7. Happy path of receiving two validated blocks concurrently that each have some transactions in pendingTx
  //    (disjoint sets of transactions).
  // 8. Happy path of receiving two validated blocks concurrently that each have some transactions in pendingTx
  //    (overlapping sets of transactions).
  // 9. Happy path of receiving an already ingested validated block (i.e., block already added to blocks database),
  //    second block should be discarded right away.
  // 10. Happy path of receiving a validated transaction.
  // 11. Happy path of receiving two validated transactions sequentially.
  // 12. Happy path of receiving two validated transactions concurrently.
  // 13. Happy path of receiving a duplicate pair of validated transactions sequentially.
  // 14. Happy path of receiving two duplicate pair of validated transactions concurrently.
  // 15. Happy path of receiving a validated transaction that its id already exists in txHash.
  // 16. Happy path of receiving a validated transaction that its id already exists in pendingTx.
  // 17. Unhappy path of receiving an entity that is neither a validated block nor a validated transaction.
  // 18. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does not contain that transaction).
  // 19. Happy path of receiving a validated transaction and a validated block concurrently
  //     (block does contain the transaction).

  @Test
  public void testConcurrentSample() {
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);

    Thread t = new Thread(() -> {
      // implement body of thread.
      // if some error happens that leads to test failure:
      // threadErrorCount.getAndIncrement();

      done.countDown();
    });

    // run threads
    t.start();

    try {
      boolean doneOnTime = done.await(1, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }

    Assertions.assertEquals(0, threadErrorCount.get());
  }
}
