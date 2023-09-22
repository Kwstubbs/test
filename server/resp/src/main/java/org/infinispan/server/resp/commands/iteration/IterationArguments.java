package org.infinispan.server.resp.commands.iteration;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.server.resp.Resp3Handler;
import org.infinispan.server.resp.RespErrorUtil;
import org.infinispan.server.resp.RespTypes;
import org.infinispan.server.resp.Util;
import org.infinispan.server.resp.commands.ArgumentUtils;
import org.infinispan.server.resp.filter.ComposedFilterConverterFactory;
import org.infinispan.server.resp.filter.GlobMatchFilterConverterFactory;
import org.infinispan.server.resp.filter.RespTypeFilterConverterFactory;

public class IterationArguments {
   private static final int DEFAULT_COUNT = 10;
   private static final byte[] COUNT = "COUNT".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] MATCH = "MATCH".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] TYPE = "TYPE".getBytes(StandardCharsets.US_ASCII);

   private final int count;
   private final String filterConverterFactory;
   private final List<byte[]> filterConverterParams;
   private final RespTypes type;

   private IterationArguments(int count, String filterConverterFactory, List<byte[]> filterConverterParams, RespTypes type) {
      this.count = count;
      this.filterConverterFactory = filterConverterFactory;
      this.filterConverterParams = filterConverterParams;
      this.type = type;
   }

   public int getCount() {
      return count;
   }

   public String getFilterConverterFactory() {
      return filterConverterFactory;
   }

   public List<byte[]> getFilterConverterParams() {
      return filterConverterParams;
   }

   public RespTypes getType() {
      return type;
   }

   public static IterationArguments parse(Resp3Handler handler, List<byte[]> arguments) {
      int argc = arguments.size();
      Map<Class<?>, List<byte[]>> filters = null;
      int count = DEFAULT_COUNT;
      RespTypes type = null;
      if (argc > 1) {
         for (int i = 1; i < argc; i++) {
            byte[] arg = arguments.get(i);
            if (Util.isAsciiBytesEquals(MATCH, arg)) {
               if (++i >= argc) {
                  RespErrorUtil.syntaxError(handler.allocator());
                  return null;
               } else {
                  if (filters == null) filters = new HashMap<>(2);
                  filters.put(GlobMatchFilterConverterFactory.class, Collections.singletonList(arguments.get(i)));
               }
            } else if (Util.isAsciiBytesEquals(COUNT, arg)) {
               if (++i >= argc) {
                  RespErrorUtil.syntaxError(handler.allocator());
                  return null;
               } else {
                  try {
                     count = ArgumentUtils.toInt(arguments.get(i));
                  } catch (NumberFormatException e) {
                     RespErrorUtil.valueNotInteger(handler.allocator());
                     return null;
                  }
               }
            } else if (Util.isAsciiBytesEquals(TYPE, arg)) {
               if (++i >= argc) {
                  RespErrorUtil.syntaxError(handler.allocator());
                  return null;
               } else {
                  try {
                     type = RespTypes.valueOf(new String(arguments.get(i), StandardCharsets.US_ASCII));
                     if (filters == null) filters = new HashMap<>(2);
                     filters.put(RespTypeFilterConverterFactory.class, Collections.singletonList(new byte[] { (byte) type.ordinal() }));
                  } catch (IllegalArgumentException e) {
                     type = RespTypes.unknown;
                  }
               }
            }
         }
      }

      if (filters != null) {
         Map.Entry<Class<?>, List<byte[]>> e = ComposedFilterConverterFactory.convertFiltersFormat(filters);
         return new IterationArguments(count, e.getKey().getName(), e.getValue(), type);
      }

      return new IterationArguments(count, null, null, type);
   }
}
