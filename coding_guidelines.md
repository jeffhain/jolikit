
# Coding guidelines

- Interfaces designed to be easily implementable, and not necessarily
  exhaustively implemented.

- Spaces only, no tab. It lowers the conceptual weight of the spacing process,
  allows for consistent display across editors, and makes it possible to
  deterministically predict required keystrokes to move the cursor around,
  without randomly stumbling on tabs.

- Always using curly-brackets for if statements and loops: it's more homogeneous
  than sometimes not using them, and makes modifications easier and safer.

- HTML tags make javadocs both harder to write, and harder to read in raw text,
  which is how code is most often interacted with, so no HTML bloat/clutter.
  Also, it would be a pity to introduce a dependency to HTML only for comments,
  when the code itself doesn't depend on it.

- Interfaces start with "Interface", and abstract classes with "Abstract"
  (except for protected or private inner classes that start with My,
  to avoid interferences with general classes):
  - It's handy,
  - It helps the developer not try to instantiate what could look like
    concrete classes while learning the API, which make the learning process
    both faster and less upsetting.
  - The common "Foo/FooImpl" convention leads to heterogeneous naming, in that
    the "normal name" of something could sometimes be used as the name of its
    interface, and sometimes as the name of its concrete class (if there is no
    Foo interface, one uses Foo for concrete class, not FooImpl).
  Concrete test classes for abstract classes start with "Abztract",
  and abstract classes for tests end with "Tezt".

- Default order for classes content:
  - configuration (static fields) (first for easy access)
  - inner classes (from public to private) (put before class fields and methods
    that might make use of them)
  - fields (put early (if no inner class) to quickly see class data)
  - methods (from public to private)

- A preference for static inner classes and methods:
  - for some performance-critical instance methods, having the actual
    implementation code in a static method, with the instance as argument,
    has been observed to help immensely in some cases (JVM optimization trick?).
  - it helps to figure out and minimize treatments dependencies.
  - it helps to reduce megamorphic calls.

- English is in little endian, but we often prefer big endian, and name things
  as Yoda would have named them, for it helps figuring out available methods
  using auto-completion (such as getIntSigned and getIntUnsigned appearing
  when the user looks for a getInt method).

- Methods computing stuffs are called computeXXX rather than getXXX, to make
  sure the developer is aware that they have a possibly non-neglectable
  computing cost.

- Objects are initialized by their constructor (which can therefore have a lot
  of parameters) rather than by setters, to make sure that they are in a well
  defined state upon creation, and that no developer might forget to call such
  or such setter. This also helps to prevent accidental cyclic dependencies.
  If this can't be done, breaking this rule for the class of higher level.

- Often avoiding anonymous inner classes, for easier debugging.

- The English language has a single word, "number", where the French language
  has two: "nombre" for amounts, and "numéro" for identifiers.
  Using "number" in the code often makes it difficult to figure out whether the
  developer refers to an amount or to an identifier.
  To discriminate between these two semantics, we use respectively "nbr"
  and "num" abbreviations instead of "number", both being compatible with
  "number" while also referring to the proper French word.

- For some quantities, like dates or angles, values can typically use various
  units, while other quantities are usually represented by values in SI (unless
  if you live in the UK, in the US or else).
  For those quantities, always suffixing with the unit (dateS, dateNs, angRad,
  angDeg, etc.), for ambiguous quantities all over the code just make it hard
  to understand and check, and call for comments that might become obsolete
  and misleading.

- Using the single "TODO" tag for any kind of unfinished, bugged, unsafe or just
  weird code. The thing "to do" is to at least check that code from time to
  time, or when passing nearby, in case it could or must be made better at that
  time.
  Not using multiple tags (FIXME, XXX, etc.), for some changes elsewhere could
  make a more specific tag become obsolete (like a new usage that would turn
  dangerous code into actually bugged code).
  Using a single tag also makes it easier to check for potentially problematic
  code, and there is no risk of forgetting to search for some nth tag.
  Possibly using some sub-tags for priorities, like "TODO" followed by "argh"
  for code that must not be committed (such as temporary debug code or
  unfinished code).
