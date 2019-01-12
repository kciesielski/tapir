package tapir.json.circe

import java.nio.charset.StandardCharsets

import tapir.DecodeResult.{Error, Value}
import tapir.{CodecMeta, DecodeResult, MediaType, RawValueType, StringValueType}
import tapir.GeneralCodec.JsonCodec
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import tapir.generic.SchemaFor

trait JsonCirce {
  implicit def encoderDecoderCodec[T: Encoder: Decoder: SchemaFor]: JsonCodec[T] = new JsonCodec[T] {
    override val rawValueType: RawValueType[String] = StringValueType(StandardCharsets.UTF_8)

    override def encode(t: T): String = t.asJson.noSpaces
    override def decode(s: String): DecodeResult[T] = io.circe.parser.decode[T](s) match {
      case Left(error) => Error(s, error, error.getMessage)
      case Right(v)    => Value(v)
    }
    override def meta: CodecMeta[MediaType.Json] = CodecMeta(implicitly[SchemaFor[T]].schema, MediaType.Json())
  }
}
