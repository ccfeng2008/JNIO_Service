/**
 * 
 */
package cn.nio.tcp.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
public class TCPEchoServer {
	private boolean running = true;
	private int port = 8000;
	private Charset charset = Charset.forName("UTF-8");
	private int readThreads = 3;

	public TCPEchoServer() throws IOException {
		new Listen().start();

	}

	private class Listen extends Thread {
		private Selector listenSelector = null;
		private ServerSocketChannel serverSocketChannel = null;
		private Reader[] readers = null;
		private int currentReader = 0;
		// private Random rand = new Random();
		private ExecutorService readPool;

		public Listen() throws IOException {
			listenSelector = Selector.open();
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.socket().setReuseAddress(true);
			serverSocketChannel.socket().bind(new InetSocketAddress(port));
			serverSocketChannel
					.register(listenSelector, SelectionKey.OP_ACCEPT);

			readers = new Reader[readThreads];
			readPool = Executors.newFixedThreadPool(readThreads);
			for (int i = 0; i < readThreads; i++) {
				Selector readSelector = Selector.open();
				Reader reader = new Reader(readSelector);
				readers[i] = reader;
				readers[i].setName("Reader " + i);
				readPool.execute(reader);
			}

			this.setName("Server listener on " + port);
			this.setDaemon(true);
			System.out.println("[" + serverSocketChannel.getLocalAddress()
					+ "]# " + "server start...");
		}

		private synchronized Selector getSelector() {
			return listenSelector;
		}

		private Reader getReader() {
			currentReader = (currentReader + 1) % readers.length;
			return readers[currentReader];
		}

		@Override
		public void run() {
			while (running) {
				SelectionKey key = null;
				try {
					getSelector().select();
					Iterator<SelectionKey> iter = getSelector().selectedKeys()
							.iterator();

					while (iter.hasNext()) {
						key = iter.next();
						iter.remove();
						if (key.isValid()) {
							if (key.isAcceptable())
								accept(key);
						}
						key = null;
					}
				} catch (OutOfMemoryError e) {
					// we can run out of memory if we have too many threads
					// log the event and sleep for a minute and give
					// some thread(s) a chance to finish
					try {
						Thread.sleep(60000);
					} catch (Exception ie) {
					}
				} catch (Exception e) {

				}

			}
		}

		public void accept(SelectionKey key) throws ClosedChannelException,
				IOException {
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel channel;
			while ((channel = server.accept()) != null) {
				channel.configureBlocking(false);
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				Reader reader = getReader();
				System.out.println("[" + server.getLocalAddress() + "]# "
						+ "Server receive a connect and reader["
						+ reader.getName() + "] handled.. ");
				reader.startAdd();
				SelectionKey readKey = reader.registerChannel(channel);
				readKey.attach(buffer);
				reader.finishAdd();

			}
		}

		private class Reader extends Thread {
			private volatile boolean adding = false;
			private Selector readSelector = null;

			Reader(Selector readSelector) {
				this.readSelector = readSelector;
			}

			public void run() {
				synchronized (this) {
					while (running) {
						SelectionKey key = null;
						try {
							readSelector.select();
							while (adding) {
								this.wait(1000);
							}
							Iterator<SelectionKey> iter = readSelector
									.selectedKeys().iterator();
							while (iter.hasNext()) {
								key = iter.next();
								iter.remove();
								if (key.isValid()) {
									if (key.isReadable()) {
										doReadAndSend(key);
									}
								}
								key = null;
							}
						} catch (InterruptedException e) {

						} catch (IOException ex) {

						}
					}
				}
			}

			public void doReadAndSend(SelectionKey key) throws IOException {
				receive(key);
				send(key);
			}

			public void startAdd() {
				adding = true;
				readSelector.wakeup();
			}

			public synchronized SelectionKey registerChannel(
					SocketChannel channel) throws IOException {
				return channel.register(readSelector, SelectionKey.OP_READ);
			}

			public synchronized void finishAdd() {
				adding = false;
				this.notify();
			}

			// the effect is low, because the OP_WRITE status is all time
			public void send(SelectionKey key) throws IOException {
				ByteBuffer buffer = (ByteBuffer) key.attachment();
				SocketChannel socketChannel = (SocketChannel) key.channel();
				buffer.flip();
				// System.out.println("#server send.....");
				String data = decode(buffer);

				// The if is importance!!!
				if (data.indexOf("\n") == -1) {
					return;
				}
				String outputData = data.substring(0, data.indexOf("\n") + 1);
				System.out.print("[" + socketChannel.getLocalAddress()
						+ "]# " + outputData);
				ByteBuffer outputBuffer = encode(outputData);
				while (outputBuffer.hasRemaining())
					socketChannel.write(outputBuffer);

				ByteBuffer temp = encode(outputData);
				buffer.position(temp.limit());
				buffer.compact();

				if (outputData.equals("bye\r\n")) {
					key.cancel();
					socketChannel.close();
					System.out.println("[" + socketChannel.getLocalAddress()
							+ "]# " + "closed a connection and reader["
							+ getName() + "] released!");
				}
			}

			public void receive(SelectionKey key) throws IOException {
				ByteBuffer buffer = (ByteBuffer) key.attachment();

				SocketChannel socketChannel = (SocketChannel) key.channel();
				ByteBuffer readBuff = ByteBuffer.allocate(32);
				socketChannel.read(readBuff);
				readBuff.flip();

				buffer.limit(buffer.capacity());
				buffer.put(readBuff);
			}

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
		final TCPEchoServer server = new TCPEchoServer();

	}
}