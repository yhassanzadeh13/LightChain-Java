package protocol.engines;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import model.Entity;
import model.crypto.PrivateKey;
import model.crypto.Signature;
import model.exceptions.LightChainDistributedStorageException;
import model.lightchain.*;
import model.local.Local;
import network.Channels;
import network.Network;
import network.NetworkAdapter;
import networking.MockConduit;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import state.Snapshot;
import state.State;
import storage.Identifiers;
import unittest.fixtures.*;
import unittest.fixtures.ValidatedTransactionFixture;

/**
 * Encapsulates tests for validator engine.
 */
public class ValidatorEngineTest {
  // a single individual test function for each of these scenarios:
  // Note: except when explicitly mentioned, always assume the input block or transaction is assigned to this node
  // i.e., mock the assigner for that.
  //++ 1. Happy path of receiving a valid single block.
  // 2. Happy path of receiving two valid blocks sequentially.
  // 3. Happy path of receiving two valid blocks concurrently.
  // 4. Happy path of receiving two duplicate blocks sequentially (the second duplicate block should be discarded).
  // 5. Happy path of receiving two duplicate blocks concurrently (the second duplicate block should be discarded).
  // 6. Happy path of receiving a valid block with shared transactions in pendingTx.
  // 7. Happy path of receiving an already validated block,
  //    second block should be discarded right away.
  // 8.  Unhappy path of receiving a transaction and block (sequentially) that is not assigned to this node.
  // 9.  Unhappy path of receiving a transaction and block (concurrently) that is not assigned to this node.
  //++ 10. Happy path of receiving a valid transaction.
  //++ 11. Happy path of receiving two valid transactions sequentially.
  //++ 12. Happy path of receiving two valid transactions concurrently.
  //++ 13. Happy path of receiving a duplicate pair of valid transactions sequentially.
  //+ 14. Happy path of receiving a duplicate pair of valid transactions concurrently.
  //+ TODO: isn't 15 same with 13? 15. Happy path of receiving a transaction that already been validated (second transaction should be discarded).
  //++ 16. Unhappy path of receiving an entity that is neither a block nor a transaction.
  // 17. Happy path of receiving a transaction and a block concurrently (block does not contain that transaction).
  // 18. Happy path of receiving a transaction and a block concurrently (block does contain the transaction).
  //++ 19. Unhappy path of receiving an invalid transaction (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid transaction should be discarded without sending back a signature to its sender.
  // 19. Unhappy path of receiving an invalid block (one test per each validation criteria, e.g., correctness,
  //     soundness, etc). Invalid block should be discarded without sending back a signature to its proposer.
  private static final Random random = new Random();
  private ValidatorEngine engine;
  private Identifier localId;
  private PrivateKey localPrivateKey;
  private Local local;
  private Network network;
  private MockConduit blockConduit;
  private MockConduit txConduit;
  private Identifiers seenEntities;
  private Block block1;
  private Block block2;
  private State state;
  private Snapshot snapshot1;
  private Snapshot snapshot2;

  private ArrayList<Account> accounts1;
  private ArrayList<Account> accounts2;

