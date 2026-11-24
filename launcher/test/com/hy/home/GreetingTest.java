package com.hy.home;

import java.time.LocalDate;
import java.time.LocalTime;

public final class GreetingTest {
  private GreetingTest() {}

  public static void run() {
    T.eq("après-midi", "Bonjour 👋", Greeting.hello(LocalTime.of(14, 20)));
    T.eq("5h", "Bonjour 👋", Greeting.hello(LocalTime.of(5, 0)));
    T.eq("17h59", "Bonjour 👋", Greeting.hello(LocalTime.of(17, 59)));
    T.eq("18h", "Bonsoir 👋", Greeting.hello(LocalTime.of(18, 0)));
    T.eq("4h59", "Bonsoir 👋", Greeting.hello(LocalTime.of(4, 59)));
    T.eq("date", "jeudi 1 janvier", Greeting.date(LocalDate.of(2026, 1, 1)));
    T.eq("heure", "09:05", Greeting.clock(LocalTime.of(9, 5)));
  }
}
