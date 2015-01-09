package beatbox;

import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.SwingWorker;
import java.util.*;
import java.io.*;
import java.net.Socket;

/** Music creation creation application. Uses a Swing
  * GUI instance to collect the state of 16 rows of 16
  * JCheckBox objects, each representing an instrument to be
  * played on a specific beat through the use of sequenced
  * MIDI events. JCheckBox state is kept in an array, which 
  * is processed and turned into MidiEvent objects upon
  * pressing 'Start', according to the instrument selected.
  * The MidiEvents are thus added to a track which is then
  * added to a sequence, ultimately played by a sequencer.
  */

public class CollaborativeBeatBox {
  
  JPanel mainPanel;
  ArrayList<JCheckBox> checkboxList;
  Sequencer sequencer;
  Sequence sequence;
  Track track;
  JFrame theFrame;
  
  BufferedReader in;
  PrintWriter out;
  
  JTextField textField = new JTextField(40);
  JTextArea messageField = new JTextArea(8, 40);
  
  /** 
   * Array of instrument row labels
   * */
  
  String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
    "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
    "Hand Clap", "High Tom", "Hi Bongo", "Maracas", 
    "Whistle", "Low Conga", "Cowbell", "Vibraslap",
    "Low-mid Tom", "High Agogo", "Open Hi Conga"};
  
  /**
   * Array of instrument keys
   *  */
  
  int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60,
    70, 72, 64, 56, 58, 47, 67, 63};
  
    /**
     * Creates the Beat Box.
     */
       
  public static void main (String[] args) {
    new CollaborativeBeatBox().buildGUI();
  }
  
     /**
      * Swing construction code
      */
        
  public void buildGUI() {
    theFrame = new JFrame("Collaborative Beatbox");
    theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    BorderLayout layout = new BorderLayout();
    JPanel background = new JPanel(layout);
    background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    
    checkboxList = new ArrayList<JCheckBox>();
    Box buttonBox = new Box(BoxLayout.Y_AXIS);
    
    JButton start = new JButton("Start");
    start.addActionListener(new myStartListener());
    buttonBox.add(start);
    
    JButton stop = new JButton("Stop");
    stop.addActionListener(new myStopListener());
    buttonBox.add(stop);
    
    JButton clear = new JButton("Clear");
    clear.addActionListener(new myClearListener());
    buttonBox.add(clear);
    
    JButton upTempo = new JButton("Tempo Up");
    upTempo.addActionListener(new myUpTempoListener());
    buttonBox.add(upTempo);
    
    JButton downTempo = new JButton("Tempo Down");
    downTempo.addActionListener(new myDownTempoListener());
    buttonBox.add(downTempo);
    
    JButton saveBeat = new JButton("Save");
    saveBeat.addActionListener(new mySendListener());
    buttonBox.add(saveBeat);
    
    JButton loadBeat = new JButton("Load");
    loadBeat.addActionListener(new myReadInListener());
    buttonBox.add(loadBeat);
    
    JButton initialize = new JButton("Connect to Chat Server");
    initialize.addActionListener(new myConnectListener());
    buttonBox.add(initialize);
    
    buttonBox.add(textField, "North");
    
    textField.addActionListener(new myTextListener());
    textField.setEditable(false);
    
    buttonBox.add(new JScrollPane(messageField), "Center");
    messageField.setEditable(false);
        
    Box nameBox = new Box(BoxLayout.Y_AXIS);
    for (int i = 0; i < 16; i++) {
      nameBox.add(new Label(instrumentNames[i]));
    }
    
    background.add(BorderLayout.EAST, buttonBox);
    background.add(BorderLayout.WEST, nameBox);
    
    theFrame.getContentPane().add(background);
    
    GridLayout grid = new GridLayout(16,16);
    grid.setVgap(1);
    grid.setHgap(2);
    
    mainPanel = new JPanel(grid);
    background.add(BorderLayout.CENTER, mainPanel);
    
    // Creates 16 x 16 square of checkboxes.
    for (int i = 0; i < 256; i++) {
      JCheckBox c = new JCheckBox();
      c.setSelected(false);
      checkboxList.add(c);
      mainPanel.add(c);
    }
    
    //
    setUpMidi();
    
    theFrame.setBounds(50,50,300,300);
    theFrame.pack();
    theFrame.setVisible(true);
  }
  
  public void setUpMidi() {
    try {
      sequencer = MidiSystem.getSequencer();
      sequencer.open();
      sequence = new Sequence (Sequence.PPQ, 4);
      track = sequence.createTrack();
      sequencer.setTempoInBPM(120);
      
    } catch(Exception e) {e.printStackTrace();}
  }
  
  /** Creates arrays of checkbox state, determining
    * if each checkbox is selected, determining
    * which instrument it is for.
    */
    
  public void buildTrackAndStart() {
    int[] eventList = null;
    
    //Deletes old track and creates new. 
    //Does not uncheck checkboxes.
    
    sequence.deleteTrack(track);
    track = sequence.createTrack();
    
    //For every row of instruments, create an array to store keys.
    
    for (int i = 0; i < 16; i++) {
      eventList = new int[16];
      
      int key = instruments[i];
      
      //For every checkbox in that row, if it is selected,
      //add the instrument's key.
      
      for (int j = 0; j < 16; j++ ) {
        
        JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
       
        if (jc.isSelected()) {
          eventList[j] = key;
        } else {
          eventList[j] = 0;
        }
      }
      
      //
      addEvents(eventList);

    }
    
    //Make empty event at the last beat so 
    track.add(makeEvent(192,9,1,0,15));
    
    //Sequencer plays new sequence in a loop, which plays track.
    try {
      
      sequencer.setSequence(sequence);
      sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
      sequencer.start();
      sequencer.setTempoInBPM(120);
    } catch(Exception e) {e.printStackTrace();}
  }
  
    /**
   * Adds key-on and key-off MIDI events to every
   * instrument for each beat, depending on whether 
   * the checkbox has been checked and the instrument's
   * key is at that particular index in the array.
   * If it's 0, the checkbox is empty and the key is 0.
   */
  
  public void addEvents(int[] list) {
    
    for (int i = 0; i < 16; i++) {
      int key = list[i];
      
      if (key != 0) {
        track.add(makeEvent(144,9,key, 100, i));
        track.add(makeEvent(128,9,key, 100, i+1)); 
      }
    }
  }
  
  /**
   * Creates MIDI events.
   *   comd: message type
   *   chan: channel
   *   one: note to play
   *   two: velocity
   *   tick: beat message is played on.
   */
  
  public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
    MidiEvent event = null;
    try {
      ShortMessage a = new ShortMessage();
      a.setMessage(comd, chan, one, two);
      event = new MidiEvent(a, tick);
    } catch(Exception e) {e.printStackTrace(); }
    return event;
    
  }
  
  /**
   * Creates new track upon pressing 'Start'.
   */

  public class myStartListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      buildTrackAndStart();
    }
  }
  
  /**
   * Stops track upon pressing 'Stop'.
   */
  
  public class myStopListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      sequencer.stop();
    }
  }
  
  /**
   * Speeds up Sequencer tempo upon pressing 'Tempo Up'.
   */
  
  public class myUpTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * 1.03));
      
    }
  }
  
  /**
   * Slows down Sequencer tempo upon pressing 'Tempo Up'
   */
  
  public class myDownTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * .97));
    }
  }
  
  /**
   * Clears all checkboxes upon pressing 'Clear'
   */
  
  public class myClearListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      sequencer.stop();
      for (int i = 0; i < 256; i++) {  
        checkboxList.get(i).setSelected(false);
      }
    }
  }
  
  /**
   * Saves current checkbox state to file in current
   * working directory.
   */
  
  public class mySendListener implements ActionListener { 
    public void actionPerformed(ActionEvent a) {
      
      boolean[] checkboxState = new boolean[256];
      
      for (int i = 0; i < 256; i++) {
        
        JCheckBox check = (JCheckBox) checkboxList.get(i);
        if (check.isSelected()) {
          checkboxState[i] = true;
        }
      }
      
      try {
        FileOutputStream fileStream = new FileOutputStream(new File ("Checkbox.ser"));
        ObjectOutputStream os = new ObjectOutputStream(fileStream);
        os.writeObject(checkboxState);
      } catch(Exception ex) {
        ex.printStackTrace();
      }
    }
  }
  
  /**
   * Loads checkbox state from file in working directory
   * named 'checkbox.ser'.
   */
  
  public class myReadInListener implements ActionListener { 
    public void actionPerformed(ActionEvent a) {
      boolean[] checkboxState = null;
      try {
        FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
        ObjectInputStream is = new ObjectInputStream(fileIn);
        checkboxState = (boolean[]) is.readObject();
        
      } catch(Exception ex) {
        ex.printStackTrace();
      }
      
      for (int i = 0; i < 256; i++) {
        JCheckBox check = (JCheckBox) checkboxList.get(i);
        if (checkboxState[i]) {
          check.setSelected(true);
        } else {
          check.setSelected(false);
        }
      }
      
      sequencer.stop();
      buildTrackAndStart();
    
    }
  }
  
  /**
   * Sends contents of text area message to PrintWriter
   * and clears text area upon pressing enter.
   */
  
  class myTextListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      
      out.println(textField.getText());
      textField.setText("");
    }
  }
  
  /**
   * Attempt to this client to the server upon
   * pressing 'Connect to Chat Server'.
   */
  
  class myConnectListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      try {
      new ChatClient().execute();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }  
      private String getName() {
        return JOptionPane.showInputDialog(
            theFrame,
            "Choose your screen name:",
            "BeatBox Chat",
            JOptionPane.PLAIN_MESSAGE);
    }
      
    private String connectServer() {
        return JOptionPane.showInputDialog(
            theFrame,
            "Enter IP Address of Chat Server:",
            "BeatBox Chat",
            JOptionPane.QUESTION_MESSAGE);
    }

  /**
   */
    
    class ChatClient extends SwingWorker<Void, Void> {
    
      public Void doInBackground() throws IOException {
        textField.setFocusable(true);
        textField.requestFocus();
        
        String serverAddress = connectServer();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            if (line.startsWith("USERNAME")) {
                out.println(getName());
            } else if (line.startsWith("ACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageField.append(line.substring(8) + "\n");
            }
            else 
            return null;
        }
      }
    };  
}
      