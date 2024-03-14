package com.alibaba.dashscope.utils;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;

public class GsonNumberAdapter extends TypeAdapter<Number> {

  @Override
  public void write(JsonWriter out, Number value) throws IOException {
    out.value(value);
  }

  @Override
  public Number read(JsonReader in) throws IOException {
    String value = in.nextString();
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException longE) {
      try {
        Double d = Double.valueOf(value);
        if ((d.isInfinite() || d.isNaN()) && !in.isLenient()) {
          throw new MalformedJsonException("JSON forbids NaN and infinities: " + d + in);
        }
        return d;
      } catch (NumberFormatException doubleE) {
        throw new JsonParseException("Cannot parse " + value, doubleE);
      }
    }
  }
}
