package com.io7m.reverser.tcp_server;

import com.io7m.reverser.api.ReverserServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TCPServer implements Closeable, Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPServer.class);
  }

  private final InetSocketAddress address;
  private final ExecutorService client_pool;
  private final ReverserServiceType reverser;
  private final AtomicBoolean stop;
  private ServerSocket socket;

  public TCPServer(
    final InetSocketAddress address,
    final ReverserServiceType reverser)
    throws IOException
  {
    this.address = address;
    this.reverser = reverser;
    this.stop = new AtomicBoolean(false);
    this.client_pool = Executors.newFixedThreadPool(10, r -> {
      final Thread th = new Thread(r);
      th.setName("tcp-server-client-" + th.getId());
      return th;
    });

    this.socket = new ServerSocket();
    this.socket.setReuseAddress(true);
    this.socket.setSoTimeout(1000);
    this.socket.bind(this.address);
  }

  @Override
  public void run()
  {
    TCPServer.LOG.debug("starting server: {}", this.address);

    while (!this.stop.get()) {
      try {
        final Socket client = this.socket.accept();
        TCPServer.LOG.debug("client: {}", client.getRemoteSocketAddress());
        this.client_pool.execute(
          new TCPServerClient(this.stop, this.reverser, client));
      } catch (final SocketTimeoutException e) {
        // Ignore accept timeout
      } catch (final IOException e) {
        if (!this.socket.isClosed()) {
          TCPServer.LOG.error("server I/O error: ", e);
        }
      }
    }

    TCPServer.LOG.info("shutdown request received");
  }

  @Override
  public void close()
    throws IOException
  {
    this.stop.set(true);
    this.client_pool.shutdown();
    this.socket.close();
  }

}
