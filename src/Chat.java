import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.TextArea;
import java.awt.Button;
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
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.FlowLayout;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class Chat extends JFrame {

	private JPanel contentPane;
	private static TextArea textAreaLog;
	private TextArea textAreaMsg;
	private Button buttonSend;

	private static int serverPort;
	private static ServerSocket serverSocket = null;
	private static Thread serverThread = null;
	private static Chat thisFrame;
	private JPanel panel;
	private JLabel lblNewLabel;
	private JLabel lblPort;
	private JTextField textFieldDestPort;
	private JPanel panel_1;
	private JTextField textFieldIPAddr;
	private JPanel panel_2;
	private JLabel lblListeningPort;
	private JTextField textFieldListeningPort;
	private JButton btnChangePort;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
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
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InstantiationException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IllegalAccessException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (UnsupportedLookAndFeelException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
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

		panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);

		lblNewLabel = new JLabel("Destination:");
		panel.add(lblNewLabel);

		textFieldIPAddr = new JTextField();
		textFieldIPAddr.setText("127.0.0.1");
		panel.add(textFieldIPAddr);
		textFieldIPAddr.setColumns(10);

		lblPort = new JLabel("Port:");
		panel.add(lblPort);

		textFieldDestPort = new JTextField();
		textFieldDestPort.setText("3333");
		panel.add(textFieldDestPort);
		textFieldDestPort.setColumns(10);

		panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		textAreaMsg = new TextArea();
		panel_1.add(textAreaMsg);

		buttonSend = new Button("Send");
		panel_1.add(buttonSend, BorderLayout.EAST);

		panel_2 = new JPanel();
		panel_1.add(panel_2, BorderLayout.NORTH);
		panel_2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		lblListeningPort = new JLabel("Listening Port:");
		panel_2.add(lblListeningPort);

		textFieldListeningPort = new JTextField();
		textFieldListeningPort.setText("3333");
		panel_2.add(textFieldListeningPort);
		textFieldListeningPort.setColumns(10);
		
		btnChangePort = new JButton("Change Port");
		btnChangePort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				changeListeningPort();
			}
		});
		panel_2.add(btnChangePort);
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
					textAreaLog.append(dest + ": Couldn't get I/O for " + "the connection.\n");
				}

				// int key = (int) (rand.nextDouble() * 65535 + 1);

				int key = 100;
				out.println(key);

				char[] output = ChatProtocol.encodeString(textAreaMsg.getText(), key);
				out.println(output.length);
				// out.print(output);
				for (int i = 0; i < output.length; i++) {
					out.println((int) output[i]);
				}
				textAreaLog.append("                    " + dest + "<- " + textAreaMsg.getText() + "\n");
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
		
		changeListeningPort();
	}

	@SuppressWarnings("deprecation")
	private void changeListeningPort() {
		if (serverSocket != null) {
			serverThread.stop();
			try {
				serverSocket.close();
			} catch (IOException e) {
				textAreaLog.append(e.toString());
			}
		}
		serverPort = Integer.parseInt(textFieldListeningPort.getText());
		serverThread = new Thread(new Server());
		serverThread.start();
	}

	public static class Server implements Runnable {

		@Override
		public void run() {
			textAreaLog.append("Welcome to Chat\n");
			textAreaLog.append("Listening on port " + serverPort + "\n");

			String myAddr = null;

			try {
				boolean isLoopBack = true;
				Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
				while (en.hasMoreElements()) {
					NetworkInterface ni = en.nextElement();
					if (ni.isLoopback())
						continue;

					Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
					while (inetAddresses.hasMoreElements()) {
						InetAddress ia = inetAddresses.nextElement();
						if (ia.getHostAddress() != null && ia.getHostAddress().indexOf(".") != -1) {
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
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

					int key;

					key = Integer.parseInt(in.readLine());
					int length = Integer.parseInt(in.readLine());

					char[] buffer = new char[length];
					// in.read(buffer, 0, length);
					for (int i = 0; i < length; i++) {
						buffer[i] = (char) Integer.parseInt(in.readLine());
					}
					String clientAddr = clientSocket.getInetAddress().getHostAddress();
					textAreaLog.append(clientAddr + "-> " + new String(ChatProtocol.decodeString(buffer, key)) + "\n");

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