  @BeforeEach
  public void setup() {
    // Arrange
    /// Local node
    localId = IdentifierFixture.newIdentifier();
    localPrivateKey = KeyGenFixture.newKeyGen().getPrivateKey();
    local = new Local(localId, localPrivateKey);

    /// Network
    network = mock(Network.class);
    NetworkAdapter networkAdapter = mock(NetworkAdapter.class);
    blockConduit = new MockConduit(Channels.ProposedBlocks, networkAdapter);
    txConduit = new MockConduit(Channels.ProposedTransactions, networkAdapter);

    block1 = BlockFixture.newBlock();
    block2 = BlockFixture.newBlock(block1.id(), block1.getHeight() + 1);

    /// State and Snapshots
    state = mock(State.class);
    snapshot1 = mock(Snapshot.class);
    snapshot2 = mock(Snapshot.class);

    when(snapshot1.getReferenceBlockId()).thenReturn(block1.id());
    when(snapshot2.getReferenceBlockId()).thenReturn(block2.id());

    when(snapshot1.getReferenceBlockHeight()).thenReturn((long) block1.getHeight());
    when(snapshot2.getReferenceBlockHeight()).thenReturn((long) block2.getHeight());

    when(state.atBlockId(block1.id())).thenReturn(snapshot1);
    when(state.atBlockId(block2.id())).thenReturn(snapshot2);

    /// Accounts
    /// Create accounts for the snapshot including an account with the local id.
    ArrayList<Account>[] a = AccountFixture.newAccounts(localId, block1.id(), block2.id(), 10, 10);
    accounts1 = a[0];
    accounts2 = a[1];
    when(snapshot1.all()).thenReturn(accounts1);
    when(snapshot2.all()).thenReturn(accounts1);

    for (int i = 0; i < accounts1.size(); i++) {
      when(snapshot1.getAccount(snapshot1.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
      when(snapshot2.getAccount(snapshot2.all().get(i).getIdentifier())).thenReturn(accounts1.get(i));
    }
    /*
    for (Account account: snapshot1.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }*/
    for (Account account : snapshot2.all()) {
      when(state.atBlockId(account.getLastBlockId())).thenReturn(snapshot1);
    }

    seenEntities = mock(Identifiers.class);
    when(state.last()).thenReturn(snapshot2);

  }

  @Test
  public void testReceiveOneValidTransaction() {
    setup();
    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testReceiveTwoValidTransactionsSequentially() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Transaction transaction = TransactionFixture.newTransaction(
          block2.id(),
          snapshot2.all().get(i).getIdentifier(),
          snapshot2.all().get(i + 1).getIdentifier(),
          signerId);
      transactions.add(transaction);
      when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
          .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
      snapshot2.all().get(i).setBalance(transaction.getAmount() * 10 + 1);
    }

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transactions.get(i));
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTwoValidTransactionsConcurrently() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Transaction transaction = TransactionFixture.newTransaction(
          block2.id(),
          snapshot2.all().get(i).getIdentifier(),
          snapshot2.all().get(i + 1).getIdentifier(),
          signerId);
      transactions.add(transaction);
      when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
          .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
      snapshot2.all().get(i).setBalance(transaction.getAmount() * 10 + 1);
    }

