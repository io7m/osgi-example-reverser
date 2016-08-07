package com.io7m.reverser.tcp_server;

public @interface TCPServerConfig
{
  String host() default "127.0.0.1";

  int port() default 9999;
}
