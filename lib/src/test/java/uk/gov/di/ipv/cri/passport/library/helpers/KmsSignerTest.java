package uk.gov.di.ipv.cri.passport.library.helpers;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.SignRequest;
import com.amazonaws.services.kms.model.SignResult;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.util.Base64URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KmsSignerTest {

    public static final String KEY_ID = "test";

    @Mock private AWSKMS kmsClient;
    @Mock private SignResult signResult;

    @Test
    void shouldSignJWSObject() throws JOSEException {
        when(kmsClient.sign(any(SignRequest.class))).thenReturn(signResult);

        byte[] bytes = new byte[10];
        when(signResult.getSignature()).thenReturn(ByteBuffer.wrap(bytes));
        KmsSigner kmsSigner = new KmsSigner(KEY_ID, kmsClient);

        JSONObject jsonPayload = new JSONObject(Map.of("test", "test"));

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(jsonPayload));

        jwsObject.sign(kmsSigner);

        assertEquals(JWSObject.State.SIGNED, jwsObject.getState());
        assertEquals(jwsHeader, jwsObject.getHeader());
        assertEquals(jsonPayload.toJSONString(), jwsObject.getPayload().toString());
    }

    @Test
    void base64UrlSignatureShouldNotIncludePadding() throws Exception {
        KmsSigner kmsSigner = new KmsSigner(KEY_ID, kmsClient);
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();

        byte[] bytesThanWillNormallyHaveB64Padding = new byte[10];

        when(kmsClient.sign(any(SignRequest.class))).thenReturn(signResult);
        when(signResult.getSignature())
                .thenReturn(ByteBuffer.wrap(bytesThanWillNormallyHaveB64Padding));

        Base64URL signature = kmsSigner.sign(jwsHeader, new byte[0]);
        String signatureString = signature.toString();

        assertTrue(
                Base64.getUrlEncoder()
                        .encodeToString(bytesThanWillNormallyHaveB64Padding)
                        .endsWith("=="));
        assertFalse(signatureString.endsWith("="));
    }
}
