package com.io7m.reverser.tcp_server;

import com.io7m.reverser.api.ReverserServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TCPServer implements Closeable, Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPServer.class);
  }

  private final InetSocketAddress address;
  private final ExecutorService client_pool;
  private final ReverserServiceType reverser;
  private volatile boolean stop;
  private ServerSocket socket;

  public TCPServer(
    final InetSocketAddress address,
    final ReverserServiceType reverser)
    throws IOException
  {
    this.address = address;
    this.reverser = reverser;
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

    while (!this.stop) {
      try {
        final Socket client = this.socket.accept();
        TCPServer.LOG.debug("client: {}", client.getRemoteSocketAddress());
        this.client_pool.execute(new Client(client));
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
    this.stop = true;
    this.client_pool.shutdown();
    this.socket.close();
  }

  private final class Client implements Runnable
  {
    private final Socket socket;

    Client(final Socket socket)
    {
      this.socket = socket;
    }

    @Override
    public void run()
    {
      try (final Socket close_socket = this.socket) {
        close_socket.setSoTimeout(1000);
        close_socket.setKeepAlive(true);

        try (final InputStream is = close_socket.getInputStream()) {
          try (final OutputStream out = close_socket.getOutputStream()) {
            try (final BufferedReader br = new BufferedReader(
              new InputStreamReader(is, StandardCharsets.UTF_8))) {

              while (!TCPServer.this.stop) {
                try {
                  final String line = br.readLine();
                  if (line == null) {
                    break;
                  }

                  TCPServer.LOG.debug("client: received: {}", line);

                  final String reversed =
                    TCPServer.this.reverser.reversed(line) + "\r\n";
                  out.write(reversed.getBytes(StandardCharsets.UTF_8));
                } catch (final SocketTimeoutException e) {
                  // Ignore read timeout
                } catch (final Exception e) {
                  TCPServer.LOG.error("error during reverse: ", e);
                }
              }

              TCPServer.LOG.info(
                "client {} finished", close_socket.getRemoteSocketAddress());
            }
          }
        }
      } catch (final IOException e) {
        TCPServer.LOG.error("client I/O error: ", e);
      }
    }
  }
}
