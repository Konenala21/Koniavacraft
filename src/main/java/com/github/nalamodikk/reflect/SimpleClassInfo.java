package com.github.nalamodikk.reflect;

import java.util.Set;

/**
 * 簡易類別資訊封裝
 * 用於存儲掃描到的類名與註解
 */
public record SimpleClassInfo(String className, Set<String> annotations) {
    public boolean isAnnotationPresent(Class<?> annotationClass) {
        return annotations.contains(annotationClass.getName());
    }

    public Class<?> toClass() {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("無法加載類別: " + className, e);
        }
    }
}
