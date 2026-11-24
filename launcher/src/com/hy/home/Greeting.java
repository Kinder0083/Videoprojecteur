package com.hy.home;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textes de la barre du haut : salutation, date et heure. */
public final class Greeting {
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
  private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

  private Greeting() {}

  /** « Bonjour 👋 » de 05:00 à 17:59, sinon « Bonsoir 👋 ». */
  public static String hello(LocalTime t) {
    int h = t.getHour();
    return (h >= 5 && h < 18) ? "Bonjour 👋" : "Bonsoir 👋";
  }

  public static String date(LocalDate d) { return d.format(DATE); }

  public static String clock(LocalTime t) { return t.format(CLOCK); }
}
