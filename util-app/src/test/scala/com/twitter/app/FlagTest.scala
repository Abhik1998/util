package com.twitter.app

import org.scalatest.FunSuite
import scala.collection.mutable.Buffer

class FlagTest extends FunSuite {

  class Ctx(failFastUntilParsed: Boolean = false) {
    val flag = new Flags("test", includeGlobal = false, failFastUntilParsed)
    val fooFlag = flag("foo", 123, "The foo value")
    val barFlag = flag("bar", "okay", "The bar value")
  }

  test("Flag: defaults") {
    val ctx = new Ctx
    import ctx._

    flag.finishParsing()
    assert(fooFlag() == 123)
    assert(barFlag() == "okay")
  }

  test("Flag: override a flag") {
    val flag = new Flags("test")
    flag("foo", 1, "")
    flag("foo", 2, "")
    val allFlags = flag.getAll().toSet

    flag.finishParsing()
    assert(!allFlags.exists(_() == 1), "original flag was not overridden")
    assert(allFlags.exists(_() == 2), "overriding flag was not present in flags set")
  }

  test("Flag: withFailFast") {
    val flag = new Flags("test") // failFastUntilParsed = false
    var f1 = flag("foo", 1, "")
    var f2 = flag("foo", 2, "")

    assert(f1() == 1)
    assert(f2() == 2)

    f1 = f1.withFailFast(true)
    f2 = f2.withFailFast(true)

    // should now fail, as we are reading before parsed and have set the fail fast to true
    intercept[IllegalStateException] {
      f1()
    }
    intercept[IllegalStateException] {
      f2()
    }
  }

  test("Flag: let") {
    def current: Boolean = MyGlobalBooleanFlag()

    // track the order the blocks execute and that they only execute once
    var buf = Buffer[Int]()

    // make sure they stack properly
    assert(!current)
    MyGlobalBooleanFlag.let(true) {
      buf += 1
      assert(current)
      MyGlobalBooleanFlag.let(false) {
        buf += 2
        assert(!current)
      }
      buf += 3
      assert(current)
    }
    assert(!current)

    assert(buf == Seq(1, 2, 3))
  }

  test("Flag: let return values reflect bindings") {
    def current: Boolean = MyGlobalBooleanFlag()

    val res1 = MyGlobalBooleanFlag.let(true) { current.toString }
    assert(res1 == "true")
    val res2 = MyGlobalBooleanFlag.let(false) { current.toString }
    assert(res2 == "false")
  }

  test("Flag: letClear") {
    // track the order the blocks execute and that they only execute once
    var buf = Buffer[Int]()

    MyGlobalBooleanFlag.let(true) {
      assert(MyGlobalBooleanFlag.isDefined)
      buf += 1
      MyGlobalBooleanFlag.letClear {
        assert(!MyGlobalBooleanFlag.isDefined)
        buf += 2
      }
      assert(MyGlobalBooleanFlag.isDefined)
      buf += 3
    }

    assert(buf == Seq(1, 2, 3))
  }

  test("Flag: letClear return values reflect bindings") {
    def current: Boolean = MyGlobalBooleanFlag()

    MyGlobalBooleanFlag.let(true) {
      val res1 = current
      val res2 = MyGlobalBooleanFlag.letClear { current }
      assert(res1)
      assert(!res2)
    }
  }

  class Dctx extends Ctx {
    val quuxFlag = flag[Int]("quux", "an int")
  }

  test("Flag: no default usage") {
    val ctx = new Dctx
    import ctx._
    assert(quuxFlag.usageString == "  -quux='Int': an int")
  }

  private class GetCtx {
    private val flags = new Flags("test")
    val withDefault = flags[Int]("f1", 1, "an f1")
    val noDefault = flags[Int]("f2", "an f2")
    val noDefaultAndSupplied = flags[Int]("f3", "an f3")

    assert(flags.parseArgs(Array("-f3=3")) == Flags.Ok(Nil))
  }

  test("Flag.get") {
    val ctx = new GetCtx()
    import ctx._

    assert(withDefault.get == None)
    assert(noDefault.get == None)
    assert(noDefaultAndSupplied.get == Some(3))
  }

  test("Flag.getWithDefault") {
    val ctx = new GetCtx()
    import ctx._

    assert(withDefault.getWithDefault == Some(1))
    assert(noDefault.getWithDefault == None)
    assert(noDefaultAndSupplied.getWithDefault == Some(3))
  }

}
