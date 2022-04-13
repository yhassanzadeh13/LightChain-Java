package modules.ads;

import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

import java.util.ArrayList;

/**
 * Represents a Merkle Proof of membership against a certain root identifier.
 */
public interface MembershipProof {
  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return hash value of the root node.
   */
  Sha3256Hash getRoot();

  /**
   * Returns the path of the proof of membership.
   *
   * @return path of the proof of membership.
   */
  ArrayList<Sha3256Hash> getPath();
}
