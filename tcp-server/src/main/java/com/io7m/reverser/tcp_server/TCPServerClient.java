package com.io7m.reverser.tcp_server;

import com.io7m.reverser.api.ReverserServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

final class TCPServerClient implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPServer.class);
  }

  private final Socket socket;
  private final AtomicBoolean stop;
  private final ReverserServiceType reverser;

  TCPServerClient(
    final AtomicBoolean stop,
    final ReverserServiceType reverser,
    final Socket socket)
  {
    this.stop = stop;
    this.reverser = reverser;
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

            while (!this.stop.get()) {
              try {
                final String line = br.readLine();
                if (line == null) {
                  break;
                }

                TCPServerClient.LOG.debug("client: received: {}", line);
                final String reversed = this.reverser.reversed(line) + "\r\n";
                out.write(reversed.getBytes(StandardCharsets.UTF_8));
              } catch (final SocketTimeoutException e) {
                // Ignore read timeout
              } catch (final Exception e) {
                TCPServerClient.LOG.error("error during reverse: ", e);
              }
            }

            TCPServerClient.LOG.info(
              "client {} finished", close_socket.getRemoteSocketAddress());
          }
        }
      }
    } catch (final IOException e) {
      TCPServerClient.LOG.error("client I/O error: ", e);
    }
  }
}
