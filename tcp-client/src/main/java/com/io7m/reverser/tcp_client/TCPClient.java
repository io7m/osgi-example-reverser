package com.io7m.reverser.tcp_client;

import com.io7m.reverser.api.ReverserServiceType;
import com.io7m.reverser.backoff.ExponentialBackoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TCPClient implements ReverserServiceType, Runnable, Closeable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPClient.class);
  }

  private final BlockingQueue<String> queue_in;
  private final BlockingQueue<String> queue_out;
  private final Socket socket;
  private final AtomicBoolean stop;

  public TCPClient(
    final InetSocketAddress address)
    throws IOException
  {
    this.queue_in = new ArrayBlockingQueue<>(10);
    this.queue_out = new ArrayBlockingQueue<>(10);
    this.stop = new AtomicBoolean(false);

    this.socket = ExponentialBackoff.tryRepeatedly(5, () -> {
      Socket s = null;
      try {
        s = new Socket();
        s.setReuseAddress(true);
        s.setSoTimeout(1000);
        s.connect(address);
        return s;
      } catch (final IOException e) {
        s.close();
        throw e;
      }
    }, e -> TCPClient.LOG.error("error attempting to connect: ", e));
  }

  @Override
  public String reversed(final String text)
    throws Exception
  {
    this.queue_in.add(text);
    return this.queue_out.take();
  }

  @Override
  public void run()
  {
    try (final OutputStream out = this.socket.getOutputStream()) {
      try (final InputStream in = this.socket.getInputStream()) {
        try (final BufferedReader br = new BufferedReader(
          new InputStreamReader(in, StandardCharsets.UTF_8))) {

          while (!this.stop.get()) {
            try {
              final String take = this.queue_in.poll(1L, TimeUnit.SECONDS);
              if (take != null) {
                final String send = take + "\r\n";
                out.write(send.getBytes(StandardCharsets.UTF_8));
                out.flush();
              }

              final String line = br.readLine();
              if (line == null) {
                break;
              }

              this.queue_out.add(line);
            } catch (final SocketTimeoutException e) {
              // Ignore timeout
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
    } catch (final SocketException e) {
      if (!this.socket.isClosed()) {
        TCPClient.LOG.error("socket error: ", e);
      }
    } catch (final IOException e) {
      TCPClient.LOG.error("client I/O error: ", e);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    this.stop.set(true);
    this.socket.close();
  }
}
