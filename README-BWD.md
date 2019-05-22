BWD is a simple 2D UI API to abstract away UI libraries, by defining types and
interfaces corresponding to related basic concepts and use cases
(UI thread scheduling, windowing and related events, graphics (drawing
primitives, text, images, rectangular clipping, translations and 90 degrees
rotations), fonts, cursors, and devices (mouse, wheel and keyboard) events).

BWD is meant to serve as a basis for creating either simple 2D UIs directly,
or toolkits on top of which UIs can be built.

# Principal design constraints

- Must be relatively easy to implement on top of actual UI libraries,
  while still providing most basic features.

- Must be implementable on top of UI libraries in non-garbage-collected
  languages, hence have methods to dispose no longer used resources.

- Must not be too high level or subtle, else that could make binding code
  too complex, too large, and possibly too slow (e.g. if having affine
  transforms and/or paths for clipping).

- Must not be too low level, both to be actually helpful, and to allow for
  use of eventual optimizations available from the backing library
  (e.g. hardware acceleration, etc.).

# PAQ (Potentially Asked Questions)

- "Why abstract away UI libraries behind a common API?"
  A common API is desirable for obvious reasons, such as being able to apply the
  Dependency Inversion Principle to UI code, and all the usual benefits that
  come with it, such as not having to rewrite all your UI code when the library
  you use is abandoned or no longer running on the systems of the day, etc.,
  but also because UI libraries often have specific glitches and bugs (cf.
  related TODOs and workarounds in bindings implementations), and possibly very
  heterogeneous qualities (stability, performance, fonts rendering, supported
  fonts or images formats, etc.), which are many potential reasons for wanting
  to switch from a library to another one, or to use a mix of them. You might
  also want to switch to a library using mostly the CPU, if you want to keep GPU
  free for other applications, etc.
  It also seemed like an obvious thing do to because UI libraries, in spite of
  their large number and various peculiarities, mostly deal with always the same
  kinds of concepts and features (rectangular windowing and canvas, focus,
  iconification, maximization, events from same kinds of devices, common fonts
  and images formats, etc.). This can remind floating-point numbers before
  IEEE 754: many software systems were supporting them, but in all sorts of
  heterogeneous ways (and the large numbers of floating-points types some
  systems allowed the user to define, can be compared to the large number
  of color formats some UI libraries can support).
  There is a great quote from Joe Armstrong, about object-oriented languages
  (whatever that means): "You wanted a banana but what you got was a gorilla
  holding the banana and the entire jungle." When I pull an API to draw a few
  lines and some text on a screen, I don't want it to pull and impose on me
  the UI library that implements it, nor all the graphic stack jungle on top
  of which the library is built.

