/**
 * 
 */
package cn.nio.udp.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * <p>
 * Description:
 * </p>
 * 
 * @author ccfeng
 * @version 1.0, 2015-4-20
 * 
 */
public class UDPEchoServer {
	private boolean running = true;
	private int port = 8001;
	private Charset charset = Charset.forName("UTF-8");
	private int readThreads = 3;
	private int TIMEOUT = 5000;

	public UDPEchoServer() throws IOException {
		new Listen();

	}

	private class Listen extends Thread {
		private Selector listenSelector = null;
		private DatagramChannel serverSocketChannel = null;
		// private Reader[] readers = null;
		private int currentReader = 0;
		// private Random rand = new Random();
		private ExecutorService readPool;

		public Listen() throws IOException {

			ByteBuffer byteBuffer = ByteBuffer.allocate(65536);

			serverSocketChannel = DatagramChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().bind(new InetSocketAddress("localhost", port));

			listenSelector = Selector.open();
			serverSocketChannel.register(listenSelector, SelectionKey.OP_READ);

			this.setName("Server listener on " + port);
			System.out.println("[" + serverSocketChannel.getLocalAddress()
					+ "]# " + "server start...");
			start();
		}

		private synchronized Selector getSelector() {
			return listenSelector;
		}

		@Override
		public void run() {
			ByteBuffer buffer = ByteBuffer.allocate(1000);
			while (true) {
				try {
					int eventsCount = listenSelector.select();
					if (eventsCount > 0) {
						Iterator<SelectionKey> iterator = listenSelector
								.selectedKeys().iterator();
						while (iterator.hasNext()) {
							SelectionKey sk = (SelectionKey) iterator.next();
							iterator.remove();
							if (sk.isReadable()) {
								DatagramChannel datagramChannel = (DatagramChannel) sk
										.channel();
								datagramChannel.receive(buffer);
								buffer.flip();
								
								String data = decode(buffer);

								// The if is importance!!!
								if (data.indexOf("\n") == -1) {
									return;
								}
								
								String outputData = data.substring(0, data.indexOf("\n") + 1);
								System.out.print("[" + datagramChannel.getLocalAddress()
										+ "]# " + outputData);
								System.out.print("[" + datagramChannel.getRemoteAddress()
										+ "]# " + outputData);
								ByteBuffer outputBuffer = encode(outputData);
								while (outputBuffer.hasRemaining())
									datagramChannel.write(outputBuffer);

								ByteBuffer temp = encode(outputData);
								buffer.position(temp.limit());
								buffer.compact();

								/*
								CharBuffer charBuffer = Charset
										.defaultCharset().decode(buffer);
								System.out.println("receive message:"
										+ charBuffer.toString());
								buffer.clear();

								String echo = "This is the reply message from ·þÎñÆ÷¡£";
								ByteBuffer buffer1 = Charset.defaultCharset()
										.encode(echo);
								buffer1.flip();
								datagramChannel.write(buffer1);
								*/
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

		public void dispatch(SelectionKey key) throws ClosedChannelException,
				IOException {
			DatagramChannel server = (DatagramChannel) key.channel();
			// server.configureBlocking(false);
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			// Reader reader = getReader();
			// System.out.println("[" + server.getLocalAddress() + "]# "
			// + "Server receive a connect and reader[" + reader.getName()
			// + "] handled.. ");
			server.read(buffer);
			buffer.flip();
			String data = decode(buffer);

			// The if is importance!!!
			if (data.indexOf("\n") == -1) {
				System.out.println("=================");
				return;
			}
			String outputData = data.substring(0, data.indexOf("\n") + 1);
			System.out.print("[" + server.getLocalAddress() + "]# "
					+ outputData);
			// key.attach(buffer);
			// reader.startAdd();
			// reader.setKey(key);
			// reader.finishAdd();
		}

	}

	// private Object gate = new Object();

	public String decode(ByteBuffer buffer) {
		CharBuffer charBuffer = charset.decode(buffer);
		return charBuffer.toString();
	}

	public ByteBuffer encode(String str) {
		return charset.encode(str);
	}

	public static void main(String args[]) throws Exception {
		@SuppressWarnings("unused")
		final UDPEchoServer server = new UDPEchoServer();

	}
}