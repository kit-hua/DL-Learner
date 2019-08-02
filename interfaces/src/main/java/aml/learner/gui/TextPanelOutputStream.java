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
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;


public class TextPanelOutputStream extends OutputStream {
    private static final TextPanelOutputStream INSTANCE = new TextPanelOutputStream();
    private static PrintStream OUT;
    private static JTextPane outWriter;
    private static boolean TAINTED = false;
    private static boolean ACTIVE = false;
    static {
//        OUT = System.out;
    		try {
				OUT = new PrintStream(System.out, true, "UTF-16");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        System.setOut(new PrintStream(new TextPanelOutputStream()));
    }
  
    /** Creates a new instance of TextAreaOutputStream. */
    private TextPanelOutputStream() {}
  
    /** Gets the output stream. */
    public static TextPanelOutputStream getInstance(JTextPane textPane) {
        outWriter = textPane;
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
    		if(ACTIVE)
    			outWriter.setText(outWriter.getText() + (char)param);
    		
        TAINTED = true;
    }
    
    public void setActive (boolean active) {
    		ACTIVE = active; 
    }
    
//    public static void main(String args[]){
//        JFrame aFrame = new JFrame();       
//         
//        JTextPane jta = new JTextPane(50,100);
//        TextPanelOutputStream taos = TextPanelOutputStream.getInstance(jta);
//        aFrame.getContentPane().add(jta);
//        aFrame.pack();
//        aFrame.show();
//         
//        
//        File file = new File("/Users/aris/Documents/repositories/ipr/aml/aml_framework/src/main/resources/test/amlACM.json");
//        BufferedReader br;
//		try {
//			br = new BufferedReader(new FileReader(file));
//			String st;
//			while ((st = br.readLine()) != null) 
//				  System.out.println(st);
//		} catch (FileNotFoundException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}             
//        
//    }
}