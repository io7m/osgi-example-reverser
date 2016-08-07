package com.io7m.reverser.simple;

import com.io7m.reverser.api.ReverserServiceType;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public final class ReverserSimple implements ReverserServiceType
{
  public ReverserSimple()
  {

  }

  public String reversed(final String text)
  {
    return new StringBuilder(text).reverse().toString();
  }
}
