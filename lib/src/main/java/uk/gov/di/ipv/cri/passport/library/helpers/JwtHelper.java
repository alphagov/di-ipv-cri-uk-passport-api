package uk.gov.di.ipv.cri.passport.library.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Map;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class JwtHelper {

    private static final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private JwtHelper() {}

    public static <T> SignedJWT createSignedJwtFromClaimSet(
            JWTClaimsSet claimsSet, JWSSigner signer) throws JOSEException {
        JWSHeader jwsHeader = generateHeader();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(signer);
        return signedJWT;
    }

    public static SignedJWT transcodeSignature(SignedJWT signedJWT)
            throws JOSEException, java.text.ParseException {
        Base64URL transcodedSignatureBase64 =
                Base64URL.encode(
                        ECDSA.transcodeSignatureToConcat(
                                signedJWT.getSignature().decode(),
                                ECDSA.getSignatureByteArrayLength(ES256)));
        String[] jwtParts = signedJWT.serialize().split("\\.");
        return SignedJWT.parse(
                String.format("%s.%s.%s", jwtParts[0], jwtParts[1], transcodedSignatureBase64));
    }

    public static boolean signatureIsDerFormat(SignedJWT signedJWT) throws JOSEException {
        return signedJWT.getSignature().decode().length != ECDSA.getSignatureByteArrayLength(ES256);
    }

    private static JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }

    private static <T> JWTClaimsSet generateClaims(T claimInput) {
        var claimsBuilder = new JWTClaimsSet.Builder();

        mapper.convertValue(claimInput, Map.class)
                .forEach((key, value) -> claimsBuilder.claim((String) key, value));

        return claimsBuilder.build();
    }
}
