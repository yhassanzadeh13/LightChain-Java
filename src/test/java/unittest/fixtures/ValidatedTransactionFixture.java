package unittest.fixtures;

import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import protocol.Parameters;

/**
 * Encapsulates creating validated transactions with random content for fixture.
 */
public class ValidatedTransactionFixture extends ValidatedTransaction {
  /**
   * Random object to create random integers.
   */
  private static final Random random = new Random();

  /**
   * Constructor of the transaction.
   *
   * @param referenceBlockId identifier of a finalized block that this transaction refers to its snapshot.
   * @param sender           identifier of the sender of this transaction.
   * @param receiver         identifier of the receiver of this transaction.
   * @param amount           amount of LightChain tokens that this transaction transfers from sender to receiver.
   * @param certificates     signature of assigned validators to this transaction.
   */
  public ValidatedTransactionFixture(Identifier referenceBlockId,
                                     Identifier sender,
                                     Identifier receiver,
                                     double amount,
                                     Signature[] certificates) {
    super(referenceBlockId, sender, receiver, amount, certificates);
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters.
   *
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction() {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD + 2;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = new SignatureFixture(Bytes.byteArrayFixture(32), null);
    }
    return new ValidatedTransactionFixture(referenceBlockId, sender, receiver, amount, certificates);
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender identifier.
   *
   * @param sender identifier of the sender of this transaction.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(Identifier sender) {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    int certificatesSize = 2 + Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = new SignatureFixture(Bytes.byteArrayFixture(32), null);
    }
    return new ValidatedTransactionFixture(referenceBlockId, sender, receiver, amount, certificates);
  }

  /**
   * Constructor of the validated transactions with randomly generated parameters and given sender identifier.
   *
   * @param certificatesSize size of the certificates array.
   * @return random ValidatedTransaction object.
   */
  public static ValidatedTransaction newValidatedTransaction(int certificatesSize) {
    Identifier referenceBlockId = IdentifierFixture.newIdentifier();
    Identifier sender = IdentifierFixture.newIdentifier();
    Identifier receiver = IdentifierFixture.newIdentifier();
    double amount = 100;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = new SignatureFixture(Bytes.byteArrayFixture(32), null);
    }
    return new ValidatedTransactionFixture(referenceBlockId, sender, receiver, amount, certificates);
  }
}
