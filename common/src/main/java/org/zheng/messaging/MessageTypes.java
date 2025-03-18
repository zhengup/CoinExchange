package org.zheng.messaging;


import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.zheng.message.AbstractMessage;
import org.zheng.util.JsonUtil;

import java.io.IOException;
import java.util.*;

//消息类型
@Component
public class MessageTypes {

    final Logger logger = LoggerFactory.getLogger(getClass());
    final String messagePackage = AbstractMessage.class.getPackage().getName();
    final Map<String, Class<? extends AbstractMessage>> messageTypes = new HashMap<>();

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() {
        logger.info("find message classes...");
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        //动态扫描并过滤继承于AbstractMessage的类
        provider.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadateReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                // 获取当前扫描到的类的全限定名
                String className = metadateReader.getClassMetadata().getClassName();
                // 通过类名加载类对象
                Class<?> clazz = null;
                try{
                    clazz = Class.forName(className);
                }catch(ClassNotFoundException e){
                    throw new RuntimeException(e);
                }
                // 判断当前类是否是 AbstractMessage 的子类或实现类
                return AbstractMessage.class.isAssignableFrom(clazz);
            }
        });
        //扫描AbstractMessage下的类并加载
        Set<BeanDefinition> beans = provider.findCandidateComponents(messagePackage);
        for (BeanDefinition beanDefinition : beans) {
            try{
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
            }catch(ClassNotFoundException e){
                throw new RuntimeException(e);
            }
        }
    }

    public String serialize(AbstractMessage message) {
        String type = message.getClass().getName();
        String json = JsonUtil.writeJsonAsString(message);
        return type + SEP +json;
    }
    public List<AbstractMessage> deserialize(List<String> dataList) {
        List<AbstractMessage> list = new ArrayList<>(dataList.size());
        for (String data : dataList) {
            list.add(deserialize(data));
        }
        return list;
    }
    public AbstractMessage deserialize(String data) {
        int pos = data.indexOf(SEP);
        if (pos == -1) {
            throw new RuntimeException("Unable to handle message with data: " + data);
        }
        String type = data.substring(0, pos);
        Class<? extends AbstractMessage> clazz = messageTypes.get(type);
        if (clazz == null) {
            throw new RuntimeException("Unable to handle message with type: " + type);
        }
        String json = data.substring(pos + 1);
        return JsonUtil.readJson(json, clazz);
    }

    private static final char SEP = '#';
}
