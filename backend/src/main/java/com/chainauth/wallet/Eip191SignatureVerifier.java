package com.chainauth.wallet;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
@Component
public class Eip191SignatureVerifier {
 public boolean verify(String message,String signature,String expectedAddress){
  try{
   byte[] sig=Numeric.hexStringToByteArray(signature);if(sig.length!=65)return false;
   byte v=sig[64];if(v<27)sig[64]=(byte)(v+27);
   Sign.SignatureData data=new Sign.SignatureData(sig[64],Arrays.copyOfRange(sig,0,32),Arrays.copyOfRange(sig,32,64));
   var key=Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8),data);
   return ("0x"+Keys.getAddress(key)).equalsIgnoreCase(expectedAddress);
  }catch(Exception ignored){return false;}
 }
}
