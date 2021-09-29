package org.exoplatform.commands;

public interface CommandProgress {

   public long getComplete();

   public long getAvailable();

   public int getAttempts();
}
