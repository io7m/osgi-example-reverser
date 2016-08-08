package com.io7m.reverser.tcp_client;

import com.io7m.reverser.api.ReverserServiceType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component(scope = ServiceScope.PROTOTYPE)
public final class TCPClientReverserService implements ReverserServiceType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPClientReverserService.class);
  }

  private TCPClient client;
  private Thread thread;

  public TCPClientReverserService()
  {

  }

  @Activate
  private void activate(final TCPClientConfig config)
    throws IOException
  {
    final InetSocketAddress address =
      new InetSocketAddress(config.host(), config.port());

    TCPClientReverserService.LOG.debug("{}, starting client: {}", this, address);

    this.client = new TCPClient(address);
    this.thread = new Thread(this.client);
    this.thread.setName("tcp-client-" + this.thread.getId());
    this.thread.start();
  }

  @Deactivate
  private void deactivate()
    throws IOException
  {
    TCPClientReverserService.LOG.info("{}: shutting down client", this);
    this.client.close();
    try {
      this.thread.join();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String reversed(final String text)
    throws Exception
  {
    return this.client.reversed(text);
  }
}
