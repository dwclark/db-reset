package com.github.dwclark.dbreset;

public abstract class MapStringExtension {
    
    public static String convert(Object val) {
        if(val instanceof String) {
            return "'" + val.replace("'", "\\'") + "'";
        }
        else if(val instanceof Map) {
            return toMapString(val);
        }
        else {
            return val.toString();
        }
    }

    public static String toMapString(Map map) {
        return '[' + map.collect { key, val -> convert(key) + ":" + convert(val) }.join(',') + ']'
    }

    public void fromMapString(String str) {
        Map args = Eval.me(str);
        args.each { key, val -> this[key] = val; };
    }
    
    public abstract String toMapString();
}