package org.zheng.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.util.Map;

public final class JsonUtil {
    //静态日志工具，记录警告和错误
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    //静态共享实例，确保线程安全
    private static  final ObjectMapper OBJECT_MAPPER = createObjectMapperForInternal();
    private static  ObjectMapper createObjectMapperForInternal() {
        final ObjectMapper mapper = new ObjectMapper();
        //序列化包含所有属性
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //反序列化忽略未知字段
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //允许序列化无属性的空bean
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //日期不写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    //序列化为字节数组
    public static byte[] writeJsonAsBytes(Object object) {
        try{
            return OBJECT_MAPPER.writeValueAsBytes(object);
        }catch(JsonProcessingException e){
            throw new UncheckedIOException(e);
        }
    }
    //序列化为字符串
    public static String writeJsonAsString(Object object) {
        try{
            return OBJECT_MAPPER.writeValueAsString(object);
        }catch(JsonProcessingException e){
            throw new UncheckedIOException(e);
        }
    }
    //序列化到输出流
    public static void writeJson(OutputStream output,Object object) {
        try{
            OBJECT_MAPPER.writeValue(output, object);
        } catch(JsonProcessingException e){
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //格式化输出
    public static String writeJsonWithPrettyPrinter(Object object) {
        try{
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        }catch(JsonProcessingException e){
            throw new UncheckedIOException(e);
        }
    }

    //从字符串反序列化
    public static <T> T readJson(String json, Class<T> clazz) {
        try{
            return OBJECT_MAPPER.readValue(json, clazz);
        }catch(JsonProcessingException e){
            logger.warn("readJson error:{}", json, e);
            throw new UncheckedIOException(e);
        }
    }
    //从reader中反序列化
    public static <T> T readJson(Reader reader, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(reader, clazz);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    //使用TypeReference保留泛型的具体类型，因为java泛型在编译后会擦除类型信息
    public static <T> T readJson(Reader reader, TypeReference<T> typeReference) {
        try{
            return OBJECT_MAPPER.readValue(reader, typeReference);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    //从输入流格式化
    public static <T> T readJson(InputStream inputStream, Class<T> clazz) {
        try{
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(InputStream inputStream, TypeReference<T> typeReference) {
        try{
            return OBJECT_MAPPER.readValue(inputStream, typeReference);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(String json, TypeReference<T> typeReference) {
        try{
            return OBJECT_MAPPER.readValue(json, typeReference);
        }catch(JsonProcessingException e){
            logger.warn("readJson error:{}", json, e);
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(byte[] json, TypeReference<T> typeReference) {
        try{
            return OBJECT_MAPPER.readValue(json, typeReference);
        }catch(JsonProcessingException e){
            logger.warn("readJson error:{}", json, e);
            throw new UncheckedIOException(e);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static <T> T readJson(byte[] json, Class<T> clazz) {
        try{
            return OBJECT_MAPPER.readValue(json, clazz);
        }catch(JsonProcessingException e){
            logger.warn("cannot read json from bytes: " + ByteUtil.toHexString(json), e);
            throw new UncheckedIOException(e);
        }catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static Map<String, Object> readJsonToMap(String json) {
        try{
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        }catch(JsonProcessingException e){
            throw new UncheckedIOException(e);
        }
    }
    //使用sb，从map到JSON
    public static String buildJsonFromJsonMap(Map<String, Object> jsonMap) {
        if(jsonMap.isEmpty()){
            return "{}";
        }
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{");
        for(String key : jsonMap.keySet()){
            String str = jsonMap.get(key).toString();
            if(str != null){
                sb.append('\"').append(key).append('\"').append(':').append(str).append(',');
            }
        }
        sb.setCharAt(sb.length()-1, '}');
        return sb.toString();
    }
    public static final TypeReference<Map<String,Object>> TYPE_MAP_STRING_OBJECT = new TypeReference<>() {};
    public static final TypeReference<Map<String,String>> TYPE_MAP_STRING_STRING = new TypeReference<>() {};
    public static final TypeReference<Map<String,Integer>> TYPE_MAP_STRING_INTEGER = new TypeReference<>() {};
    public static final TypeReference<Map<String,Boolean>> TYPE_MAP_STRING_BOOLEAN = new TypeReference<>() {};

    public static class BigDecimalStringSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("\"" + value.toPlainString() + "\"");
        }
    }
}
