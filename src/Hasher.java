import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Hasher {

    private String encodedHash;  //The hash in base 64
    private byte[] hash;        //The raw hash
    private final String algorithm = "MD5";

    public Hasher(String message){
        hashMessage(message);
    }

    public Hasher(File file){
        hashMessage(getFileContents(file));
    }

    private void hashMessage(String message){
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
            hash = md.digest(message.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        Base64.Encoder encoder = Base64.getEncoder();
        encodedHash = encoder.encodeToString(getHash());
    }

    private String getFileContents(File file){
        StringBuilder stringBuilder = new StringBuilder();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null){
                stringBuilder.append(line + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileContents = stringBuilder.toString();
        System.out.println(fileContents);
        return fileContents;
    }

    public String getEncodedHash() {
        return encodedHash;
    }

    public byte[] getHash() {
        return hash;
    }

    public static void main(String[] args){
        String filepath = "C://Users/Marco Morelos/Desktop/Brownie AF.txt";
        File file = new File(filepath);
        Hasher hasher = new Hasher(file);
        System.out.println("Hash: " + hasher.getEncodedHash());
    }
}