- "Why such a simple API?"
  The more advanced the API, the more complex the implementation, and too heavy
  implementations cause more lock-in, because it's harder to create alternative
  ones.
  On the contrary, I want this API to be disruptively boring, so that the user
  can feel like he could do an implementation of it on its own as a relatively
  small side project, even if that's on top of a low level library, and doesn't
  feel stuck with what he's currently using, having to depend on the good will
  of a corporation or an open source community to help him out.
  Also: "Personal Mastery: If a system is to serve the creative spirit,
  it must be entirely comprehensible to a single individual."
  (http://www.cs.virginia.edu/~evans/cs655/readings/smalltalk.html)
  That said, I would not have felt the need to do it, if a maybe a little bit
  more advanced, but yet simple and stable, UI API standard had emerged, but,
  alas, it doesn't look like it has been on the agenda of ISO/IEEE yet. The
  only approaching thing I'm aware of is a proposal to "add 2D graphics
  rendering and display to C++" (P0267R0), but unfortunately it is limited
  to the C++ world, and as OpenGL doesn't cover things like device events etc.
  (I also doubt it would be easy to implement on top of other libraries).

- "Why not just use the API of one of the many well-known existing
  UI libraries?"
  Because they are not designed to be easily implementable on top of other
  UI libraries, often having very idiosyncrasic ways of providing basic
  features, causing lock-in into their implementations, their glitches,
  their bugs, the languages they are coded in and the platforms they can
  run on.
  Also, they might not try to abstract away OS specificities, and try
  to conform to native behaviors, while I want to be able to define the UI
  behavior in my code as much as possible, with the OS on which my software
  happens to be running interfering the least.
  Also, they often evolve or get deprecated in the name of new advanced
  features or hardware, and thus are not a reliable basis for simple 2D UI
  code that doesn't make use of those. On the contrary, the Interface
  Segregation Principle tells us not to depend on advanced APIs when we
  only need simple features.

- "Why not just use OpenGL or Vulkan, which are UI APIs?"
  For the same reasons why I didn't use existing UI libraries APIs,
  plus the fact that they are too low level, and too complex since designed
  for advanced 3D, not simple 2D.
  It would be like wanting a bike and buying an airplane.
  Also, they don't cover windowing, primitives, text, images, cursors
  and device events.

- "Why not use HTML5?"
   - I would rather ask: why use HTML5, given:
     - the static idiosyncrasies (HTML, CSS), the dependencies and the
       complexity (JavaScript, WebGL, and a need for a browser) that it adds
       on top of the simple concepts BWD API is based on (integer, floating
       point, struct/class, methods and arguments, enums, and a String type
       used in two cases),
     - its limitations (being stuck in the browser, which is another layer
       of uncontrollable bloat on top of the OS, no access to the file system,
       etc.).
   - I did some Ajax when it was new, with Sarissa and validated XHTML.
     After a few months of browser updates nothing was working anymore,
     silently. I didn't plan to touch web technologies again since then.
   That said, it should be possible to implement at least parts of BWD API
   on top of HTML5.

- "Why make nine bindings?"
  The implementations/bindings available along with it in JOLIKIT
  are just a side effect of the design process.
  In an interview, Joshua Bloch tells about the "rule of 3": when designing
  an API, make three implementations of it on different grounds to help
  rule out specificities.
  I applied the "rule of 9", because this API is large enough that using only
  three libraries would not be enough to avoid accidental similarities among
  specificities.
  It was also the least I could do for an API that pretends to be easy
  to implement.
  Also, nine UIs fit nicely on the screen in a 3*3 grid, when launching
  a test case with all bindings at once.

- "Why not just make one binding per platform, without the intermediary bloat
  of portable UI libraries?"
  Building anything platform-specific is like building inside of a prison,
  or inside of a sinking ship, and I'm allergic to it, even if it's building
  a ladder to get out. Platform-specific code is my kryptonite.
  That said, I could happily use such bindings if anyone could make them.

- "How did you choose the backing libraries?"
  I chose libraries either in Java, or with Java bindings, or in C,
  which can easily be used from Java with JNA.
  I also discarded libraries that were in C but without binaries for download,
  because the beginner-friendliness of compiling C code on various platforms
  is close to absolute zero, and I didn't want to impose that on users.

- "Why only test the bindings on Windows and Mac, not Linux?"
  Because I'm too lazy, and count on Linux fans to tell me where to put
  "if (OsUtils.isLinux())" hacks for bindings to work properly on Linux
  (cf. the "few" OsUtils.isWindows() and OsUtils.isMac() here and there).
  All the libraries used are supposed to also work on Linux, so that should not
  be too hard.
  I initially did these bindings just on Windows, then ensured they could also
  work on a Retina Mac to make sure they handled HiDPI displays properly.

- "Why not 3D?"
  It's one or two orders of magnitude of complexity out of the scope of BWD.
  If you want to do nice 3D graphics, just pick the latest technology
  from the entertainment industry.
  That said, doing pseudo-3D (i.e. 2.5D) is possible and efficient with
  drawing primitives, doing a projection before calling them.

- "When does painting take place?"
  Soon after a call to host.ensurePendingClientPainting(), typically less than
  1/60th of a second after, or much less, depending on the binding and its
  configuration. It can also be triggered by the backing library or the binding.

- "Where does painting take place?"
  Painting on BWD client area must be done in client.paintClient(...) method,
  forwarding to the backing library and further down the graphics stack being
  done either synchronously or later at some flush time depending on the
  binding and its configuration.

- "Why only rectangular clipping?"
  Clips with arbitrary shapes is somewhat out of the scope of BWD.
  Also, they are much slower to apply, and can easily kill the frame rate.
  Rectangular clips have the nice property that the intersection
  of two of them is another rectangular clip (or emptiness).

- "Why only 90 degrees rotations and no scaling? Why not affine transforms
  with floating-point values?"
  It's out of the scope of BWD, because it brings complexities that would
  have a noticeable impact on ease of implementation and on performances,
  and corresponds to a kind of features best served going full 3D.
  It would no longer be possible to simply use a rectangle for clips,
  as the intersections between clips (base clip, and user clips defined
  after various transforms) might no longer be rectangular.
  Also, it's not possible to implement in the binding if the backing library
  doesn't support them and also doesn't provide a way to read the pixels
  it renders, which would be required to transform them within the binding
  (that said, we could maybe assume that libraries advanced enough to have
  an asynchronous drawing pipeline that make it hard to read pixels,
  are also advanced enough to support affine transforms).

- "Why no support for gestures, multi-touch, and other mobile devices
  friendly features?"
  These features are often not or inconsistently supported across
  desktop UI libraries.
  That said, W3C's Pointer Events recommendation is designed to be compatible
  with usual mouse events, so you should be able to somehow generate mouse and
  wheel events from gesture events.

- "Why BWD client's painting method, which is unambiguously called on a
  client object, is called 'paintClient', and not just 'paint'?"
  For the same reasons why BWD classes are typically prefixed with 'Bwd':
  to make it easier to search for it, and to not confuse it with
  something else.

- "What if I can't implement some methods of the API?"
  It's perfectly fine for some methods of your binding to throw or
  return default values, if you don't rely on them.

- "Why is newImage(...) method only supposed to work with a file path,
  not an URL?"
  A UI API is not a networking or downloading API, but you are free
  to implement a binding that supports it if you like.

- "What about threading?"
  Unless specified otherwise, every method of BWD API should be called
  in the UI thread, but if parallel painting is supported, multiple
  graphics can be used concurrently.
  To execute a treatment in UI thread, use the UI thread scheduler available
  from binding, which also provides a parallelizer for parallelizing work
  otherwise done in UI thread (such as painting).
  For lower latency, bindings might not add asynchronism between backing events
  and BWD events, such as on event it's safer not to try to modify host state
  synchronously, in case it would cause infinite recursion, and to use
  uiThreadScheduler.execute(...) instead for that.

- "For a simple API, why bother with parallel painting?"
  Painting a grid of pixels is an embarrassingly parallel problem, and it
  doesn't cost much to make the API allow for it, so it would have been
  a pity not to make it possible.
  That said, a lot UI libraries, especially "advanced" ones, have a UI thread
  in which some or all painting methods are supposed be called
  (e.g. Canvas.snapshot(...) in JavaFX), making it impractical for them.

- "For a simple API, why bothering with transparency?"
  All UI libraries I know more or less support it, and it would be a pity
  to waste a byte of ints by sticking to Rgb24.
  It also doesn't complicate the API much, and (almost) only slows things
  down when actually using non-opaque colors.

- "Why complicate the API with methods for Argb64 colors?"
  It doesn't cost much to add such methods, and allows for possibly much more
  accurate colors, in particular for grayscales: 65526 shades of gray is much
  better than 256.
  Also, having color stored as Argb64 (in graphics, in BwdColor, or in a long
  managed by the user) allows for less accuracy loss across modifications.

- "Why only 1-pixel thick primitives drawings?"
  To make implementation much easier (our primitives are really meant to be
  primitive).
  Also, thickness can always be added later (or aside) to the API without
  backward compatibility issue, with a graphics.setThickness(double) method.

- "Why no draw/fillRoundRect(...) methods?"
  These are easy to implement on top of ovals and lines/rectangles methods, but
  they correspond to a too specific choice or drawing corners. For example, one
  might want only some corners to be rounded, or rounded using "inverse" ovals
  centered on rectangle corners, or corners with straight oblique lines instead.
  If new primitive were added, that would rather be to draw/fill polygons/paths.

- "Why is there no way to draw on an offscreen image?"
  Images creation in memory (not from a file), and drawing on images using
  a graphics, could be added, but some libraries don't allowing for painting
  out of their UI thread, in which case preparing an image by drawing it
  offscreen would block events and regular painting.
  Also, some libraries don't provide graphics for their offscreen images,
  which would make it hard to implement, or slow to execute.
  That said, it could be used without issue in some initialization phase,
  or in progressive offscreen painting with short duration steps.

- "Why use xSpan and ySpan instead of width and height?"
  To be more explicit and not having to remember or remind each time that
  width corresponds to x and height to y, and not to lie after a 90 degrees
  rotation, in which case width is actually vertical and height horizontal.
  For images we use width and height because they are expressed in image
  frame of reference.

# Pain points

"The only way to build highly reliable systems containing millions of lines
of software is to assemble them from highly reliable and well defined smaller
components. Unfortunately, these components don't exist." (Martyn Thomas)

The more I was advancing bindings implementations, the more issues and
inconsistencies across libraries I encountered, and the more I saw BWD as a
mandatory anti-corruption layer behind which to hide and factor out related
workarounds.

- The hardest thing to abstract away was Window state and events.
  Not all libraries support basic features such as iconification,
  maximization, or even hiding, and it cannot always be emulated.
  Window events, when they exist, often occur differently depending on both
  the library and the OS, with various kinds of preconditions and orderings,
  and possibly in inconsistent ways (such as a window gaining focus before
  the previously focused one loses it).
  Cf. AbstractBwdHost and extending classes for the complicated logics
  put together to bring invariants, simplicity and consistency among window
  events fired to the client, so that they can be used as corner stones for
  life cycle management, and HostUnitTestBwdTestCase for related unit tests.

- The second hardest thing to abstract away was fonts.
  Trying to deal with fonts portably is a mess, for multiple reasons:
  - A mess because Unicode goes out of its way to try to support all the
    complexities of natural languages orthographies, with modifier characters,
    ligatures, kerning, hinting, alternate glyphs, etc. (ex. of related turmoil:
    http://tinyletter.com/mbutterick/letters/q-ligatures-in-programming-fonts-a-hell-no)
  - A mess because a same font will easily be rendered quite differently
    depending on the library and/or the platform.
  - A mess because font metrics are a mess, and might only make sense for some
    orthographies (baseline). Libraries don't always provide font metrics, and
    when they do, these metrics are not reliable, as some glyphs can leak
    outside of their theoretical box, possibly depending on the used font size,
    or usage of diacritical marks.
  - A mess because libraries handle control or special characters differently,
    or just ignore them.
  - A mess because it's common for fonts APIs to use the absolute unit of
    a "point" for the size, which is theoretically 1/72th of an inch, but most
    often actually corresponds to 1.3333 (OS-resolution) pixel, whatever the
    actual physical size of these pixels.
  - A mess because font formats are a mess.
  - A mess because each library has its own idea of what loading and using
    a font means, and their APIs is often quite obscure, such as they don't
    tell which fonts were loaded or whether glyphs are available for a given
    code point, or silently draw unavailable glyphs with unknown fallback
    fonts, etc.

- Among the few libraries I used that have APIs for graphics primitives,
  only JavaFX got them right.
  Ovals most often look like potatoes, possibly due to the use of sin/cos with
  steps (such as in FLTK), which can't be pixel-perfect and must be very slow.
  Drawn/filled ovals can also be inconsistent, or there can be bad corner cases
  when one or more span is 1, etc.
  Plain lines on the other hand seem to be always fine.
  Some libraries not having primitives, I had to reimplement them anyway,
  so not much additional work was needed to make up for that.

- Often had to add or remove one or half a pixel here and there to have things
  properly located depending on the rotation.
  Sometimes it is normal, such as for AWT/Swing, for which drawing and filling
  a same rectangle requires you to use possibly different bounds depending on
  the rotation, due to AWT/Swing coordinates being centered on pixels edges
  (not centers) and drawing hitting bottom-right pixels (whatever the rotation)
  and filling only inner pixels; but other times it must be due to bugs in the
  backing library, because the adjustments can be different depending on the
  drawing method.
