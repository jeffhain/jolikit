Jolikit is a Java library which APIs only depend on the most basic JDK classes
and Java language features, for easier translations into other languages,
and which aims to abstract away things not abstracted away by the JDK,
to serve as a low lock-in decoupling basis.

There is no top-level class or package dependency cycle in the code,
which should simplify a lot porting into languages such as Go.

See README-BWD.md for more info about the 2D UIs API.

# License

Apache License V2.0

# Current external dependencies

- src/main/java:
  - Java 6
  - BWD bindings libraries (JavaFX 8, SWT, etc.)

- src/build/java:
  - Java 6
  - Jadecy 2.0.1

- src/test/java:
  - Java 6 (or newer, to avoid an issue (looks like JDK-8012453)
    with Runtime.getRuntime().exec(String) for tests launching JVMs)
  - BWD bindings libraries (JavaFX 8, SWT, etc.)
  - Jadecy 2.0.1
  - JUnit 3.8.1

- src/samples/java:
  - Java 6
  - BWD bindings libraries used in samples

# Principal features

- net.jolikit.lang:
  Low level technical code.
  Principal classes:
  - LangUtils
  - NumbersUtils
  - InterfaceFactory
  - InterfaceBooleanCondition
  - XxxWrapper: To output primitives or references without garbage.
  - ThinTime: About as accurate as System.currentTimeMillis(),
    but possibly much more precise.
  - HeisenLogger: Low-overhead logger to debug concurrent treatments.

- net.jolikit.threading.locks:
  Provides an API to abstract away whether locking and waiting is done using
  a monitor or an implementation of Lock, and an API extending Lock
  with check methods to help guard against deadlocks or wrongfully locked
  or unlocked sections.
  Also provides default implementations of these APIs.
  Principal entry points:
  - net.jolikit.threading.locks.InterfaceCondilock
  - net.jolikit.threading.locks.InterfaceCheckerLock

- net.jolikit.threading.prl:
  Provides APIs to abstract away parallelization.
  Also provides default implementations of these APIs.
  Principal entry points:
  - net.jolikit.threading.prl.InterfaceParallelizer
  - net.jolikit.threading.prl.InterfaceSplittable
  - net.jolikit.threading.prl.InterfaceSplitmergable

- net.jolikit.time:
  Provides APIs for clocks to abstract away time, and APIs for scheduling
  treatments based on such clocks, either and transparently:
  - with "hard scheduling", i.e. using one or more threads, according to some
    wall-clock time (possibly dynamically accelerated or slowed down), or
  - with "soft scheduling", i.e. in a single thread, according to the time
    of a virtual clock, which allows for both (as) fast (as possible) and
    deterministic execution (useful for Monte Carlo simulations, for tests,
    to help figure out whether a bug is related to multi-threading, etc.).
  Also provides default implementations of these APIs.
  Principal entry points:
  - net.jolikit.time.clocks.InterfaceClock
  - net.jolikit.time.sched.InterfaceScheduler

- net.jolikit.bwd:
  Provides a simple 2D UI API to abstract away UI libraries, by defining types
  and interfaces corresponding to related basic concepts and use cases
  (UI thread scheduling, windowing and related events, graphics (drawing
  primitives, text, images, rectangular clipping, translations and 90 degrees
  rotations), fonts, cursors, and devices (mouse, wheel and keyboard) events).
  Also provides implementations of this API based on various UI libraries.
  Principal entry points:
  - net.jolikit.bwd.api.InterfaceBwdBinding
  More info in README-BWD.md

# Build output

Jars content and inter-dependencies,
with compilation target version in parentheses:

- jolikit.jar (1.6):
  Everything except BWD bindings implementations
  (even those based on AWT/Swing).

- jolikit-bwd-impl-utils.jar (1.6):
  Utilities for BWD bindings implementations.
  Depends on:
  - jolikit.jar

- jolikit-bwd-impl-awt.jar (1.6):
  A BWD binding based on AWT.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

- jolikit-bwd-impl-swing.jar (1.6):
  A BWD binding based on Swing.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar
  - jolikit-bwd-impl-awt.jar

- jolikit-bwd-impl-jfx.jar (1.6):
  A BWD binding based on JavaFX.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

- jolikit-bwd-impl-swt.jar (1.6):
  A BWD binding based on SWT.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

- jolikit-bwd-impl-lwjgl3.jar (1.8, due to a lambda in LWJGL3):
  A BWD binding based on LWJGL3.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar
  - jolikit-bwd-impl-awt.jar (for fonts)

- jolikit-bwd-impl-jogl.jar (1.6):
  A BWD binding based on JOGL/NEWT.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar
  - jolikit-bwd-impl-awt.jar (for fonts and images)

- jolikit-bwd-impl-qtj4.jar (1.6):
  A BWD binding based on QtJambi4.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

- jolikit-bwd-impl-algr5.jar (1.6):
  A BWD binding based on Allegro5.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

