
# To do or not to do, that's the question.

- Build Java 9+ modules in addition to jars, and see how small a UI executable
  using BWD can be depending on the backing library and OS.

- BWD API:
  - Other blending modes than SRC_OVER.
  - Bulk pixels drawing methods (but they could be seen as an excuse to make
    drawPoint(int,int) slow, which would be bad), and same for reading.
  - Offscreen image painting (but in UI thread, due to limitations of some
    (too) advanced libraries).

- BWD bindings:
  - Bindings wrapper that implements windowing on top of a single backing
    window. Without animations by default (they can be annoying for state
    based treatments and consistency, since during the action the state is
    unclear). Would be much faster and much more reliable than windowing
    of actual UI libraries, but would only work with itself.
  - In-memory binding implementation, for unit tests of UI code, possibly with
    soft scheduling (fast and deterministic).
  - Bindings to native APIs.
  - Binding to HTML5 (best effort).
  - LWJGL3 and JOGL bindings: use FreeType instead of AWT for fonts
    (couldn't manage to make it work through JNA (???)).
  - JOGL binding: use FreeImage instead of AWT for images.
  - In our bindings, make pixel size configurable, not to burn CPU on HiDPIs,
    or to easily scale UIs depending on screen DPI without having to change
    a line of code, or for user preferring to see pixels, for example to know
    if a scrollbar actually did hit its limit.
    For that we could add pixelRatioUserOverOsX/Y (int >= 1) in base binding
    config (as we have pixelRatioOsOverDeviceX/Y (double > 0) in configs of
    some bindings), i.e. size of a user pixel in OS pixels.
  - In our bindings to 3D libraries, make it possible to use backing 3D
    capabilities after casting the graphics into its concrete class,
    so that 3D drawing can take place than then 2D drawing with BWD
    on top of the 3D.

- A BWD-like API but for simple 3D.

- Terminate and add BWT (toolkit based on BWD).

- Add DataBuffer, ByteCopyUtils, etc.
