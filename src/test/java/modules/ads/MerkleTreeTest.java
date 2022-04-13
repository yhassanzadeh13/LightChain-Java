package modules.ads;

import model.crypto.Sha3256Hash;
import modules.ads.merkletree.MerkleTree;
import modules.ads.merkletree.Verifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerkleTreeFixture;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of SkipList ADS.
 */
public class MerkleTreeTest {
  // TODO: writing tests to cover
  // 1. When putting a unique entity into merkle tree, we can recover it.
  // 2. Proof of membership for putting and getting an entity is the same.
  // 3. Putting an already existing entity does not change its membership proof.
  // 4. Putting 100 distinct entities concurrently inserts all of them into skip list with correct membership proofs,
  //  and also, makes them all retrievable with correct membership proofs.
  // 5. Getting non-existing identifiers returns null.
  // 7. Putting null returns null.
  // 8. Tampering with root identifier of an authenticated entity fails its verification.
  // 9. Tampering with entity of an authenticated entity fails its verification.
  // 10. Tampering with proof of an authenticated entity fails its verification.

  @Test
  public void TestVerification() {
    MerkleTree merkleTree = MerkleTreeFixture.createSkipList(5);
    EntityFixture entityFixture = new EntityFixture();
    merkleTree.put(entityFixture);
    AuthenticatedEntity authenticatedEntity = merkleTree.get(entityFixture);
    Verifier verifier = new Verifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }
}
