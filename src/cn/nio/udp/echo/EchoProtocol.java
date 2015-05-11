/*
 * @class EchoProtocol.java
 * @author ccfeng
 * @date 2015Äê4ÔÂ20ÈÕ
 * 
 */
package cn.nio.udp.echo;

import java.nio.channels.SelectionKey;
import java.io.IOException;

public interface EchoProtocol {
	void handleAccept(SelectionKey key) throws IOException;

	void handleRead(SelectionKey key) throws IOException;

	void handleWrite(SelectionKey key) throws IOException;
}