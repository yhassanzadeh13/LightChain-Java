package protocol.engines;

import java.util.concurrent.locks.ReentrantLock;

import model.Entity;
import model.codec.EntityType;
import model.crypto.Signature;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Assignment;
import model.lightchain.Block;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import model.local.Local;
import network.Channels;
import network.Conduit;
import network.Network;
import protocol.Engine;
import protocol.Parameters;
import protocol.assigner.LightChainValidatorAssigner;
import protocol.block.BlockValidator;
import protocol.transaction.TransactionValidator;
import state.State;
import storage.Identifiers;

/**
 * ValidatorEngine is a standalone engine of LightChain that runs transaction and block validation.
 */
public class ValidatorEngine implements Engine {
  public final Identifiers seenEntities;
  private final Local local;
  private final Conduit blockCon;
  private final Conduit transCon;
  private final State state;
  private final ReentrantLock lock;

  /**
   * Constructor for ValidatorEngine.
   *
   * @param net          the network
   * @param local        the local
   * @param state        the state
   * @param seenEntities the seen entities
   */
  public ValidatorEngine(Network net, Local local, State state, Identifiers seenEntities) {
    this.local = local;
    this.blockCon = net.register(this, Channels.ProposedBlocks);
    this.transCon = net.register(this, Channels.ProposedTransactions);
    this.state = state;
    this.seenEntities = seenEntities;
    this.lock = new ReentrantLock();
  }


  /**
   * Received entity to this engine can be either a block or a transaction, anything else should throw an exception.
   * Upon receiving a block or transaction, the engine runs the assignment and checks whether the current node
   * is an assigned validator for that transaction or block. If the current node is not assigned, the engine
   * discards the entity.
   * If received entity is a block, the engine runs block validation on it, and if it passes the validation,
   * the engine signs the identifier of that block and sends the signature to the proposer of the block.
   * If received entity is a transaction, it runs the transaction validation on it.
   * If the transaction passes validation,
   * the engine signs the identifier of that transaction and sends the signature to the sender of that transaction.
   *
   * @param e the arrived Entity from the network, it should be either a transaction or a block.
   * @throws IllegalArgumentException when the arrived entity is neither a transaction nor a block.
   */
  @Override
  public void process(Entity e) throws IllegalArgumentException {
    if (!e.type().equals(EntityType.TYPE_BLOCK) && !e.type().equals(EntityType.TYPE_TRANSACTION)) {
      throw new IllegalArgumentException("entity is neither a block nor a transaction:" + e.type());
    }

    if (seenEntities.has(e.id())) {
      return; // entity already processed.
    }

    try {
      lock.lock();
      LightChainValidatorAssigner assigner = new LightChainValidatorAssigner();
      Identifier currentNode = this.local.myId();

      if (e.type().equals(EntityType.TYPE_BLOCK)) {
        Block block = ((Block) e);
        Assignment assignment;
        try {
          assignment = assigner.assign(block.id(),
                  state.atBlockId((block).getPreviousBlockId()),
                  Parameters.VALIDATOR_THRESHOLD);
        } catch (IllegalArgumentException ex) {
          return;
        }

        if (!assignment.has(currentNode)) {
          return; // current node is not an assigned validator.
        }

        if (isBlockValidated(block)) {
          Signature certificate = this.local.signEntity(block);
          try {
            this.blockCon.unicast(certificate, (block.getProposer()));
            this.seenEntities.add(block.id());
          } catch (LightChainNetworkingException ex) {
            System.err.println("could not unicast the block certificate");
            System.exit(1);
          }
        }

      } else if (e.type().equals(EntityType.TYPE_TRANSACTION)) {

        Transaction tx = ((Transaction) e);
        Assignment assignment;
        try {
          assignment = assigner.assign(
                  tx.id(),
                  state.atBlockId(tx.getReferenceBlockId()),
                  Parameters.VALIDATOR_THRESHOLD);
        } catch (IllegalArgumentException ex) {
          return;
        }

        if (!assignment.has(currentNode)) {
          return; // current node is not an assigned validator.
        }

        if (isTransactionValidated((Transaction) e)) {
          Signature certificate = this.local.signEntity(tx);
          try {
            this.transCon.unicast(certificate, (tx.getSender()));
            this.seenEntities.add(tx.id());
          } catch (LightChainNetworkingException ex) {
            System.err.println("could not unicast the transaction certificate");
            System.exit(1);
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private boolean isBlockValidated(Block b) {
    BlockValidator verifier = new BlockValidator(state);
    try {
      verifier.allTransactionsSound(b);
      verifier.allTransactionsValidated(b);
      verifier.isAuthenticated(b);
      verifier.isConsistent(b);
      verifier.isCorrect(b);
      verifier.noDuplicateSender(b);
      verifier.proposerHasEnoughStake(b);
    } catch (Exception ex) {
      return false;
    }
    return verifier.allTransactionsSound(b)
            && verifier.allTransactionsValidated(b)
            && verifier.isAuthenticated(b)
            && verifier.isConsistent(b)
            && verifier.isCorrect(b)
            && verifier.noDuplicateSender(b)
            && verifier.proposerHasEnoughStake(b);
  }

  private boolean isTransactionValidated(Transaction t) {
    TransactionValidator verifier = new TransactionValidator(state);
    try {
      verifier.isCorrect(t);
      verifier.isSound(t);
      verifier.isAuthenticated(t);
      verifier.senderHasEnoughBalance(t);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return verifier.isCorrect(t)
            && verifier.isSound(t)
            && verifier.isAuthenticated(t)
            && verifier.senderHasEnoughBalance(t);
  }
}
