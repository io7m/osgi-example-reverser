package com.io7m.reverser.tcp_client;

import com.io7m.reverser.api.ReverserServiceType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(scope = ServiceScope.PROTOTYPE)
public final class Client implements ReverserServiceType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Client.class);
  }

  private final BlockingQueue<String> queue_in;
  private final BlockingQueue<String> queue_out;
  private ExecutorService client_exec;
  private volatile boolean stop;

  public Client()
  {
    this.queue_in = new ArrayBlockingQueue<>(10);
    this.queue_out = new ArrayBlockingQueue<>(10);
  }

  @Activate
  private void activate(final ClientConfig config)
    throws IOException
  {
    final InetSocketAddress address =
      new InetSocketAddress(config.host(), config.port());

    Client.LOG.debug("{}, starting client: {}", this, address);

    this.client_exec = Executors.newFixedThreadPool(1, r -> {
      final Thread th = new Thread(r);
      th.setName("tcp-client");
      return th;
    });

    this.client_exec.execute(() -> {
      try (final Socket socket = new Socket()) {
        socket.setReuseAddress(true);
        socket.setSoTimeout(1000);
        socket.connect(address);

        try (final OutputStream out = socket.getOutputStream()) {
          try (final InputStream in = socket.getInputStream()) {
            try (final BufferedReader br = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))) {

              while (!this.stop) {
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
        }

        Client.LOG.debug("{}: client finished", this);
      } catch (final IOException e) {
        Client.LOG.error("{}: could not start client: ", this, e);
      }
    });
  }

  @Deactivate
  private void deactivate()
    throws IOException
  {
    Client.LOG.info("{}: shutting down client", this);
    this.client_exec.shutdown();
    this.stop = true;
  }

  @Override
  public String reversed(final String text)
    throws InterruptedException
  {
    this.queue_in.add(text);
    return this.queue_out.take();
  }
}
