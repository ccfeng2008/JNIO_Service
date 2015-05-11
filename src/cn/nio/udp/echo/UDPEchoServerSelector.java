/*
 * @class UDPEchoServerSelector.java
 * @author ccfeng
 * @date 2015Äê4ÔÂ20ÈÕ
 * 
 */
package cn.nio.udp.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class UDPEchoServerSelector {

	private static final int TIMEOUT = 3000; // Wait timeout (milliseconds)

	public static void main(String[] args) throws IOException {
		// Create a selector to multiplex client connections.
		Selector selector = Selector.open();

		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(5500));
		channel.register(selector, SelectionKey.OP_READ,
				new UDPEchoSelectorProtocol.ClientRecord());

		UDPEchoSelectorProtocol echoSelectorProtocol = new UDPEchoSelectorProtocol();
		while (true) { // Run forever, receiving and echoing datagrams
			// Wait for task or until timeout expires
			if (selector.select(TIMEOUT) == 0) {
				//System.out.println("[" + channel.getLocalAddress() + "]# none data!" );
				continue;
			}

			// Get iterator on set of keys with I/O to process
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next(); // Key is bit mask

				// Client socket channel has pending data?
				if (key.isReadable())
					echoSelectorProtocol.handleRead(key);

				// Client socket channel is available for writing and
				// key is valid (i.e., channel not closed).
				if (key.isValid() && key.isWritable())
					echoSelectorProtocol.handleWrite(key);

				keyIter.remove();
			}
		}
	}

}
