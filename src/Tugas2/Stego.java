package Tugas2;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

class Stego
{
  public boolean feasible = true;
  private AudioInputStream audioInputStream;
  private AudioFormat sndFormat;
  private byte[] audioBytes;
  private byte[] buff;
  private byte[] cipherbuff;
  private byte[] clearbuff;
  private String outFile;
  char[] password;
  PBEKeySpec pbeKeySpec;
  public long soundFileSize;

  public Stego(String sndFile, String ptFile, String oFile, char[] pwd)
  {
    this.password = pwd;
    this.outFile = oFile;
    readSND(sndFile);
    this.feasible = possible(ptFile);
  }

  public Stego(String sndFile, String ptFile, char[] pwd)
  {
    this.password = pwd;
    this.outFile = ptFile;
    readSND(sndFile);

    System.out.println("Pwd " + pwd);
  }

  public void encode()
  {
    int k = 0;
    int i = 1;

    System.out.println("Hiding the ciphertext in wav file ...");

    int pt = this.cipherbuff.length;
    for (int j = 1; j <= 32; j++)
    {
      if ((pt & 0x80000000) != 0)
      {
        this.audioBytes[i] = ((byte)(this.audioBytes[i] | 0x1));
      }
      else if ((this.audioBytes[i] & 0x1) != 0)
      {
        this.audioBytes[i] = ((byte)(this.audioBytes[i] >>> 1));
        this.audioBytes[i] = ((byte)(this.audioBytes[i] << 1));
      }
      pt <<= 1;
      i++;
    }
    while (k < this.cipherbuff.length)
    {
      byte pb = this.cipherbuff[k];
      for (int j = 1; j <= 8; j++)
      {
        if ((pb & 0x80) != 0)
        {
          this.audioBytes[i] = ((byte)(this.audioBytes[i] | 0x1));
        }
        else if ((this.audioBytes[i] & 0x1) != 0)
        {
          this.audioBytes[i] = ((byte)(this.audioBytes[i] >>> 1));
          this.audioBytes[i] = ((byte)(this.audioBytes[i] << 1));
        }
        pb = (byte)(pb << 1);
        i++;
      }
      k++;
    }
    System.out.println();

    File fileOut = new File(this.outFile);
    ByteArrayInputStream byteIS = new ByteArrayInputStream(this.audioBytes);
    AudioInputStream audioIS = new AudioInputStream(byteIS, this.audioInputStream.getFormat(), this.audioInputStream.getFrameLength());
    if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.AU, audioIS)) {
      try
      {
        AudioSystem.write(audioIS, AudioFileFormat.Type.AU, fileOut);
        System.out.println("Steganographed WAV file is written as " + this.outFile + "...");
      }
      catch (Exception e)
      {
        System.err.println("Sound File write error");
      }
    }
  }

  public boolean decode()
  {
    boolean success = true;
    byte out = 0;
    int length = 0;
    int k = 1;
    System.out.println("Retrieving the ciphertext from wav file ....");

    length &= 0x0;
    for (int j = 1; j <= 32; j++)
    {
      length <<= 1;
      if ((this.audioBytes[k] & 0x1) != 0) {
        length |= 0x1;
      }
      k++;
    }
    this.buff = new byte[length];

    out = (byte)(out & 0x0);
    for (int i = 0; i < length; i++)
    {
      for (int j = 1; j <= 8; j++)
      {
        out = (byte)(out << 1);
        if ((this.audioBytes[k] & 0x1) != 0) {
          out = (byte)(out | 0x1);
        }
        k++;
      }
      this.buff[i] = out;

      out = (byte)(out & 0x0);
    }
    decrypt();
    try
    {
      System.out.println("Writing the decrypted hidden message to" + this.outFile + " ...");
      FileOutputStream outfile = new FileOutputStream(this.outFile);
      outfile.write(this.clearbuff);
      outfile.close();
    }
    catch (Exception e)
    {
      System.out.println("Caught Exception: " + e);
    }
    return success;
  }

  private void readSND(String sndf)
  {
    File sndfile = new File(sndf);
    this.soundFileSize = sndfile.length();

    int totalFramesRead = 0;
    System.out.println("Reading (wav) sound file ...");
    try
    {
      this.audioInputStream = AudioSystem.getAudioInputStream(sndfile);

      int bytesPerFrame = this.audioInputStream.getFormat().getFrameSize();

      this.audioBytes = new byte[(int)this.soundFileSize];
      try
      {
        int numBytesRead = 0;
        int numFramesRead = 0;
        while ((numBytesRead = this.audioInputStream.read(this.audioBytes)) != -1)
        {
          numFramesRead = numBytesRead / bytesPerFrame;
          totalFramesRead += numFramesRead;
        }
      }
      catch (Exception ex)
      {
        System.out.println("Audio file error:" + ex);
      }
    }
    catch (Exception e)
    {
      System.out.println("Audio file error:" + e);
    }
  }

  private boolean possible(String pt)
  {
    try
    {
      System.out.println("Reading the plaintext file ..." + pt);
      FileInputStream fis = new FileInputStream(pt);
      BufferedInputStream bis = new BufferedInputStream(fis);
      int len = bis.available();
      this.buff = new byte[len];
      while (bis.available() != 0) {
        len = bis.read(this.buff);
      }
      bis.close();
      fis.close();
    }
    catch (Exception e)
    {
      System.out.println("Could Not Read Plain Text Caught Exception: " + e);
    }
    try
    {
      encrypt();
      if (this.cipherbuff.length * 8 > this.audioBytes.length) {
        return false;
      }
      return true;
    }
    catch (Exception e)
    {
      System.out.println("******Exception: " + e);
    }
    return true;
  }

  private char[] generatePasswd(char[] inputval)
    throws IOException
  {
    char[] lineBuffer;
    char[] buf = lineBuffer = new char['?'];
    int room = buf.length;
    int offset = 0;

    int index = 0;
    int lenofinputval = inputval.length;
    System.out.println("Debug:inputval: " + inputval);
    System.out.println("Debug:lenofinputval: " + lenofinputval);
    while (index < lenofinputval)
    {
      int c;
      switch (c = inputval[index])
      {
      case '?':
      case '\n':
        System.out.println("Debug::Iam in NewLine or -1");
        break;
      case '\r':
        System.out.println("Debug::Iam in Carriage Return");
        index++;
        int c2 = inputval[index];
        if ((c2 == 10) || (c2 == -1)) {
          break;
        }
        index--;
        System.out.println("Debug::Iam in Carriage Return IF Block");
      default:
        room--;
        if (room < 0)
        {
          buf = new char[offset + 128];
          room = buf.length - offset - 1;
          System.arraycopy(lineBuffer, 0, buf, 0, offset);
          Arrays.fill(lineBuffer, ' ');
          lineBuffer = buf;
        }
        buf[(offset++)] = ((char)c);

        index++;
      }
    }
    if (offset == 0) {
      return null;
    }
    char[] ret = new char[offset];
    System.arraycopy(buf, 0, ret, 0, offset);
    Arrays.fill(buf, ' ');
    System.out.println("$$$$$$$$$$::::...... Password Generated: " + ret);
    return ret;
  }

  private char[] readPasswd(InputStream in)
    throws IOException
  {
    char[] lineBuffer;
    char[] buf = lineBuffer = new char['?'];

    int room = buf.length;
    int offset = 0;
    for (;;)
    {
      int c;
      switch (c = in.read())
      {
      case -1:
      case 10:
        break;
      case 13:
        int c2 = in.read();
        if ((c2 == 10) || (c2 == -1)) {
          break;
        }
        if (!(in instanceof PushbackInputStream)) {
          in = new PushbackInputStream(in);
        }
        ((PushbackInputStream)in).unread(c2);
      default:
        room--;
        if (room < 0)
        {
          buf = new char[offset + 128];
          room = buf.length - offset - 1;
          System.arraycopy(lineBuffer, 0, buf, 0, offset);
          Arrays.fill(lineBuffer, ' ');
          lineBuffer = buf;
        }
        buf[(offset++)] = ((char)c);
      }
   // }
    
    if(offset == 0) {
     return null;
    }
    char[] ret = new char[offset];
    System.arraycopy(buf, 0, ret, 0, offset);
    Arrays.fill(buf, ' ');
    System.out.println("Password Captured: " + ret);
    return ret;

  }}

  private void encrypt()
  {
    byte[] salt = { -57, 115, 33, -116, 126, -56, -18, -103 };

    int count = 20;

    PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
    try
    {
      System.out.println("Password Verification: " + this.password);

      this.pbeKeySpec = new PBEKeySpec(this.password);
      System.out.println("Encrypting the plaintext message ...");

      SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey pbeKey = keyFac.generateSecret(this.pbeKeySpec);

      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");

      pbeCipher.init(1, pbeKey, pbeParamSpec);

      this.cipherbuff = pbeCipher.doFinal(this.buff);
    }
    catch (Exception ex)
    {
      System.out.println("Caught Exception: " + ex);
      ex.printStackTrace();
    }
  }

  private void decrypt()
  {
    byte[] salt = { -57, 115, 33, -116, 126, -56, -18, -103 };

    int count = 20;

    PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
    try
    {
      System.out.println();
      System.out.print("Enter encryption password:  ");

      PBEKeySpec pbeKeySpec = new PBEKeySpec(this.password);

      System.out.println("Decrypting the ciphertext ...");

      SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
      SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

      Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");

      pbeCipher.init(2, pbeKey, pbeParamSpec);
      try
      {
        this.clearbuff = pbeCipher.doFinal(this.buff);
      }
      catch (Exception ex)
      {
        System.out.println("Possible password missmatch!!\n");
        System.out.println("Caught Exception1: " + ex);
      }
    }
    catch (Exception ex)
    {
      System.out.println("Caught Exception2: " + ex);
    }
  }
}
