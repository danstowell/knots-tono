/*
This file is part of TONO.

    TONO is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    TONO is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with TONO.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;

public class KnotApp extends JFrame implements Runnable
{
  private KnotData       kd, initialkd;
  private KnotDataViewer knotViewer;
  private int            knotViewerHeight, knotViewerWidth;
  private static KnotApp theApp;
  private boolean        running;
  private File           defaultLoadDirectory = new File("~/javastuff/uk/co/mcld/");
  private File           defaultSaveDirectory = new File("~/javastuff/uk/co/mcld/");
  private Thread         calcThread;

    // UI objects
    private JButton interpolateButton, rethreadButton, loadKnotButton, saveKnotButton, goButton, goOnceButton;
    private JScrollPane algoProgScrollPane;
    private JTextPane   algoProgTextPane;
    // End of UI objects

  public static void main(String[] args)
  {
    theApp = new KnotApp();
  }

  public KnotApp()
  {
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setTitle("Knot machine");
    running = false;
    knotViewerHeight = 500;
    knotViewerWidth = 500;
    
    kd = initialkd = new KnotData();
    
    // Set up UI objects
    knotViewer = new KnotDataViewer(knotViewerWidth, knotViewerHeight, kd);
    algoProgTextPane = new JTextPane();
    algoProgTextPane.setEditable(false);
    algoProgScrollPane = new JScrollPane(algoProgTextPane);
    interpolateButton = new JButton("Interpolate");
    interpolateButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
                                    kd.interpolate();
                                    knotViewer.update(kd);
                                  }});
    rethreadButton = new JButton("Rethread");
    rethreadButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
                                    kd.rethread();
                                    knotViewer.update(kd);
                                  }});
    loadKnotButton = new JButton("Load knot");
    loadKnotButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
					  // Show file chooser
					  JFileChooser fileChooser = new JFileChooser();
					  fileChooser.setCurrentDirectory(defaultLoadDirectory);
					  fileChooser.setSelectedFile(new File(defaultLoadDirectory, 
											"knotdata"));
					  int result = fileChooser.showOpenDialog(theApp);
					  if(result==fileChooser.APPROVE_OPTION)
					  {
					    loadKnotData(fileChooser.getSelectedFile());
					  }
                                  }});
    saveKnotButton = new JButton("Save knot");
    saveKnotButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
					  // Show file chooser
					  JFileChooser fileChooser = new JFileChooser();
					  fileChooser.setCurrentDirectory(defaultSaveDirectory);
					  fileChooser.setSelectedFile(new File(defaultSaveDirectory, 
											"knotdata"));
					  int result = fileChooser.showSaveDialog(theApp);
					  if(result==fileChooser.APPROVE_OPTION)
					  {
					    saveKnotData(fileChooser.getSelectedFile());
					  }

                                  }});
    goButton = new JButton("GO!");
    goButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
                                    if(goButton.getText()=="GO!")
                                    {
                                      running = true;
                                      goButton.setText("Stop");
                                      calcThread = new Thread(theApp);
                                      calcThread.start();
                                    }
                                    else
                                    {
                                      goButton.setText("Algorithm stopping...");
                                      running = false;
                                    }
                                  }});
    goOnceButton = new JButton("Iterate once");
    goOnceButton.addActionListener(new ActionListener(){
                                  public void actionPerformed(ActionEvent e)
                                  {
                                      running = false;
                                      calcThread = new Thread(theApp);
                                      calcThread.start();
                                  }});
    Box viewsBox = Box.createHorizontalBox();
    Box buttonsBox = Box.createHorizontalBox();
    Box vertBox = Box.createVerticalBox();
    viewsBox.add(knotViewer);
    viewsBox.add(algoProgScrollPane);
    buttonsBox.add(interpolateButton);
    buttonsBox.add(rethreadButton);
    buttonsBox.add(loadKnotButton);
    buttonsBox.add(saveKnotButton);
    buttonsBox.add(goButton);
    buttonsBox.add(goOnceButton);
    vertBox.add(viewsBox);
    vertBox.add(buttonsBox);
    getContentPane().add(vertBox);
    // End of: Set up UI objects

    Toolkit theToolkit = getToolkit();
    Dimension wndSize = theToolkit.getScreenSize();
    setBounds(0,0,(int)(wndSize.width*0.9f),(int)(wndSize.height*0.9f));
    setVisible(true);
  }
  
  public void loadKnotData(File inFile)
  {
    // Get the file as an input stream
    try
    {
	  defaultLoadDirectory = inFile.getParentFile();
      FileInputStream inStream = new FileInputStream(inFile);
      kd = new KnotData(inStream);
      inStream.close();
      initialkd = kd.getCopy();
      // knotViewer = new KnotDataViewer(knotViewerWidth, knotViewerHeight, kd);
      knotViewer.update(kd);
    }
    catch(IOException e)
    {
      JOptionPane.showMessageDialog(theApp, "Failed to load knot!: "+e, 
                                    "File error", JOptionPane.ERROR_MESSAGE);
    }
  } // End of: loadKnotData(File inFile)
  
  public void saveKnotData(File saveFile)
  {
    String outString = kd.toString();
	// Open a BufferedWriter and save the file
	try
	{
	  defaultSaveDirectory = saveFile.getParentFile();
	  saveFile.createNewFile();
	  BufferedWriter out = new BufferedWriter(
	   		new FileWriter(saveFile));
	  out.write(outString);
	  out.close();
	}
	catch(IOException ee)
	{
       JOptionPane.showMessageDialog(theApp, 
                                    "Failed to save knot! Sorry! : "+ee, 
                                    "File error", JOptionPane.ERROR_MESSAGE);
	}
  }
  
  public void run()
  {
    if(kd==null) return; // If there's no knot data then we do nothing

    do
    {
      // Carry out an iteration
      kd.iterate();
      
      // Update the viewer
      if(knotViewer!=null)
        knotViewer.update(kd);
      
      try
      {
        Thread.sleep(10);
      }catch(InterruptedException e){}
    } // End of thread loop
    while(running);

    // Update the algoProgress text
    algoProgTextPane.setText(kd.getAlgoProgress());

    goButton.setText("GO!");
  } // End of: run()

}
