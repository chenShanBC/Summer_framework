package com.wubai.summer.core;

/**
 * @Author：fs
 * @Date:2026/3/517:52
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 基于XML配置的ApplicationContext
 * 核心功能：
 * 1. 解析XML配置文件，加载BeanDefinition
 * 2. 实例化所有Bean（构造器注入 + 属性注入）
 * 3. 管理Bean的生命周期（单例池）
 */
public class ClassPathXmlApplicationContext {
    // 复用相同的三个核心缓存
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private final Map<String, Object> singletonObjects = new HashMap<>();
    private final Set<String> creatingBeanNames = new HashSet<>();

    /**
     * 构造器：传入XML路径，启动容器
     * @param xmlPath XML文件路径（相对于ClassPath，如 "beans.xml"）
     */
    public ClassPathXmlApplicationContext(String xmlPath) {
        System.out.println("🚀 启动XML容器，配置文件：" + xmlPath);

        // 步骤1：解析XML，加载BeanDefinition
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader();
        this.beanDefinitionMap.putAll(reader.loadBeanDefinitions(xmlPath));

        // 步骤2：实例化所有Bean
        refresh();

        System.out.println("✅ 容器启动完成，共加载 " + singletonObjects.size() + " 个Bean");
    }

    /**
     * 刷新容器：实例化所有单例Bean
     */
    private void refresh() {
        for (String beanName : beanDefinitionMap.keySet()) {
            getBean(beanName);
        }
    }

    /**
     * 获取Bean实例（核心方法）
     * 与注解方式的区别：依赖按名称获取（getBean），而非按类型（getBeanByType）
     */
    public Object getBean(String beanName) {
        // 1. 单例池中有则直接返回
        if (singletonObjects.containsKey(beanName)) {
            return singletonObjects.get(beanName);
        }

        // 2. 获取BeanDefinition
        BeanDefinition beanDef = beanDefinitionMap.get(beanName);
        if (beanDef == null) {
            throw new RuntimeException("Bean不存在：" + beanName);
        }

        // 3. 循环依赖检测
        if (!creatingBeanNames.add(beanName)) {
            throw new RuntimeException("检测到循环依赖：" + beanName);
        }

        System.out.println("🔨 实例化Bean：" + beanName);

        // 4. 实例化Bean
        Object instance;
        if (beanDef.getFactoryMethod() == null) {
            // 普通Bean：构造器实例化（XML方式）
            instance = instantiateByConstructorXml(beanDef);
        } else {
            // 工厂Bean：工厂方法实例化
            instance = instantiateByFactoryMethod(beanDef);
        }

        // 5. 属性注入（XML的<property>标签）
        injectPropertiesXml(instance, beanDef);

        // 6. 放入单例池
        singletonObjects.put(beanName, instance);
        creatingBeanNames.remove(beanName);

        return instance;
    }

    /**
     * 构造器实例化（XML方式：根据constructorArgRefs按名称获取依赖）
     */
    private Object instantiateByConstructorXml(BeanDefinition beanDef) {
        try {
            Constructor<?> constructor = beanDef.getConstructor();
            List<String> argRefs = beanDef.getConstructorArgRefs();

            // 根据ref名称获取依赖Bean（递归调用getBean）
            Object[] paramValues = new Object[argRefs.size()];
            for (int i = 0; i < argRefs.size(); i++) {
                paramValues[i] = getBean(argRefs.get(i)); // 按名称获取
            }

            return constructor.newInstance(paramValues);
        } catch (Exception e) {
            throw new RuntimeException("构造器实例化失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 工厂方法实例化（与注解方式相同）
     */
    private Object instantiateByFactoryMethod(BeanDefinition beanDef) {
        try {
            Method factoryMethod = beanDef.getFactoryMethod();
            factoryMethod.setAccessible(true);
            Object factoryBean = getBean(beanDef.getFactoryBeanName());

            // 工厂方法无参数（简化版，可扩展支持参数）
            return factoryMethod.invoke(factoryBean);
        } catch (Exception e) {
            throw new RuntimeException("工厂方法实例化失败：" + beanDef.getBeanName(), e);
        }
    }

    /**
     * 属性注入（XML的<property>标签）
     * 根据属性名找到Setter方法，注入依赖Bean
     */
    private void injectPropertiesXml(Object instance, BeanDefinition beanDef) {
        Map<String, String> propertyRefs = beanDef.getPropertyRefs();
        if (propertyRefs.isEmpty()) return;

        Class<?> clazz = instance.getClass();
        for (Map.Entry<String, String> entry : propertyRefs.entrySet()) {
            String propertyName = entry.getKey();
            String refBeanName = entry.getValue();

            try {
                // 根据属性名生成Setter方法名（如 dataSource → setDataSource）
                String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

                // 获取依赖Bean
                Object refBean = getBean(refBeanName);

                // 查找匹配的Setter方法（参数类型与refBean兼容）
                Method setter = findSetter(clazz, setterName, refBean.getClass());
                setter.setAccessible(true);
                setter.invoke(instance, refBean);

                System.out.println("  ↳ 注入属性：" + propertyName + " = " + refBeanName);

            } catch (Exception e) {
                throw new RuntimeException("属性注入失败：" + propertyName, e);
            }
        }
    }

    /**
     * 查找Setter方法（参数类型兼容即可）
     * 支持接口/父类注入（如参数是接口，实际注入实现类）
     */
    private Method findSetter(Class<?> clazz, String setterName, Class<?> paramType) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> methodParamType = method.getParameterTypes()[0];
                // 检查类型兼容（支持接口/父类注入）
                if (methodParamType.isAssignableFrom(paramType)) {
                    return method;
                }
            }
        }
        throw new RuntimeException("找不到Setter方法：" + setterName);
    }

    /**
     * 按类型获取Bean（可选功能，如果需要支持按类型注入）
     */
    public <T> T getBeanByType(Class<T> type) {
        List<String> matchNames = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                matchNames.add(entry.getKey());
            }
        }
        if (matchNames.isEmpty()) {
            throw new RuntimeException("无匹配类型的Bean：" + type.getName());
        }
        if (matchNames.size() > 1) {
            throw new RuntimeException("多个Bean匹配类型：" + type.getName());
        }
        return (T) getBean(matchNames.get(0));
    }
}
/**
 * 1. **依赖解析方式不同**：XML按名称（getBean），注解按类型（getBeanByType）
 * 2. **构造器注入**：从constructorArgRefs读取Bean名称，递归调用getBean
 * 3. **属性注入**：从propertyRefs读取映射，找到Setter方法并反射调用
 * 4. **类型兼容检查**：支持接口/父类注入（isAssignableFrom）
 */
