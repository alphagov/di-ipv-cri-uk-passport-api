package uk.gov.di.ipv.cri.passport.library.helpers;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.MessageType;
import com.amazonaws.services.kms.model.SignRequest;
import com.amazonaws.services.kms.model.SignResult;
import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import uk.gov.di.ipv.cri.passport.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class KmsSigner implements JWSSigner {

    private final AWSKMS kmsClient;

    private final JCAContext jcaContext = new JCAContext();
    private final String keyId;

    @ExcludeFromGeneratedCoverageReport
    public KmsSigner(String keyId) {
        this.keyId = keyId;
        this.kmsClient = AWSKMSClientBuilder.defaultClient();
    }

    public KmsSigner(String keyId, AWSKMS kmsClient) {
        this.keyId = keyId;
        this.kmsClient = kmsClient;
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        byte[] signingInputHash;

        try {
            signingInputHash = MessageDigest.getInstance("SHA-256").digest(signingInput);
        } catch (NoSuchAlgorithmException e) {
            throw new JOSEException(e.getMessage());
        }

        SignRequest signRequest =
                new SignRequest()
                        .withSigningAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256.toString())
                        .withKeyId(keyId)
                        .withMessage(ByteBuffer.wrap(signingInputHash))
                        .withMessageType(MessageType.DIGEST);

        SignResult signResult = kmsClient.sign(signRequest);

        return Base64URL.encode(signResult.getSignature().array());
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return Set.of(ES256);
    }

    @Override
    public JCAContext getJCAContext() {
        return jcaContext;
    }
}
