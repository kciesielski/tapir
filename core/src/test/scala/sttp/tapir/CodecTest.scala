package sttp.tapir

import java.math.{BigDecimal => JBigDecimal}
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.fortysevendeg.scalacheck.datetime.jdk8.ArbitraryJdk8.{arbLocalDateJdk8, arbLocalDateTimeJdk8, genZonedDateTimeWithZone}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{Assertion, FlatSpec, Matchers}
import org.scalatestplus.scalacheck.Checkers
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.DecodeResult.Value

import scala.reflect.ClassTag

class CodecTest extends FlatSpec with Matchers with Checkers {

  implicit val arbitraryJBigDecimal: Arbitrary[JBigDecimal] = Arbitrary(
    (implicitly[Arbitrary[BigDecimal]]).arbitrary.map(bd => new JBigDecimal(bd.toString))
  )

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(
    genZonedDateTimeWithZone(Some(ZoneOffset.ofHoursMinutes(3, 30)))
  )

  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary(Gen.chooseNum(0, 86399999999999L).map(LocalTime.ofNanoOfDay))
  val localDateTimeCodec: Codec[LocalDateTime, TextPlain, String] = implicitly[Codec[LocalDateTime, TextPlain, String]]
  val zonedDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  it should "encode simple types using .toString" in {
    checkEncodeDecodeToString[String]
    checkEncodeDecodeToString[Byte]
    checkEncodeDecodeToString[Int]
    checkEncodeDecodeToString[Long]
    checkEncodeDecodeToString[Float]
    checkEncodeDecodeToString[Double]
    checkEncodeDecodeToString[Boolean]
    checkEncodeDecodeToString[UUID]
    checkEncodeDecodeToString[BigDecimal]
    checkEncodeDecodeToString[JBigDecimal]
    checkEncodeDecodeToString[LocalTime]
    checkEncodeDecodeToString[LocalDate]
  }

  // Because there is no separate standard for local date time, we encode it WITH timezone set to "Z"
  // https://swagger.io/docs/specification/data-models/data-types/#string
  it should "encode LocalDateTime with timezone" in {
    check((ldt: LocalDateTime) => localDateTimeCodec.encode(ldt) == ldt.atZone(ZoneOffset.UTC).format(zonedDateTimeFormatter))
  }

  it should "decode LocalDateTime from string with timezone" in {
    check((zdt: ZonedDateTime) => localDateTimeCodec.decode(zdt.format(zonedDateTimeFormatter)) == Value(zdt.toLocalDateTime))
  }

  it should "correctly encode and decode zoned date time" in {
    val codec = implicitly[Codec[ZonedDateTime, TextPlain, String]]
    codec.encode(ZonedDateTime.of(LocalDateTime.of(2010, 9, 22, 14, 32, 1), ZoneOffset.ofHours(5))) shouldBe "2010-09-22T14:32:01+05:00"
    codec.encode(ZonedDateTime.of(LocalDateTime.of(2010, 9, 22, 14, 32, 1), ZoneOffset.UTC)) shouldBe "2010-09-22T14:32:01Z"
    check((zdt: ZonedDateTime) => codec.decode(codec.encode(zdt)) == Value(zdt))
  }

  it should "decode LocalDateTime from string without timezone" in {
    check((ldt: LocalDateTime) => localDateTimeCodec.decode(ldt.toString) == Value(ldt))
  }

  def checkEncodeDecodeToString[T: Arbitrary](implicit c: Codec[T, TextPlain, String], ct: ClassTag[T]): Assertion =
    withClue(s"Test for ${ct.runtimeClass.getName}") {
      check((a: T) => {
        val decodedAndReEncoded = c.decode(c.encode(a)) match {
          case Value(v)   => c.encode(v)
          case unexpected => fail(s"Value $a got decoded to unexpected $unexpected")
        }
        decodedAndReEncoded == a.toString
      })
    }
}