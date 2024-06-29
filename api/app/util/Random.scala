package util

/**
  * Wrapper on the scala random libraries providing higher level
  * common functions.
  */
case class Random() {

  private val random = new java.security.SecureRandom

  private val Ambiguous = "B8G6I1l0OoQDS5Z2".split("")
  private val Numbers = "0123456789"
  private val Lower = "abcdefghijklmnopqrstuvwxyz"
  private val LowerAndUpper = Lower + Lower.toUpperCase
  private val LowerAndUpperAndNumbers = LowerAndUpper + Numbers
  private val NonAmbiguousLowerAndUpper = LowerAndUpper.split("").filter(!Ambiguous.contains(_)).mkString("")
  private val NonAmbiguousLowerAndUpperAndNumbers = NonAmbiguousLowerAndUpper + "3479"

  /**
    * Generate a random string of length n from the given alphabet
    * 
    * @param alphabet The complete set of 
    * @param n Length of random string to generate
    */
  def string(alphabet: String)(n: Int): String = {
    assert(n > 0, "n must be > 0")
    LazyList.continually(random.nextInt(alphabet.length)).map(alphabet).take(n).mkString
  }

  /**
    * Generate a random string of length n using only a-z (lower case
    * alphabet letters)
    * 
    * @param n Length of random string to generate
    */
  def lowercaseAlpha(n: Int): String = {
    string(Lower)(n)
  }

  /**
    * Generate a random string of length n using only a-z and A-Z
    * (alphabet letters only)
    * 
    * @param n Length of random string to generate
    */
  def alpha(n: Int): String = {
    string(LowerAndUpper)(n)
  }

  /**
    * Generate a random string of length n using only letters (a-z,
    * A-Z) and numbers (0-9). Also guarantees that the random string
    * will start with a letter, not a number - this is mostly to
    * prevent problems with any applications that infer numeric based
    * on first digit (or strip zeroes).
    * 
    * The random string is guaranteed to start with a letter (we do
    * this to avoid confusion in some programs like excel which can
    * infer a numeric type based on the first character)
    * 
    * @param n Length of random string to generate
    */
  def alphaNumeric(n: Int): String = {
    if (n == 1) {
      alpha(1)
    } else {
      alpha(1) + string(LowerAndUpperAndNumbers)(n - 1)
    }
  }

    /**
    * Generate a random string of length n using only letters and
    * numbers that are non ambiguous (e.g. B can look like an 8 so
    * neither B nor 8 is used in the random string). This is a good
    * option for random strings that will be read by humans.
    * 
    * The random string is guaranteed to start with a letter (we do
    * this to avoid confusion in some programs like excel which can
    * infer a numeric type based on the first character)
    * 
    * @param n Length of random string to generate
    */
  def alphaNumericNonAmbiguous(n: Int): String = {
    if (n == 1) {
      string(NonAmbiguousLowerAndUpper)(1)
    } else {
      string(NonAmbiguousLowerAndUpper)(1) + string(NonAmbiguousLowerAndUpperAndNumbers)(n - 1)
    }
  }

  /**
    * Generate a random positive int
    */
  @scala.annotation.tailrec
  final def positiveInt(): Int = {
    val value = random.nextInt
    if (value > 0) {
      value
    } else {
      positiveInt()
    }
  }

  /**
    * Generate a random positive long
    */
  @scala.annotation.tailrec
  final def positiveLong(): Long = {
    val value = random.nextLong
    if (value > 0) {
      value
    } else {
      positiveLong()
    }
  }

}
