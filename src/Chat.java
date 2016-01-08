import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.TextArea;
import java.awt.Button;
import java.awt.TextField;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;

@SuppressWarnings("serial")
public class Chat extends JFrame {

	private JPanel contentPane;
	private static TextArea textAreaLog;
	private TextArea textAreaMsg;
	private Button buttonSend;
	private static TextField textFieldIPAddr;

	private static int serverPort;
	private static Chat thisFrame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		if (args.length < 1)
			serverPort = 3333;
		else
			serverPort = Integer.parseInt(args[0]);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Chat frame = new Chat();
					frame.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Chat() {
		thisFrame = this;
		setTitle("Chat");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 536);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		textAreaLog = new TextArea();
		textAreaLog.setEditable(false);

		contentPane.add(textAreaLog, BorderLayout.NORTH);

		textAreaMsg = new TextArea();
		contentPane.add(textAreaMsg, BorderLayout.CENTER);

		buttonSend = new Button("Send");
		buttonSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Socket echoSocket = null;
				PrintWriter out = null;
				String dest = textFieldIPAddr.getText();

				try {
					echoSocket = new Socket(dest, serverPort);
					out = new PrintWriter(echoSocket.getOutputStream(), true);
				} catch (UnknownHostException ex) {
					textAreaLog.append(dest + ": Don't know about host.\n");
				} catch (IOException ex) {
					textAreaLog.append(dest + ": Couldn't get I/O for "
							+ "the connection.\n");
				}

				//int key = (int) (rand.nextDouble() * 65535 + 1);
				
				int key = 100;
				out.println(key);
				
				char[] output = ChatProtocol.encodeString(textAreaMsg.getText(), key);
				out.println(output.length);
//				out.print(output);
				for (int i = 0; i < output.length; i++) {
					out.println((int) output[i]);
				}
				textAreaLog.append("                    " + dest + "<- "
						+ textAreaMsg.getText() + "\n");
				textAreaMsg.setText("");
				// System.out.println(key);

				out.close();
				try {
					echoSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		contentPane.add(buttonSend, BorderLayout.EAST);

		textFieldIPAddr = new TextField();
		textFieldIPAddr.setText("127.0.0.1");
		contentPane.add(textFieldIPAddr, BorderLayout.SOUTH);

		//
		Thread t1 = new Thread(new Server());
		t1.start();

	}

	public static class Server implements Runnable {

		@Override
		public void run() {
			textAreaLog.append("Welcome to Chat\n");

			ServerSocket serverSocket = null;
			textAreaLog.append("Listening on port " + serverPort + "\n");

			String myAddr = null;

			try {
				boolean isLoopBack = true;
				Enumeration<NetworkInterface> en = NetworkInterface
						.getNetworkInterfaces();
				while (en.hasMoreElements()) {
					NetworkInterface ni = en.nextElement();
					if (ni.isLoopback())
						continue;

					Enumeration<InetAddress> inetAddresses = ni
							.getInetAddresses();
					while (inetAddresses.hasMoreElements()) {
						InetAddress ia = inetAddresses.nextElement();
						if (ia.getHostAddress() != null
								&& ia.getHostAddress().indexOf(".") != -1) {
							myAddr = ia.getHostAddress();
							isLoopBack = false;
							break;
						}
					}
					if (!isLoopBack)
						break;
				}
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			textAreaLog.append("My IP address: " + myAddr + "\n");

			try {
				serverSocket = new ServerSocket(serverPort);
			} catch (IOException e) {
				System.err.println("Could not listen on port.");
				System.exit(1);
			}

			Socket clientSocket = null;
			while (true) {
				try {
					clientSocket = serverSocket.accept();
				} catch (IOException e) {
					System.err.println("Accept failed.");
					System.exit(1);
				}

				PrintWriter out;
				BufferedReader in;
				try {
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(
							clientSocket.getInputStream()));

					int key;

					key = Integer.parseInt(in.readLine());
					int length = Integer.parseInt(in.readLine());
					
					char[] buffer = new char[length];
					//in.read(buffer, 0, length);
					for (int i = 0; i < length; i++) {
						buffer[i] = (char) Integer.parseInt(in.readLine());
					}
					String clientAddr = clientSocket.getInetAddress().getHostAddress();
					textAreaLog.append(clientAddr + "-> "
							+ new String(ChatProtocol.decodeString(buffer, key)) + "\n");

					// flash the window
					if (!thisFrame.isFocused()) {
						try {
							final int sleepTime = 50;
							for (int i = 0; i < 2; i++) {
								thisFrame.setVisible(false);
								Thread.sleep(sleepTime);
								thisFrame.setVisible(true);
								Thread.sleep(sleepTime);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					thisFrame.toFront();

					out.close();
					in.close();
					clientSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// serverSocket.close(); }
		}
	}

	static class ChatProtocol {
		public static char[] encodeString(String input, int key) {
			System.out.println("encode");
			System.out.println(input);
			System.out.println("key: " + key);
			char[] chars = input.toCharArray();
			
			for (int i = 0; i < chars.length; i++) {
				System.out.print(chars[i] + " ");
				System.out.print((int) chars[i] + " ");
				chars[i] += key; 
				System.out.print((int) chars[i] + ",");
			}
			System.out.println();
			return chars;
		}
		
		public static char[] decodeString(char[] input, int key) {
			System.out.println("decode");
			System.out.println("key: " + key);
			char[] chars = new char[input.length];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = input[i];
				System.out.print((int) chars[i] + " ");
				chars[i] -= key; 
				System.out.print((int) chars[i] + ",");
			}
			System.out.println();
			return chars;
		}		
		
	}
}