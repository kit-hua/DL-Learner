package aml.learner.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JTextArea;


public class TextAreaOutputStream extends OutputStream {
    private static final TextAreaOutputStream INSTANCE = new TextAreaOutputStream();
    private static PrintStream OUT;
    private static JTextArea outWriter;
    private static boolean TAINTED = false;
    static {
//        OUT = System.out;
    		try {
				OUT = new PrintStream(System.out, true, "UTF-16");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        System.setOut(new PrintStream(new TextAreaOutputStream()));
    }
  
    /** Creates a new instance of TextAreaOutputStream. */
    private TextAreaOutputStream() {}
  
    /** Gets the output stream. */
    public static TextAreaOutputStream getInstance(JTextArea textArea) {
        outWriter = textArea;
        return INSTANCE;
    }
  
    /** Gets the functioning console output.
      * @see java.lang.System.out
      */
    public static PrintStream getOldSystemOut() {
        return OUT;
    }
  
    /** Determines if output has occured. */
    public static boolean isTainted() {
        return TAINTED;
    }
  
    /** Write output to the Text Area. */
    public void write(int param) {
        outWriter.setText(outWriter.getText() + (char)param);
        TAINTED = true;
    }
    
    public static void main(String args[]){
        JFrame aFrame = new JFrame();       
         
        JTextArea jta = new JTextArea(50,100);
        TextAreaOutputStream taos = TextAreaOutputStream.getInstance(jta);
        aFrame.getContentPane().add(jta);
        aFrame.pack();
        aFrame.show();
         
        
        File file = new File("/Users/aris/Documents/repositories/ipr/aml/aml_framework/src/main/resources/test/amlACM.json");
        BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String st;
			while ((st = br.readLine()) != null) 
				  System.out.println(st);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}             
        
    }
}