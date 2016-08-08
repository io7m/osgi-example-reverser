package com.io7m.reverser.backoff;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ExponentialBackoff
{
  private ExponentialBackoff()
  {

  }

  public interface Op<T, E extends Exception>
  {
    T exec()
      throws E;
  }

  public static <T, E extends Exception> T tryRepeatedly(
    final int max_tries,
    final Op<T, E> run,
    final Consumer<Throwable> errors)
    throws E
  {
    for (int attempt = 0; attempt < max_tries; ++attempt) {
      try {
        return run.exec();
      } catch (final Throwable e) {
        if (e instanceof Error) {
          throw (Error) e;
        }
        if (attempt + 1 == max_tries) {
          throw e;
        }

        errors.accept(e);
        try {
          TimeUnit.SECONDS.sleep((long) StrictMath.pow(2.0, (double) attempt));
        } catch (final InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }

    throw new AssertionError("Unreachable code");
  }
}
