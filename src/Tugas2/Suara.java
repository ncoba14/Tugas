package Tugas2;
import java.io.File;
import javax.swing.filechooser.FileFilter;

class Suara
  extends FileFilter
{
  public boolean accept(File file)
  {

    return (file.isDirectory()) || (file.getAbsolutePath().endsWith(".wav"));
  }

  public String getDescription()
  {

    return ".wav File (*.wav)";
  }
}