    // Act
    /// Create two threads that will process the transactions concurrently.
    int concurrencyDegree = 2;
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      validationThreads[i] = new Thread(() -> {
        try {
          engine.process(transactions.get(finalI));
        } catch (IllegalArgumentException ex) {
          threadErrorCount.incrementAndGet();
        }
        done.countDown();
      });
    }

    ///  Run Threads
    for (Thread t : validationThreads) {
      t.start();
    }

    /// Assert done on time and got no errors
    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }
    Assertions.assertEquals(0, threadErrorCount.get());


    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateTransactionsSequentially() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }

    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transaction);
    }
    verify(seenEntities, times(1)).add(transactions.get(0).id());

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveDuplicateTransactionsConcurrently() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }
    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    /// Create two threads that will process the transactions concurrently.
    int concurrencyDegree = 2;
    AtomicInteger threadErrorCount = new AtomicInteger();
    CountDownLatch done = new CountDownLatch(1);
    Thread[] validationThreads = new Thread[2];
    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      validationThreads[i] = new Thread(() -> {
        try {
          engine.process(transactions.get(finalI));
        } catch (IllegalArgumentException ex) {
          threadErrorCount.incrementAndGet();
        }
        done.countDown();
      });
    }

    ///  Run Threads
    for (Thread t : validationThreads) {
      t.start();
    }

    /// Assert done on time and got no errors
    try {
      boolean doneOnTime = done.await(10, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOnTime);
    } catch (InterruptedException e) {
      Assertions.fail(e);
    }
    Assertions.assertEquals(0, threadErrorCount.get());

    verify(seenEntities, times(1)).add(transactions.get(0).id());
    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testReceiveNoTransactionNorBlockTxConduit() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    // An entity which is neither block nor transaction.
    Entity ent = new EntityFixture();
    try {
      engine.process(ent);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction:" + ent.type());
    }
    Assertions.assertFalse(txConduit.hasSent(ent.id()));
  }

  @Test
  public void testReceiveNoTransactionNorBlockSecondConduit() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    // An entity which is neither block nor transaction.
    Signature signature = SignatureFixture.newSignatureFixture();
    try {
      engine.process(signature);
      Assertions.fail("Should have thrown an exception");
    } catch (IllegalArgumentException ex) {
      Assertions.assertEquals(ex.getMessage(), "entity is neither a block nor a transaction:" + signature.type());
    }
    Assertions.assertFalse(blockConduit.hasSent(signature.id()));
  }


  @Test
  public void testReceiveTransactionsSequentially() throws LightChainDistributedStorageException {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    ArrayList<Transaction> transactions = new ArrayList<>();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);
    snapshot2.all().get(0).setBalance(transaction.getAmount() * 10 + 1);

    for (int i = 0; i < 2; i++) {
      transactions.add(transaction);
    }

    final boolean[] called = {false};
    when(seenEntities.add(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        called[0] = true;
        return called[0];
      }
    });
    when(seenEntities.has(transactions.get(0).id())).thenAnswer(new Answer() {
      public Object answer(InvocationOnMock invocMock) {
        return called[0];
      }
    });

    // Act
    for (int i = 0; i < 2; i++) {
      engine.process(transaction);
    }
    verify(seenEntities, times(1)).add(transactions.get(0).id());

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertTrue(txConduit.hasSent(e.id()));
      }
      Assertions.assertTrue(txConduit.allEntities().size() == 1);
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotCorrect_InvalidSender() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier invalidSender = IdentifierFixture.newIdentifier();
    while (accounts2.contains(invalidSender)) {
      invalidSender = IdentifierFixture.newIdentifier();
    }
    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        invalidSender,
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotCorrect_InvalidReceiver() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    Identifier invalidReceiver = IdentifierFixture.newIdentifier();
    while (accounts2.contains(invalidReceiver)) {
      invalidReceiver = IdentifierFixture.newIdentifier();
    }
    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        invalidReceiver,
        signerId);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testReceiveTransactionNotCorrect_NegativeAmount() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);


    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId,
        -5);

    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotSound_LowerHeight() {
    // Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);


    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() * 5 + 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    when(state.atBlockId(snapshot2.getAccount(transaction.getSender()).getLastBlockId()).getReferenceBlockHeight())
        .thenReturn((long) block2.getHeight() + 5);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotAuthenticated() {
// Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);


    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() * 5 + 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(false);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTransactionNotEnoughBalance() {
// Arrange
    setup();

    // Register the network adapter with the network and create engine.
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedTransactions))).thenReturn(txConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);


    Identifier signerId = snapshot2.all().get(random.nextInt(accounts1.size())).getIdentifier();
    Transaction transaction = TransactionFixture.newTransaction(
        block2.id(),
        snapshot2.all().get(0).getIdentifier(),
        snapshot2.all().get(1).getIdentifier(),
        signerId);

    snapshot2.getAccount(transaction.getSender()).setBalance(transaction.getAmount() - 5);
    when(state.atBlockId(transaction.getReferenceBlockId()).getAccount(transaction.getSender()).getPublicKey()
        .verifySignature(transaction, transaction.getSignature())).thenReturn(true);

    try {
      engine.process(transaction);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      Assertions.fail("Failed because of another reason that this test was not aiming for.");
    }

    try {
      for (Entity e : txConduit.allEntities()) {
        Assertions.assertFalse(txConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testReceiveTwoValidBlocksSequentially() {
    setup();

    Block[] blocks = new Block[2];
    blocks[0] = BlockFixture.newBlock(block2.id(), block2.getHeight() + 1, snapshot2.all());
    blocks[1] = BlockFixture.newBlock(block2.id(), block2.getHeight() + 1, snapshot2.all());

    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);
    for (int i=0; i<2; i++){
      try {
        engine.process(blocks[i]);
      } catch (IllegalArgumentException ex) {
        Assertions.fail(ex);
      }
    }

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }
  @Test
  public void testReceiveOneValidBlock() {
    setup();

    Block block = BlockFixture.newBlock(block2.id(), block2.getHeight() + 1, snapshot2.all());
    when(network.register(any(ValidatorEngine.class), eq(Channels.ProposedBlocks))).thenReturn(blockConduit);
    engine = new ValidatorEngine(network, local, state, seenEntities);

    try {
      engine.process(block);
    } catch (IllegalArgumentException ex) {
      Assertions.fail(ex);
    }

    try {
      for (Entity e : blockConduit.allEntities()) {
        Assertions.assertTrue(blockConduit.hasSent(e.id()));
      }
    } catch (LightChainDistributedStorageException e) {
      e.printStackTrace();
    }
  }
}
