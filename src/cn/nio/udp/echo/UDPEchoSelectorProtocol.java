/*
 * @class UDPEchoSelectorProtocol.java
 * @author ccfeng
 * @date 2015Äê4ÔÂ20ÈÕ
 * 
 */
package cn.nio.udp.echo;

import java.net.SocketAddress;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.io.IOException;

public class UDPEchoSelectorProtocol implements EchoProtocol {
	private static final int ECHOMAX = 255; // Maximum size of echo datagram
	private Charset charset = Charset.forName("UTF-8");

	static class ClientRecord {
		public SocketAddress clientAddress;
		public ByteBuffer buffer = ByteBuffer.allocate(ECHOMAX);
	}

	public void handleAccept(SelectionKey key) throws IOException {

	}

	public void handleRead(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clntRec = (ClientRecord) key.attachment();
		//clntRec.buffer.flip();
		//String data = decode(clntRec.buffer);
		//System.out.println("[" + channel.getLocalAddress() + "]# " + data);
		
		clntRec.buffer.clear(); // Prepare buffer for receiving
		clntRec.clientAddress = channel.receive(clntRec.buffer);

		if (clntRec.clientAddress != null) { // Did we receive something?
			// Register write with the selector
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	public void handleWrite(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clntRec = (ClientRecord) key.attachment();
		clntRec.buffer.flip(); // Prepare buffer for sending
		int bytesSent = channel.send(clntRec.buffer, clntRec.clientAddress);
		if (bytesSent != 0) { // Buffer completely written?
			// No longer interested in writes
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	public String decode(ByteBuffer buffer) {
		CharBuffer charBuffer = charset.decode(buffer);
		return charBuffer.toString();
	}

	public ByteBuffer encode(String str) {
		return charset.encode(str);
	}
}