- jolikit-bwd-impl-sdl2.jar (1.6):
  A BWD binding based on SDL2.
  Depends on:
  - jolikit.jar
  - jolikit-bwd-impl-utils.jar

# Settings

- Load Jolikit code in an IDE, using JDK 8.
  You can also use JDK 6/7 or 9+ instead, but:
  - you will need to add JavaFX 8 jars for related BWD binding,
  - window opacity in AWT/Swing is only available from Java 7,
  - LWJGL3 BWD binding requires Java 8+ to run due to a lambda.

- Update xxx_config properties files in:
  - src/build/resources
  - src/test/resources
  which involves gathering the required dependencies
  (cf. comments in these config files about where they can be found).

# Build

- Launch JlkBuildMain: it will build the jars it can
  (it's fine to fail on bindings requiring unavailable libraries
  if you don't care about them).

# Unit tests

- Run individual XxxTest files with JUnit,
  or all tests at once if your IDE allows.

# BWD GUI tests

- Launch BwdTestLauncherMain, choose binding(s) on the left panel
  and test(s) case(s) on other panels.
  Tests will launch as soon as at least one binding and one test case
  are chosen.

- To kill the tests, left-click on the "Kill Gui" that pops up
  in top left corner, or for most tests middle-click on test GUI.
  On Mac, sometimes also need to launch KillBwdTestJvmsOnMacMain
  for complete cleanup.

# BWD samples

- Launch SampleBwdWithAwtMain or SampleBwdWithJavaFxMain.

# PAQ (Potentially Asked Questions)

- "Why abstracting things away, why simplicity and decoupling?"
  Not easy for me to explain something I feel is so obvious.
  A few idea that point more or less into the same direction:
  - Occam's razor.
  - Interface Segregation Principle and
    Dependency Inversion Principle (Robert C. Martin)
    (at least when applied at a large enough scale:
    I don't mean to push for overengineering)
  - "Decouple yourself from others." (Robert C. Martin,
    Craftsmanship and Ethics, 9m19s)
  - "Decentralization [or decoupling] provides strong peace of mind."
    (David Vorick, https://www.infoq.com/presentations/decentralized-storage)
  - "Find the dependencies -- and eliminate them."
    (http://www.joelonsoftware.com/articles/fog0000000007.html)
  - "When in doubt, leave it out." (Joshua Bloch)
  - "It seems that perfection is achieved, not when there is nothing left
    to add, but when there is nothing left to take away." (A. de Saint-Exupéry)
  - "It is difficult to free fools from the chains they revere." (Voltaire)
  - "But nevertheless I praise detachment above all love." (Meister Eckhart)
  - "I'm here to motivate you to be a better engineer (...) The super-power
    that allows me to do this is (...) a deep and strong hatred for technology,
    all kinds, without limitation." (Theo Schlossnagel,
    https://www.infoq.com/presentations/Scalable-Internet-Architectures, 5m47s)

- "Why Java?"
  For its portability, its backward compatibility (i.e. portability in time),
  its simplicity, its basic multi-threading features, and now its openness.

- "Why stick to Java 6?"
  I don't see any new feature from Java 7+ which value for this library would
  compensate the fact of not be usable by all the people still using Java 6,
  by far.
  Main reasons why I didn't stick to Java 5 instead: AWT/Swing API a bit behind,
  no AtomicXxx.lazySet(...), no JavaCompiler class, and easy to make code Java 5
  compliant if needed by removing the few Java 6 specific code.

- "Why not use a build tool?"
  Jolikit build is trivial, so it doesn't require a powerful build tool: we can
  just use JavaCompiler and JarOutputStream more or less directly for it.

- "In scheduler API, why not use java.util.concurrent.TimeUnit?"
  I consider the use of TimeUnit as a code smell, for I've seen too many
  precision loss bugs related to it, such as when people convert a duration
  from nanoseconds to seconds, and then back to nanoseconds.
  Instead, I prefer to use:
  - a long in nanoseconds for low level technical code, since systems timers
    often provide time as a mathematical integer in milliseconds, microseconds
    or nanoseconds, and
  - a double in seconds (SI unit) for physical computations in domain code,
    as done for other quantities (lengths, velocities, etc.) (I've seen people
    define unit-aware APIs for quantities, in the hope of fixing units issues,
    but it actually made the problem worse, as developpers started to use fancy
    units in setters and then assumed that getters returned values in SI units.
    Plus it caused the code to become cluttered with unit-related bloat,
    added memory overhead, and added CPU overhead due to possibly systematic
    conversions on read).

# Feedback

This first version is 0.1, to allow for taking eventual feedback into account
before moving to 1.0.
You can send your remarks to "jolikit" google group, after joining it by sending
an email to jolikit+subscribe@googlegroups.com and following the little process.

# Donation

[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=P7EYEFUCXBS9J)
