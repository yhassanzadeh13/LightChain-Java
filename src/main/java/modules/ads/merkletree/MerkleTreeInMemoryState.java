package modules.ads.merkletree;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedDataStructure;

/**
 * Implementation of an in-memory Authenticated Skip List
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerkleTreeInMemoryState implements AuthenticatedDataStructure, Serializable, MerkleTree {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final ReentrantReadWriteLock lock;
  private MerkleTreeState state;
  private int size;
  private MerkleNode root;

  /**
   * Default constructor for a Merkle Tree.
   */
  public MerkleTreeInMemoryState() {
    this.size = 0;
    this.root = new MerkleNode();
    this.lock = new ReentrantReadWriteLock();
    this.state = new MerkleTreeState();
  }

  public modules.ads.AuthenticatedEntity put(Entity e) throws IllegalArgumentException {
    try {
      lock.writeLock().lock();
      if (e == null) {
        throw new IllegalArgumentException("entity cannot be null");
      }
      int idx = state.getNodeIndex(new Sha3256Hash(e.id().getBytes()));
      if (idx == -1) {
        this.state = put(e, state, size);
        size++;
        buildMerkleTree();
      }
      MerkleProof proof = getProof(e.id(), state, root);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public modules.ads.AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    MerkleProof proof;
    if (id == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    try {
      lock.readLock().lock();
      proof = getProof(id, state, root);
      Entity e = state.getEntity(id);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.readLock().unlock();
    }
  }

  private void buildMerkleTree() {
    // keeps nodes of the current level of the merkle tree
    // will be updated bottom up
    // initialized with leaves
    ArrayList<MerkleNode> currentLevelNodes = new ArrayList<>(state.getLeafNodes());

    // keeps nodes of the next level of merkle tree
    // used as an intermediary data structure.
    ArrayList<MerkleNode> nextLevelNodes = new ArrayList<>();

    while (currentLevelNodes.size() > 1) { // more than one current node, means we have not yet reached root.
      for (int i = 0; i < currentLevelNodes.size(); i += 2) {
        // pairs up current level nodes as siblings for next level.
        MerkleNode left = currentLevelNodes.get(i);
        left.setLeft(true);

        MerkleNode right;
        if (i + 1 < currentLevelNodes.size()) {
          right = currentLevelNodes.get(i + 1); // we have a right node
        } else {
          // TODO: edge case need to get fixed.
          right = new MerkleNode(left.getHash());
        }
        Sha3256Hash hash = hasher.computeHash(left.getHash().getBytes(), right.getHash().getBytes());
        MerkleNode parent = new MerkleNode(hash, left, right);
        left.setParent(parent);
        right.setParent(parent);
        nextLevelNodes.add(parent);
      }
      currentLevelNodes = nextLevelNodes;
      nextLevelNodes = new ArrayList<>();
    }
    root = currentLevelNodes.get(0);
  }

  public int size() {
    return this.size;
  }

  public MerkleTreeState getState() {
    return state;
  }
}
