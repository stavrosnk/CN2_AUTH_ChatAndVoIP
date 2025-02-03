package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.DataLine;


public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	InetAddress receiverAddress;
	static String KEY = "12345678901234567890123456789012";
	static String ALGORITHM = "AES";
	static SecretKey SECRET_KEY = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
	Thread Call;
    boolean X;
    
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");			
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args) throws Exception{
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("CN2 - AUTH");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  
		//do {
		DatagramSocket textSocket = null;
		try {
			textSocket = new DatagramSocket(5000);
		    byte[] buffer = new byte[1024];
		    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		    final DatagramSocket finalTextSocket = textSocket;
		    
		    new Thread(() -> {
		        while (true) {
		            try {
		                finalTextSocket.receive(packet); 
		                byte[] encryptedBytes = new byte[packet.getLength()];
		                System.arraycopy(packet.getData(), 0, encryptedBytes, 0, packet.getLength());
		                String decryptedMessage = decrypt(encryptedBytes); 
		                textArea.append("Received: " + decryptedMessage + "\n");
		            } catch (Exception e) {
		                e.printStackTrace();
		                textArea.append("Error processing incoming message.\n");
		            }
		        }
		    }).start();
		} catch (Exception ex) {
		    ex.printStackTrace();
		    textArea.append("Error initializing textSocket.\n");
		}

		new Thread(() -> {
		    DatagramSocket audioSocket = null;
		    try {
		        audioSocket = new DatagramSocket(5001); 
		        AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true);
		        SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
		        speaker.open(format);
		        speaker.start();

		        byte[] buffer = new byte[1024];
		        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		        while (true) {
		            audioSocket.receive(packet);
		            speaker.write(packet.getData(), 0, packet.getLength());
		        }

		    } catch (Exception ex) {
		        ex.printStackTrace();
		    } finally {
		        if (audioSocket != null && !audioSocket.isClosed()) {
		            audioSocket.close(); 
		        }
		    }
		}).start();
		//}while(true);
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@SuppressWarnings("removal")
	@Override
	public void actionPerformed(ActionEvent e){
		
	

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			new Thread(() -> {
			try(DatagramSocket socket = new DatagramSocket()){
				String message = inputTextField.getText();
				byte[] buffer = encrypt(message);
				InetAddress receiverAddress = InetAddress.getByName("192.168.1.15");
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverAddress, 5000);
				socket.send(packet);
				textArea.append("Sent: " + inputTextField.getText() + "\n");
			}catch (Exception ex) {
				ex.printStackTrace();
				textArea.append("Error during text transmission.\n");
			}
			}).start();
		}else if(e.getSource() == callButton){
			
			if (Call != null && Call.isAlive()) {
				X = false;
	            Call.interrupt();
	            textArea.append("Call stopped.\n");
	            return;
			}else {
				X = true;
			}
				
				Call = new Thread(() -> {
				textArea.append("Starting call...\n");
				try(DatagramSocket socket = new DatagramSocket()) {
					AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true);
					TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
					microphone.open(format);
					microphone.start();
					byte[] buffer = new byte[1024];
					DatagramPacket packet;
					InetAddress receiverAddress = InetAddress.getByName("192.168.1.15");
					while ( X) {
						int bytesRead = microphone.read(buffer, 0, buffer.length);
						boolean detectsSound = false;
						for (int i = 0; i < bytesRead; i++) {
							if (buffer[i] != 0) {
								detectsSound = true;
								;
							}
						}
						if (detectsSound) {
							packet = new DatagramPacket(buffer, bytesRead, receiverAddress, 5001);
							socket.send(packet);
						}
					}
					microphone.stop();
					microphone.close();
				}catch (Exception ex) {
					ex.printStackTrace();
					textArea.append("Error during audio transmission.\n");
				}
				
			});
			Call.start();
	}
		
			

	}
	 public static byte[] encrypt(String data) throws Exception {
	        Cipher cipher = Cipher.getInstance(ALGORITHM);
	        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
	        return cipher.doFinal(data.getBytes());
	    }

	  public static String decrypt(byte[] data) throws Exception {
	        Cipher cipher = Cipher.getInstance(ALGORITHM);
	        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
	        byte[] decryptedBytes = cipher.doFinal(data);
	        return new String(decryptedBytes);
	  }
	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
