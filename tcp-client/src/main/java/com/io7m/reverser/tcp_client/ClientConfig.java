package com.io7m.reverser.tcp_client;

public @interface ClientConfig
{
  String host() default "127.0.0.1";

  int port() default 9999;
}
