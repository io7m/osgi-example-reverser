package com.io7m.reverser.tcp_server;

import com.io7m.reverser.api.ReverserServiceType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component(immediate = true)
public final class TCPServerService
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TCPServerService.class);
  }

  private ReverserServiceType reverser;
  private Thread thread;
  private TCPServer server;

  public TCPServerService()
  {

  }

  @Reference
  private void setReverser(
    final ReverserServiceType in_reverser)
  {
    this.reverser = in_reverser;
  }

  @Activate
  private void activate(final TCPServerConfig config)
    throws IOException
  {
    final InetSocketAddress address =
      new InetSocketAddress(config.host(), config.port());

    TCPServerService.LOG.info("starting server");
    this.server = new TCPServer(address, this.reverser);
    this.thread = new Thread(this.server);
    this.thread.setName("tcp-server-" + this.thread.getId());
    this.thread.start();
  }

  @Deactivate
  private void deactivate()
    throws IOException
  {
    TCPServerService.LOG.info("shutting down server");
    this.server.stop();
    this.thread.interrupt();
  }
}
