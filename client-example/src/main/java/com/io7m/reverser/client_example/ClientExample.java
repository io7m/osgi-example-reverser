package com.io7m.reverser.client_example;

import com.io7m.reverser.api.ReverserServiceType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(immediate = true)
public final class ClientExample
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ClientExample.class);
  }

  private ReverserServiceType reverser;
  private Thread thread;

  public ClientExample()
  {

  }

  @Reference
  private void setReverser(final ReverserServiceType s)
  {
    this.reverser = s;
  }

  @Activate
  private void activate(final ComponentContext context)
  {
    ClientExample.LOG.debug("starting client example");

    this.thread = new Thread(() -> {
      try {
        ClientExample.LOG.debug("{}: sending", this);
        System.out.println(this.reverser.reversed("Message One"));
        ClientExample.LOG.debug("{}: sending", this);
        System.out.println(this.reverser.reversed("Message Two"));
        ClientExample.LOG.debug("{}: sending", this);
        System.out.println(this.reverser.reversed("Message Three"));
        ClientExample.LOG.debug("{}: finished", this);
      } catch (final Exception e) {
        ClientExample.LOG.error("{}: client error: ", this, e);
      } finally {
        context.disableComponent(this.getClass().getName());
      }
    });
    this.thread.setName("client-example-" + this.thread.getId());
    this.thread.start();
  }

  @Deactivate
  private void deactivate()
    throws InterruptedException
  {
    ClientExample.LOG.debug("{}: stopping client example", this);
    this.thread.interrupt();
    this.thread.join();
  }
}
