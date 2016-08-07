package com.io7m.reverser.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ReverserServiceType
{
  String reversed(String text)
    throws Exception;
}
